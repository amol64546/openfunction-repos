package utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import enums.InstanceKind;
import models.Arg;
import models.Instance;
import models.Param;
import okio.Buffer;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.IntStream;

public class InstanceUtils {

    private static final Moshi MOSHI = new Moshi.Builder().build();
    private static final ObjectMapper JACKSON = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Map<String, Object> map, String paramName) {
        try {
            Map<String, Object> paramMap = JACKSON.convertValue(map.get("params"), new TypeReference<>() {
            });
            Param param = JACKSON.convertValue(paramMap.get(paramName), Param.class);
            if (param.getKind() == null) {
                throw new RuntimeException("Param kind is required");
            }
            Class<?> aClass = Class.forName(param.getClassName());

            // Build param args (reuse your existing arg materialization)
            if (param.getKind().getValue().equalsIgnoreCase(InstanceKind.FACTORY.getValue())) {
                List<Object> fValues = new ArrayList<>();
                List<Class<?>> fTypes = new ArrayList<>();
                if (param.getArgs() != null) {
                    for (Arg a : param.getArgs()) {
                        Object val = materializeArg(a);
                        fValues.add(val);
                        fTypes.add(inferTypeForParam(a, val));
                    }
                }

                // Find & invoke static param
                Method m = resolveStaticFactory(aClass, param.getName(), fTypes);
                Object instance = m.invoke(null, fValues.toArray());
                return (T) instance;
            } else {
                // Build constructor args
                List<Object> values = new ArrayList<>();
                List<Class<?>> types = new ArrayList<>();

                if (param.getArgs() != null) {
                    for (Arg a : param.getArgs()) {
                        switch (a.getKind().getValue()) {
                            case "string" -> {
                                values.add(a.getValue());
                                types.add(String.class);
                            }
                            case "primitive" -> {
                                Object parsed = parsePrimitive(a.getKind().getValue(), a.getValue());
                                values.add(parsed);
                                types.add(toPrimitiveOrWrapper(a.getKind().getValue()));
                            }
                            case "object" -> {
                                Object nested = decodeObjectFromBase64(a.getBase64(), a.getClassName());
                                values.add(nested);
                                types.add(Class.forName(a.getClassName()));
                            }
                            default -> throw new IllegalArgumentException("Unsupported arg kind: " + a.getKind());
                        }
                    }
                }

                // Pick constructor and instantiate
                Constructor<?> ctor = resolveBestConstructor(aClass, types);
                return (T) ctor.newInstance(values.toArray());

            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing param request: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Map<String, Object> req) {
        try {
            Map<String, Object> map = JACKSON.convertValue(req.get("instance"), new TypeReference<>() {
            });
            Instance instance = JACKSON.convertValue(map, Instance.class);

            if (instance.getKind() == null) {
                throw new RuntimeException("Request kind is required");
            }

            Class<?> aClass = Class.forName(instance.getClassName());

            if (instance.getKind().getValue().equalsIgnoreCase(InstanceKind.FACTORY.getValue())) {
                List<Object> fValues = new ArrayList<>();
                List<Class<?>> fTypes = new ArrayList<>();
                if (instance.getArgs() != null) {
                    for (Arg a : instance.getArgs()) {
                        Object val = materializeArg(a);
                        fValues.add(val);
                        fTypes.add(inferTypeForParam(a, val));
                    }
                }

                // Find & invoke static instance
                Method m = resolveStaticFactory(aClass, instance.getName(), fTypes);
                return (T) m.invoke(null, fValues.toArray());
            } else {
                // Build constructor args
                List<Object> values = new ArrayList<>();
                List<Class<?>> types = new ArrayList<>();

                if (instance.getArgs() != null) {
                    for (Arg a : instance.getArgs()) {
                        switch (a.getKind().getValue()) {
                            case "string" -> {
                                values.add(a.getValue());
                                types.add(String.class);
                            }
                            case "primitive" -> {
                                Object parsed = parsePrimitive(a.getKind().getValue(), a.getValue());
                                values.add(parsed);
                                types.add(toPrimitiveOrWrapper(a.getKind().getValue()));
                            }
                            case "object" -> {
                                Object nested = decodeObjectFromBase64(a.getBase64(), a.getClassName());
                                values.add(nested);
                                types.add(Class.forName(a.getClassName()));
                            }
                            default -> throw new IllegalArgumentException("Unsupported arg kind: " + a.getKind());
                        }
                    }
                }

                // Pick constructor and instantiate
                Constructor<?> ctor = resolveBestConstructor(aClass, types);
                return (T) ctor.newInstance(values.toArray());

            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing instance request: " + e.getMessage(), e);
        }
    }


    public static Method resolveStaticFactory(Class<?> owner, String name, List<Class<?>> argTypes) {
        // If caller didn’t specify a name, try common factory names in order
        String[] candidates = (name == null || name.isBlank())
                ? new String[]{"of", "from", "valueOf", "getInstance", "newInstance", "parse", "fromString"}
                : new String[]{name};

        for (String nm : candidates) {
            for (Method m : owner.getMethods()) {
                if (!m.getName().equals(nm) || (m.getModifiers() & Modifier.STATIC) == 0) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != argTypes.size()) continue;
                boolean ok = true;
                for (int i = 0; i < params.length; i++) {
                    if (!isCompatible(params[i], argTypes.get(i))) {
                        ok = false;
                        break;
                    }
                }
                if (ok) return m;
            }
        }
        throw new IllegalArgumentException("No static factory found on " + owner.getName()
                + " named " + (name == null ? Arrays.toString(candidates) : name)
                + " with arg types " + argTypes);
    }

    public static Object decodeObjectFromBase64(String base64, String className) throws Exception {
        if (base64 == null || className == null) {
            throw new IllegalArgumentException("object arg requires className and base64");
        }
        Class<?> clazz = Class.forName(className);
        byte[] bytes = Base64.getDecoder().decode(base64);
        JsonAdapter<?> adapter = MOSHI.adapter(clazz);
        return adapter.fromJson(new Buffer().write(bytes));
    }

    public static Constructor<?> resolveBestConstructor(Class<?> target, List<Class<?>> argTypes)
            throws NoSuchMethodException {
        Constructor<?>[] ctors = target.getConstructors();
        for (Constructor<?> c : ctors) {
            Class<?>[] params = c.getParameterTypes();
            if (params.length == argTypes.size()
                    && IntStream.range(0, params.length).allMatch(i -> isCompatible(params[i], argTypes.get(i)))) {
                return c;
            }
        }
        try {
            return ConstructorUtils.getMatchingAccessibleConstructor(
                    target, argTypes.toArray(new Class<?>[0]));
        } catch (Exception ignore) {
        }
        throw new NoSuchMethodException("No suitable constructor found for " + target.getName() +
                " with arg types " + argTypes);
    }

    public static boolean isCompatible(Class<?> param, Class<?> arg) {
        if (param.isAssignableFrom(arg)) return true;
        if (param.isPrimitive()) {
            return (param == int.class && arg == Integer.class)
                    || (param == long.class && arg == Long.class)
                    || (param == double.class && arg == Double.class)
                    || (param == float.class && arg == Float.class)
                    || (param == boolean.class && arg == Boolean.class)
                    || (param == byte.class && arg == Byte.class)
                    || (param == short.class && arg == Short.class)
                    || (param == char.class && arg == Character.class);
        }
        return false;
    }

    public static Class<?> toPrimitiveOrWrapper(String type) {
        if (type == null) return Object.class;
        switch (type.toLowerCase(Locale.ROOT)) {
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "double":
                return double.class;
            case "float":
                return float.class;
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "char":
                return char.class;
//            case "string":
//                return String.class;
            default:
                try {
                    return Class.forName(type);
                } catch (ClassNotFoundException e) {
                    return Object.class;
                }
        }
    }

    public static Object parsePrimitive(String type, String value) {
        if (type == null) return value;
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "int" -> Integer.parseInt(value);
            case "long" -> Long.parseLong(value);
            case "double" -> Double.parseDouble(value);
            case "float" -> Float.parseFloat(value);
            case "boolean" -> Boolean.parseBoolean(value);
            case "byte" -> Byte.parseByte(value);
            case "short" -> Short.parseShort(value);
            case "char" -> (value != null && !value.isEmpty()) ? value.charAt(0) : '\0';
//            case "string" -> value;
            default -> value;
        };
    }


    public static Object buildArrayArg(String componentTypeName, List<Arg> elems) throws Exception {
        if (componentTypeName == null) throw new IllegalArgumentException("array requires 'type' as component type");
        Class<?> componentType = toPrimitiveOrWrapper(componentTypeName);
        Object array = Array.newInstance(componentType, elems == null ? 0 : elems.size());
        for (int i = 0; i < (elems == null ? 0 : elems.size()); i++) {
            Object v = materializeArg(elems.get(i)); // reuse same logic as your switch
            // attempt primitive boxing/unboxing is fine; Array.set will handle boxing
            Array.set(array, i, v);
        }
        return array;
    }

    @SuppressWarnings("unchecked")
    public static Object buildCollectionArg(String implClassName, List<Arg> elems) throws Exception {
        // pick impl: either the provided className or a sensible default
        Class<?> impl = (implClassName != null && !implClassName.isBlank())
                ? Class.forName(implClassName)
                : ArrayList.class;

        Object coll = impl.getDeclaredConstructor().newInstance();
        if (!(coll instanceof Collection)) {
            throw new IllegalArgumentException("className for collection must be a Collection implementation");
        }

        Collection<Object> c = (Collection<Object>) coll;
        if (elems != null) {
            for (Arg e : elems) {
                c.add(materializeArg(e));
            }
        }
        return coll;
    }

    public static Object buildStringBuilder(String value, boolean buffer) {
        return buffer ? new StringBuffer(value == null ? "" : value)
                : new StringBuilder(value == null ? "" : value);
    }

    public static Object materializeArg(Arg a) throws Exception {
        return switch (a.getKind().getValue()) {
            case "string" -> a.getValue();
            case "primitive" -> parsePrimitive(a.getType().getValue(), a.getValue());
            case "object" -> decodeObjectFromBase64(a.getBase64(), a.getClassName());
            case "array" -> buildArrayArg(a.getKind().getValue(), a.getElements());
            case "collection" -> buildCollectionArg(a.getClassName(), a.getElements());
            case "stringbuilder" -> buildStringBuilder(a.getValue(), false);
            case "stringbuffer" -> buildStringBuilder(a.getValue(), true);
            default -> throw new IllegalArgumentException("Unsupported arg kind: " + a.getKind());
        };
    }

    public static Class<?> inferTypeForParam(Arg a, Object value) throws ClassNotFoundException {
        return switch (a.getKind().getValue()) {
            case "string" -> String.class;
            case "primitive" -> toPrimitiveOrWrapper(a.getType().getValue());
            case "object" -> Class.forName(a.getClassName());
            case "array" -> {
                Class<?> comp = toPrimitiveOrWrapper(a.getType().getValue());
                yield Array.newInstance(comp, 0).getClass();
            }
            case "collection" ->
                // Use the runtime class of the created impl (e.g., ArrayList) – constructor param can be Collection/List/Set,
                // and isAssignableFrom will pass because ArrayList implements those.
                    value.getClass();
            case "stringbuilder" -> StringBuilder.class;
            case "stringbuffer" -> StringBuffer.class;
            default -> value.getClass();
        };
    }

}
