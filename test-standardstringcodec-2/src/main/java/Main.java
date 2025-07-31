import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import dev.openfunction.functions.Routable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.github.f4b6a3.uuid.codec.StandardStringCodec;

public class Main extends Routable implements HttpFunction {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Override
  public void service(HttpRequest request, HttpResponse response) throws Exception {
    Object result;
    int statusCode = 200;
    String path = request.getPath();

    try {
      switch (path) {
          case "/decode" :
              String requestBody = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));

              // Map to store the request parameters (for dynamically calling methods)
              Map<String, Object> body = mapper.readValue(requestBody,
                 new TypeReference<Map<String, Object>>() {
                 });
              // Ensure the number of parameters in the request body matches the method signature
              if (1 != body.size()){
                 // Set result to an error message and break out of the case
                 result = "Number of parameters in request body does not match method signature.";
                 statusCode = 400; // Set status to 400 for bad request
                 break;
              }

              // Prepare parameters dynamically
              Object[] params = new Object[1];

              // Check if the parameter exists in the request body
              // Set result to an error message and break out of the case
              result = "Missing parameter: " + "string";
              statusCode = 400; // Set status to 400 for bad request
              break;

              // Call the method based on whether it is static or not
              result = StandardStringCodec.decode(java.lang.String string);
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
    responseBody.put("statusCode", statusCode);
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
