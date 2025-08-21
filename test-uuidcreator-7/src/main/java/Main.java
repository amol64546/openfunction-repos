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

import com.github.f4b6a3.uuid.UuidCreator;

public class Main extends Routable implements HttpFunction {

  private static final ObjectMapper JACKSON = new ObjectMapper();
  private static final Map<String, Function<Map<String, Object>, Object>> pathHandlers =
      new HashMap<>();

  static {
    pathHandlers.put("/getTimeBased", Main::getTimeBased);
    pathHandlers.put("/getRandomBased", Main::getRandomBased);
  }

  private static Object getTimeBased(Map<String, Object> body) {
    return UuidCreator.getTimeBased();
  }

  private static Object getRandomBased(Map<String, Object> body) {
    return UuidCreator.getRandomBased();
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
