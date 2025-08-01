
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.codec.UrnCodec;
import com.github.f4b6a3.uuid.util.UuidValidator;

import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import dev.openfunction.functions.Routable;

public class Main extends Routable implements HttpFunction {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, BiConsumer<Map<String, Object>, HttpResponse>> pathHandlers = new HashMap<>();

    static {
        pathHandlers.put("/isUuidUrn", Main::handleIsUuidUrn);
        pathHandlers.put("/isValid", Main::handleIsValid);
    }

    private static void handleIsUuidUrn(Map<String, Object> body, HttpResponse response) {
        if (body.size() != 1) {
            String errorMessage = "Number of parameters in request body does not match method signature.";
            sendJsonResponse(response, 404, errorMessage);
            return;
        }
        if (!(body.get("string") instanceof String)) {
            sendJsonResponse(response, 404, "Invalid parameters.");
            return;
        }
        Object result = UrnCodec.isUuidUrn((String) body.get("string"));
        sendJsonResponse(response, 200, result);

    }

    private static void handleIsValid(Map<String, Object> body, HttpResponse response) {
        if (body.size() != 2) {
            String errorMessage = "Number of parameters in request body does not match method signature.";
            sendJsonResponse(response, 404, errorMessage);
            return;
        }
        if (!(body.get("uuid") instanceof String)) {
            sendJsonResponse(response, 404, "Invalid parameters.");
            return;
        }
        if (!(body.get("version") instanceof Integer)) {
            sendJsonResponse(response, 404, "Invalid parameters.");
            return;
        }
        Object result = UuidValidator.isValid((String) body.get("uuid"), (int) body.get("version"));
        sendJsonResponse(response, 200, result);

    }

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {

        String requestBody = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));
        if (requestBody.isEmpty()) {
            sendJsonResponse(response, 400, "Request body is empty.");
            return;
        }
        Map<String, Object> body = mapper.readValue(requestBody, new TypeReference<>() {
        });

        pathHandlers.get(request.getPath())
                .accept(body, response);

        pathHandlers.getOrDefault(request.getPath(), (b, r) -> sendJsonResponse(r, 404, "Path not found."))
                .accept(body, response);
    }

    @Override
    public String getPath() {
        return "/*";
    }

    @Override
    public String[] getMethods() {
        return new String[]{"GET"};
    }

    private static void sendJsonResponse(HttpResponse response, int statusCode, Object result) {
        response.setStatusCode(statusCode);
        response.setContentType("application/json");
        try {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("statusCode", statusCode);
            responseBody.put("result", result);
            response.getWriter().write(mapper.writeValueAsString(responseBody));
        } catch (IOException e) {
            System.err.println("Error writing response: " + e.getMessage());
        }
    }
}
