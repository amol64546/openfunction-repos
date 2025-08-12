import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import dev.openfunction.functions.Routable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import models.Arg;
import models.CreateInstanceRequest;
import models.CreateInstanceResponse;
import utils.InstanceUtils;

import java.util.UUID;
import com.github.f4b6a3.uuid.util.UuidComparator;

public class Main extends Routable implements HttpFunction {

    private static final Gson GSON = new Gson();
    private static final ObjectMapper JACKSON = new ObjectMapper();
    private static final Map<String, Function<Map<String, Object>, Object>> pathHandlers = new HashMap<>();

    static {
        pathHandlers.put("/instances", Main::instances);
        pathHandlers.put("/compare", Main::compare);
    }

    private static Object compare(Map<String, Object> body) {
        UuidComparator instance = InstanceUtils.getInstance(body);
        return instance.compare(uuid1, uuid2);
    }


    private static Object instances(Object object) {
        try {
            CreateInstanceRequest req = (CreateInstanceRequest) object;
            if (req.kind == null || req.kind.isBlank()) {
                throw new RuntimeException("Request kind is required");
            }

            Class<?> aClass = Class.forName(req.className);

            if (req.kind.equals("factory")) {

                // Build factory args (reuse your existing arg materialization)
                List<Object> fValues = new ArrayList<>();
                List<Class<?>> fTypes = new ArrayList<>();
                if (req.args != null) {
                    for (Arg a : req.args) {
                        Object val = InstanceUtils.materializeArg(a);
                        fValues.add(val);
                        fTypes.add(InstanceUtils.inferTypeForParam(a, val));
                    }
                }

                // Find & invoke static factory
                Method m = InstanceUtils.resolveStaticFactory(aClass, req.name, fTypes);
                Object instance = m.invoke(null, fValues.toArray());

                byte[] bytes = GSON.toJson(instance).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                String b64 = Base64.getEncoder().encodeToString(bytes);
                return new CreateInstanceResponse(req.className, b64);
            } else if (req.kind.equals("constructor")) {

                // Build constructor args
                List<Object> values = new ArrayList<>();
                List<Class<?>> types = new ArrayList<>();

                if (req.args != null) {
                    for (Arg a : req.args) {
                        Object val = InstanceUtils.materializeArg(a);
                        values.add(val);
                        types.add(InstanceUtils.inferTypeForParam(a, val));
                    }
                }

                // Pick constructor and instantiate
                Constructor<?> ctor = InstanceUtils.resolveBestConstructor(aClass, types);
                Object instance = ctor.newInstance(values.toArray());

                // Encode instance -> JSON bytes via Gson; then Base64
                byte[] bytes = GSON.toJson(instance).getBytes(StandardCharsets.UTF_8);
                String b64 = Base64.getEncoder().encodeToString(bytes);

                return new CreateInstanceResponse(aClass.getName(), b64);
            } else {
                throw new RuntimeException("Invalid request kind " + req.kind);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error creating instance: " + e.getMessage(), e);
        }
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String requestBody = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));
        Map<String, Object> body = JACKSON.readValue(requestBody, new TypeReference<>() {
        });
        Object result = pathHandlers.get(request.getPath())
                .apply(body);
        sendResponse(response, result);
    }

    @Override
    public String getPath() {
        return "/*";
    }

    private void sendResponse(HttpResponse response, Object result)
            throws IOException {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("result", result);
        response.setContentType("application/json");
        response.getWriter().write(JACKSON.writeValueAsString(responseBody));
    }
}
