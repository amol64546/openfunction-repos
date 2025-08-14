import Handlers.SwaggerHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.google.gson.Gson;
import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import dev.openfunction.functions.Routable;
import models.Arg;
import models.CreateInstanceRequest;
import models.CreateInstanceResponse;
import utils.InstanceUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main extends Routable implements HttpFunction {

    private static final Gson GSON = new Gson();
    private static final ObjectMapper JACKSON = new ObjectMapper();
    private static final Map<String, Function<Map<String, Object>, Object>> pathHandlers = new HashMap<>();

    static {
        pathHandlers.put("/swagger-ui", SwaggerHandler::swaggerUIHandler);
        pathHandlers.put("/swagger.json", SwaggerHandler::swaggerJsonHandler);
        pathHandlers.put("/instances", Main::instances);
        pathHandlers.put("/getTimeBased", Main::getTimeBased);
        pathHandlers.put("/getRandomBased", Main::getRandomBased);
    }

    private static Object getTimeBased(Map<String, Object> body) {
        return UuidCreator.getTimeBased();
    }

    private static Object getRandomBased(Map<String, Object> body) {
        return UuidCreator.getRandomBased();
    }


    private static Object instances(Object object) {
        try {
            CreateInstanceRequest req = JACKSON.convertValue(object, CreateInstanceRequest.class);
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
        Map<String, Object> body;
        if (requestBody.isEmpty()) {
            body = new HashMap<>();
        } else {
            body = JACKSON.readValue(requestBody, new TypeReference<>() {
            });
        }
        if (!pathHandlers.containsKey(request.getPath())) {
            response.setStatusCode(404);
            response.getWriter().write("Not Found");
            return;
        }
        body.put("path", request.getPath());
        Object result = pathHandlers.get(request.getPath())
                .apply(body);
        sendResponse(response, result);
    }

    @Override
    public String getPath() {
        return "/*";
    }

    private void sendResponse(HttpResponse response, Object result) throws IOException {
        if (result instanceof SwaggerHandler.StaticFileResponse) {
            SwaggerHandler.StaticFileResponse sfr = (SwaggerHandler.StaticFileResponse) result;
            response.setContentType(sfr.contentType);
            response.getOutputStream().write(sfr.content);
        } else {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("result", result);
            response.setContentType("application/json");
            response.getWriter().write(JACKSON.writeValueAsString(responseBody));
        }
    }


}
