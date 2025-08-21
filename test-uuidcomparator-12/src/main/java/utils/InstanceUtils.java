package utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
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

    @SuppressWarnings("unchecked" )
    public static <T> T getInstance(Map<String, Object> map, String paramName) {
        try {
            Map<String, Object> paramMap = JACKSON.convertValue(map.get("params" ), new TypeReference<>() {
            });
            Param param = JACKSON.convertValue(paramMap.get(paramName), Param.class);
            if (param.getKind() == null) {
                throw new RuntimeException("Param kind is required" );
            }
            Class<?> aClass = Class.forName(param.getClassName());

            // Build param args (reuse your existing arg materialization)
            switch (param.getKind().getValue().toLowerCase()) {
                case "factory" -> {
                    List<Object> fValues = new ArrayList<>();
                    List<Class<?>> fTypes = new ArrayList<>();

                    if (param.getArgs() != null) {
                        for (Arg a : param.getArgs()) {
                            Object val = materializeArg(a);
                            fValues.add(val);
                            fTypes.add(inferTypeForParam(a, val));
                        }
                    }

                    // Find & invoke static factory method
                    Method m = resolveStaticFactory(aClass, param.getName(), fTypes);
                    Object instance = m.invoke(null, fValues.toArray());
                    return (T) instance;
                }

                case "object" -> {
                    return getInstance(param.getBase64(), param.getClassName());
                }

                case "constructor" -> {
                    List<Object> values = new ArrayList<>();
                    List<Class<?>> types = new ArrayList<>();

                    if (param.getArgs() != null) {
                        for (Arg a : param.getArgs()) {
                            switch (a.getKind().getValue().toLowerCase()) {
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

                    // Call constructor reflectively
                    Constructor<?> ctor = aClass.getConstructor(types.toArray(new Class[0]));
                    Object instance = ctor.newInstance(values.toArray());
                    return (T) instance;
                }

                default -> throw new IllegalArgumentException("Unsupported param kind: " + param.getKind());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing param request: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked" )
    public static <T> T getInstance(Map<String, Object> req) {
        try {
            Map<String, Object> map = JACKSON.convertValue(req.get("instance" ), new TypeReference<>() {
            });
            Instance instance = JACKSON.convertValue(map, Instance.class);

            if (instance.getKind() == null) {
                throw new RuntimeException("Request kind is required" );
            }

            Class<?> aClass = Class.forName(instance.getClassName());

            switch (instance.getKind().getValue().toLowerCase()) {
                case "factory" -> {
                    // Build factory args
                    List<Object> fValues = new ArrayList<>();
                    List<Class<?>> fTypes = new ArrayList<>();

                    if (instance.getArgs() != null) {
                        for (Arg a : instance.getArgs()) {
                            Object val = materializeArg(a);
                            fValues.add(val);
                            fTypes.add(inferTypeForParam(a, val));
                        }
                    }

                    // Find & invoke static factory method
                    Method m = resolveStaticFactory(aClass, instance.getName(), fTypes);
                    return (T) m.invoke(null, fValues.toArray());
                }

                case "object" -> {
                    return getInstance(instance.getBase64(), instance.getClassName());
                }

                case "constructor" -> {
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

                default -> throw new IllegalArgumentException("Unsupported kind: " + instance.getKind().getValue());
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
            throw new IllegalArgumentException("object arg requires className and base64" );
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
        if (componentTypeName == null) throw new IllegalArgumentException("array requires 'type' as component type" );
        Class<?> componentType = toPrimitiveOrWrapper(componentTypeName);
        Object array = Array.newInstance(componentType, elems == null ? 0 : elems.size());
        for (int i = 0; i < (elems == null ? 0 : elems.size()); i++) {
            Object v = materializeArg(elems.get(i)); // reuse same logic as your switch
            // attempt primitive boxing/unboxing is fine; Array.set will handle boxing
            Array.set(array, i, v);
        }
        return array;
    }

    @SuppressWarnings("unchecked" )
    public static Object buildCollectionArg(String implClassName, List<Arg> elems) throws Exception {
        // pick impl: either the provided className or a sensible default
        Class<?> impl = (implClassName != null && !implClassName.isBlank())
                ? Class.forName(implClassName)
                : ArrayList.class;

        Object coll = impl.getDeclaredConstructor().newInstance();
        if (!(coll instanceof Collection)) {
            throw new IllegalArgumentException("className for collection must be a Collection implementation" );
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


    @SuppressWarnings("unchecked" )
    public static <T> T getInstance(Object base64, String className) {
        try {
            String s = Objects.toString(base64, "" ).trim();
            byte[] bytes = tryDecodeB64(s);
            if (bytes == null) {
                // Not base64? treat as raw text/JSON bytes
                bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }

            Class<?> clazz = Class.forName(className);

            // 1) Try Moshi first
            try {
                JsonAdapter<?> adapter = MOSHI.adapter(clazz);
                return (T) adapter.fromJson(new okio.Buffer().write(bytes));
            } catch (Exception moshiErr) {
                // 2) Generic fallback: parse to generic JSON value, then reflectively coerce
                Object generic = MOSHI.adapter(Object.class).fromJson(new okio.Buffer().write(bytes));
                return (T) coerce(generic, clazz);
            }
        } catch (Exception e) {
            throw new RuntimeException("getInstance error for " + className + ": " + e.getMessage(), e);
        }
    }

    public static byte[] tryDecodeB64(String s) {
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException ignore) {
        }
        try {
            return Base64.getUrlDecoder().decode(s);
        } catch (IllegalArgumentException ignore) {
        }
        return null;
    }

    /**
     * Coerce a generic JSON value (String/Double/Boolean/Map/List/null) into target class without custom adapters.
     */
    public static Object coerce(Object value, Class<?> target) throws Exception {
        if (value == null) return null;

        // Identity / direct assign
        if (target.isInstance(value)) return value;

        // Strings and primitives/wrappers
        if (target == String.class) return String.valueOf(value);
        if (target == int.class || target == Integer.class) return toNumber(value).intValue();
        if (target == long.class || target == Long.class) return toNumber(value).longValue();
        if (target == double.class || target == Double.class) return toNumber(value).doubleValue();
        if (target == float.class || target == Float.class) return toNumber(value).floatValue();
        if (target == boolean.class || target == Boolean.class) return toBoolean(value);
        if (target == byte.class || target == Byte.class) return toNumber(value).byteValue();
        if (target == short.class || target == Short.class) return toNumber(value).shortValue();
        if (target == char.class || target == Character.class) {
            String sv = String.valueOf(value);
            return sv.isEmpty() ? '\0' : sv.charAt(0);
        }

        // Enums from string
        if (target.isEnum() && value instanceof String str) {
            @SuppressWarnings("rawtypes" )
            Class<? extends Enum> ec = (Class<? extends Enum>) target;
            return Enum.valueOf(ec, str);
        }

        // Try common static factories from String
        if (value instanceof String strVal) {
            Method m = findStaticFactory(target, "fromString", String.class);
            if (m == null) m = findStaticFactory(target, "valueOf", String.class);
            if (m == null) m = findStaticFactory(target, "parse", String.class);
            if (m != null) return m.invoke(null, strVal);

            // Try String constructor
            try {
                Constructor<?> c = target.getDeclaredConstructor(String.class);
                if (!c.canAccess(null)) c.setAccessible(true);
                return c.newInstance(strVal);
            } catch (NoSuchMethodException ignored) { /* fall through */ }
        }

        // Maps/lists → try default Moshi mapping into target if it's a POJO
        try {
            // Re-encode generic value as JSON, then decode into target
            String json = new com.google.gson.Gson().toJson(value);
            JsonAdapter<?> adapter = MOSHI.adapter(target);
            return adapter.fromJson(new okio.Buffer().write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
        }

        throw new IllegalArgumentException("Cannot coerce value '" + value + "' (" + value.getClass().getName() + ") to " + target.getName());
    }

    public static java.lang.reflect.Method findStaticFactory(Class<?> target, String name, Class<?>... paramTypes) {
        try {
            Method m = target.getMethod(name, paramTypes);
            if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) return m;
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    public static Number toNumber(Object v) {
        if (v instanceof Number n) return n;
        if (v instanceof String s) {
            if (s.matches("^-?\\d+$" )) return Long.parseLong(s);
            return Double.parseDouble(s);
        }
        throw new IllegalArgumentException("Not a numeric value: " + v);
    }

    public static Boolean toBoolean(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        throw new IllegalArgumentException("Not a boolean value: " + v);
    }


}
