import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpUtils {

    private static final ObjectMapper JACKSON = new ObjectMapper();

    public static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        }
    }

    public static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] out = json.getBytes(StandardCharsets.UTF_8);
        Headers h = exchange.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, out.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(out);
        }
    }

    public static String ok(Object result) throws IOException {
        return JACKSON.writeValueAsString(Map.of("result", result));
    }

    public static String err(String message) throws IOException {
        return JACKSON.writeValueAsString(Map.of("error", message));
    }
}
