import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import com.github.f4b6a3.uuid.util.UuidComparator;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import models.Arg;
import models.CreateInstanceRequest;
import models.CreateInstanceResponse;
import utils.HttpUtils;
import utils.InstanceUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class HttpServerApp {

    private static final ObjectMapper JACKSON = new ObjectMapper();
    private static final Gson GSON = new Gson();

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

    static class InstanceHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            try {
                String body = HttpUtils.readBody(exchange);
                CreateInstanceRequest req = JACKSON.readValue(body, CreateInstanceRequest.class);

                if (req.kind == null || req.kind.isBlank()) {
                    HttpUtils.send(exchange, 400, HttpUtils.err("Request kind is required"));
                    return;
                }

                // If factory block present, use it
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
                    CreateInstanceResponse resp = new CreateInstanceResponse(req.className, b64);
                    HttpUtils.send(exchange, 200, JACKSON.writeValueAsString(resp));
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

                    CreateInstanceResponse resp = new CreateInstanceResponse(aClass.getName(), b64);
                    HttpUtils.send(exchange, 200, JACKSON.writeValueAsString(resp));
                } else {
                    HttpUtils.send(exchange, 400, HttpUtils.err("Invalid request kind: " + req.kind));
                }
            } catch (Exception e) {
                HttpUtils.send(exchange, 500, HttpUtils.err(e.getMessage()));
            }
        }
    }


}
