package Handlers;

import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SwaggerHandler {

    public static void handleSwaggerUI(HttpRequest request, HttpResponse response) throws IOException {
        String filePath = "src/main/resources/swagger-ui" + request.getPath().substring("/swagger-ui".length());
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        response.getOutputStream().write(fileContent);
    }

    public static void handlerSwaggerJson(HttpRequest request, HttpResponse response) throws IOException {
        String filePath = "src/main/resources/swagger.json";
        String openApiSpec = Files.readString(Paths.get(filePath));
        response.setContentType("application/json");
        OutputStream os = response.getOutputStream();
        os.write(openApiSpec.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}