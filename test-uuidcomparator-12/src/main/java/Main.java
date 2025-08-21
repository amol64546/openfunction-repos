import handlers.InstanceHandler;
import handlers.SwaggerHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import dev.openfunction.functions.Routable;
import utils.InstanceUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.UUID;
import com.github.f4b6a3.uuid.util.UuidComparator;

public class Main extends Routable implements HttpFunction {

  private static final ObjectMapper JACKSON = new ObjectMapper();
  private static final Map<String, Function<Map<String, Object>, Object>> pathHandlers =
      new HashMap<>();

  static {
    pathHandlers.put("/compare", Main::compare);
  }

  private static Object compare(Map<String, Object> body) {
    UUID uuid1 = InstanceUtils.getInstance(body, "uuid1");
    UUID uuid2 = InstanceUtils.getInstance(body, "uuid2");
    UuidComparator instance = InstanceUtils.getInstance(body);
    return instance.compare(uuid1, uuid2);
  }

  @Override
  public void service(HttpRequest request, HttpResponse response) throws IOException {
    if (request.getPath().contains("/swagger-ui")) {
      SwaggerHandler.handleSwaggerUI(request, response);
    } else if (request.getPath().equals("/swagger.json")) {
      SwaggerHandler.handlerSwaggerJson(request, response);
    } else if (request.getPath().contains("/instances")) {
      InstanceHandler.handleInstanceCreation(request, response);
    } else if (pathHandlers.containsKey(request.getPath())) {
      String requestBody =
          request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      Map<String, Object> body =
          requestBody.isEmpty()
              ? new HashMap<>()
              : JACKSON.readValue(requestBody, new TypeReference<>() {});
      Object result = pathHandlers.get(request.getPath()).apply(body);
      sendResponse(response, result);
    } else {
      response.setStatusCode(404);
      response.getWriter().write("Path Not Found");
    }
  }

  @Override
  public String getPath() {
    return "/*";
  }

  private void sendResponse(HttpResponse response, Object result) throws IOException {
    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("result", result);
    response.setContentType("application/json");
    response.getWriter().write(JACKSON.writeValueAsString(responseBody));
  }
}
