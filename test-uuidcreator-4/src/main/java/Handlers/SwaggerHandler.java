package Handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SwaggerHandler {

    // For static files, wrap content and content type
    public static class StaticFileResponse {
        public final byte[] content;
        public final String contentType;

        public StaticFileResponse(byte[] content, String contentType) {
            this.content = content;
            this.contentType = contentType;
        }
    }

    // Handler for /swagger-ui
    public static Object swaggerUIHandler(Map<String, Object> body) {
        try {
            String requestPath = (String) body.get("path"); // Assuming body contains the request path
            String filePath = "src/main/resources/swagger-ui" + requestPath;
            // Determine the content type based on file extension
            String contentType = getContentType(filePath);

            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            return new StaticFileResponse(fileContent, contentType);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "File not found");
            return error;
        }
    }

    private static String getContentType(String filePath) {
        if (filePath.endsWith(".html")) {
            return "text/html";
        } else if (filePath.endsWith(".css")) {
            return "text/css";
        } else if (filePath.endsWith(".js")) {
            return "application/javascript";
        } else if (filePath.endsWith(".json")) {
            return "application/json";
        } else if (filePath.endsWith(".map")) {
            return "application/json"; // For source maps
        } else {
            return "application/octet-stream"; // Default binary content type
        }
    }

    // Handler for /swagger.json
    public static Object swaggerJsonHandler(Map<String, Object> body) {
        // Replace with your OpenAPI JSON
        String openApiSpec = "{\n" +
                "                  \"openapi\": \"3.0.1\",\n" +
                "                  \"info\": {\n" +
                "                    \"title\": \"My API\",\n" +
                "                    \"version\": \"1.0.0\"\n" +
                "                  },\n" +
                "                  \"paths\": {\n" +
                "                    \"/hello\": {\n" +
                "                      \"get\": {\n" +
                "                        \"summary\": \"Returns a greeting\",\n" +
                "                        \"responses\": {\n" +
                "                          \"200\": {\n" +
                "                            \"description\": \"A greeting message\"\n" +
                "                          }\n" +
                "                        }\n" +
                "                      }\n" +
                "                    }\n" +
                "                  }\n" +
                "                }";
        Map<String, Object> result = new HashMap<>();
        result.put("swagger", openApiSpec);
        return result;
    }
}