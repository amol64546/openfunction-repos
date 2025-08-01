import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import dev.openfunction.functions.Routable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// render this block
import com.github.f4b6a3.uuid.util.UuidValidator;
import java.lang.String;
//

public class Main extends Routable implements HttpFunction {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<String, Function<Map<String, Object>, Object>> pathHandlers = new HashMap<>();

    // render this block
    static {
        pathHandlers.put("/{"docString":null,"isNoArg":false,"isPublic":false,"isStatic":true,"name":"isValid","parameters":[{"isPrimitive":false,"name":"uuid","qualifiedName":"java.lang.String","type":"String"},{"isPrimitive":true,"name":"version","qualifiedName":"int","type":"int"}],"qualifiedName":null,"returnType":null,"signature":null}", Main::{"docString":null,"isNoArg":false,"isPublic":false,"isStatic":true,"name":"isValid","parameters":[{"isPrimitive":false,"name":"uuid","qualifiedName":"java.lang.String","type":"String"},{"isPrimitive":true,"name":"version","qualifiedName":"int","type":"int"}],"qualifiedName":null,"returnType":null,"signature":null});
    }

    //

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        String requestBody = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));
        Map<String, Object> body = mapper.readValue(requestBody, new TypeReference<>() {
        });
        Object result = pathHandlers.get(request.getPath())
                .apply(body);
        sendResponse(response, result);
    }

    @Override
    public String getPath() {
        return "/*";
    }

    private void sendResponse(HttpResponse response, int statusCode, Object result)
            throws IOException {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("result", result);
        responseBody.put("message", result);
        response.setStatusCode(statusCode);
        response.setContentType("application/json");
        response.getWriter().write(mapper.writeValueAsString(responseBody));
    }
}
