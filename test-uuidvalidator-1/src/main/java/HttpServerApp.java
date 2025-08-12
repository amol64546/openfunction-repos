import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.github.f4b6a3.uuid.util.UuidComparator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import utils.HttpUtils;
import utils.InstanceUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

public class HttpServerApp {

    private static final ObjectMapper JACKSON = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/instances", new InstanceHandler());
        // Register handlers for specific paths
        server.createContext("/compare", new CompareHandler());
        server.createContext("/getRandomBased", new GetRandomBasedHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        System.out.println("HTTP server started on http://localhost:" + port);
        server.start();
    }

    // ===== Handlers =====

    static class GetRandomBasedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Object result = UuidCreator.getRandomBased();
                HttpUtils.send(exchange, 200, HttpUtils.ok(result));
            } catch (Exception e) {
                HttpUtils.send(exchange, 500, HttpUtils.err(e.getMessage()));
            }
        }
    }

    static class CompareHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            try {
                String body = HttpUtils.readBody(exchange);
                Map<String, Object> map = JACKSON.readValue(body, new TypeReference<>() {
                });

                // instance
                UuidComparator comparator = InstanceUtils.getInstance(map);
                // params
                UUID uuid1 = InstanceUtils.getInstance(map, "uuid1");
                UUID uuid2 = InstanceUtils.getInstance(map, "uuid2");

                Object result = comparator.compare(uuid1, uuid2);

                HttpUtils.send(exchange, 200, HttpUtils.ok(result));
            } catch (Exception e) {
                HttpUtils.send(exchange, 500, HttpUtils.err(e.getMessage()));
            }
        }
    }


}
