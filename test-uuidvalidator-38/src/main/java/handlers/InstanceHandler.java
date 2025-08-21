package handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import enums.InstanceKind;
import models.Arg;
import models.CreateInstanceRequest;
import models.CreateInstanceResponse;
import utils.InstanceUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InstanceHandler {

    private static final Gson GSON = new Gson();
    private static final ObjectMapper JACKSON = new ObjectMapper();

    public static void handleInstanceCreation(HttpRequest request, HttpResponse response) throws IOException {
        try {
            String requestBody = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            List<CreateInstanceRequest> reqList = JACKSON.readValue(
                    requestBody, new TypeReference<>() {
                    }
            );

            List<CreateInstanceResponse> responses = new ArrayList<>();
            for (CreateInstanceRequest req : reqList) {
                CreateInstanceResponse createInstanceResponse = getCreateInstanceResponse(req);
                responses.add(createInstanceResponse);
            }
            sendSuccessResponse(response, responses);

        } catch (Exception e) {
            sendErrorResponse(response, Map.of("error", e.getMessage()));
        }
    }

    private static CreateInstanceResponse getCreateInstanceResponse(CreateInstanceRequest req) throws Exception {
        if (req.getKind() == null) {
            throw new RuntimeException("Request kind is required");
        }

        CreateInstanceResponse createInstanceResponse;

        Class<?> aClass = Class.forName(req.getClassName());

        if (req.getKind().getValue().equalsIgnoreCase(InstanceKind.FACTORY.getValue())) {

            // Build factory args (reuse your existing arg materialization)
            List<Object> fValues = new ArrayList<>();
            List<Class<?>> fTypes = new ArrayList<>();
            if (req.getArgs() != null) {
                for (Arg a : req.getArgs()) {
                    Object val = InstanceUtils.materializeArg(a);
                    fValues.add(val);
                    fTypes.add(InstanceUtils.inferTypeForParam(a, val));
                }
            }

            // Find & invoke static factory
            Method m = InstanceUtils.resolveStaticFactory(aClass, req.getName(), fTypes);
            Object instance = m.invoke(null, fValues.toArray());

            byte[] bytes = GSON.toJson(instance).getBytes(StandardCharsets.UTF_8);
            String b64 = Base64.getEncoder().encodeToString(bytes);
            createInstanceResponse = new CreateInstanceResponse(req.getClassName(), b64);
        } else {
            // Build constructor args
            List<Object> values = new ArrayList<>();
            List<Class<?>> types = new ArrayList<>();

            if (req.getArgs() != null) {
                for (Arg a : req.getArgs()) {
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

            createInstanceResponse = new CreateInstanceResponse(aClass.getName(), b64);
        }
        return createInstanceResponse;
    }

    private static void sendSuccessResponse(HttpResponse response, List<CreateInstanceResponse> createInstanceResponses) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(JACKSON.writeValueAsString(createInstanceResponses));
    }

    private static void sendErrorResponse(HttpResponse response, Map<String, String> error) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write(JACKSON.writeValueAsString(error));
    }

}
