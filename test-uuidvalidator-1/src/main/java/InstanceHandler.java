import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Arg;
import models.CreateInstanceRequest;
import models.CreateInstanceResponse;
import utils.HttpUtils;
import utils.InstanceUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class InstanceHandler implements HttpHandler {
    private static final Gson GSON = new Gson();
    private static final ObjectMapper JACKSON = new ObjectMapper();

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

