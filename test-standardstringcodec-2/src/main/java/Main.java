import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.codec.UrnCodec;
import com.github.f4b6a3.uuid.util.UuidValidator;
import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import dev.openfunction.functions.Routable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Main extends Routable implements HttpFunction {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {
    Object result;
    int statusCode = 200;
    String path = request.getPath();

    String requestBody = request.getReader().lines()
        .collect(Collectors.joining(System.lineSeparator()));
    Map<String, Object> body = mapper.readValue(requestBody, new TypeReference<>() {
    });

    try {
      switch (path) {
        case "/isUuidUrn" :
          if (1 != body.size()) {
            result = "Number of parameters in request body does not match method signature.";
            statusCode = 400;
            break;
          }
          String string = "";
          if (body.get("string") instanceof String) {
            string = (String) body.get("string");
          }
          result = (Object) UrnCodec.isUuidUrn(string);
          break;

        case "/isValid" :
          if (2 != body.size()) {
            result = "Number of parameters in request body does not match method signature.";
            statusCode = 400;
            break;
          }
          String uuid = "";
          int version = 0;
          if (body.get("uuid") instanceof String) {
            uuid = (String) body.get("uuid");
          }
          if (body.get("version") instanceof Integer) {
            version = (int) body.get("version");
          }
          result = (Object) UuidValidator.isValid(uuid, version);
          break;

        default:
          result = "Invalid endpoint: " + path;
          statusCode = 404;
          break;
      }

    } catch (Exception e) {
      result = e.getMessage();
      statusCode = 500;
    }
    sendJsonResponse(response, statusCode, result);
  }


  @Override
  public String getPath() {
    return "/*";
  }

  @Override
  public String[] getMethods() {
    return new String[]{"GET"};
  }

  ;

  private void sendJsonResponse(HttpResponse response, int statusCode, Object result)
      throws IOException {
    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("statusCode", Optional.of(statusCode));
    if (statusCode == 200) {
      responseBody.put("status", "success");
      responseBody.put("data", result);
    } else {
      responseBody.put("status", "error");
      responseBody.put("message", result);
    }

    response.setStatusCode(statusCode);
    response.setContentType("application/json");
    response.getWriter().write(mapper.writeValueAsString(responseBody));
  }
}
