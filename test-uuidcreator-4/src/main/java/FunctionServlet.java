import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FunctionServlet extends HttpServlet {
    private final Main function = new Main();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        try {
            function.service(
                    new HttpRequest() {
                        @Override
                        public String getMethod() {
                            return req.getMethod();
                        }

                        @Override
                        public String getUri() {
                            return req.getRequestURI();
                        }

                        @Override
                        public String getPath() {
                            return req.getPathInfo();
                        }

                        @Override
                        public Optional<String> getQuery() {
                            return Optional.empty();
                        }

                        @Override
                        public Map<String, List<String>> getQueryParameters() {
                            return Map.of();
                        }

                        @Override
                        public Map<String, HttpPart> getParts() {
                            return Map.of();
                        }

                        @Override
                        public Optional<String> getContentType() {
                            return req.getContentType() != null ?
                                    Optional.of(req.getContentType()) : Optional.empty();
                        }

                        @Override
                        public long getContentLength() {
                            return req.getContentLengthLong();
                        }

                        @Override
                        public Optional<String> getCharacterEncoding() {
                            return Optional.empty();
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            return req.getInputStream();
                        }

                        @Override
                        public java.io.BufferedReader getReader() throws java.io.IOException {
                            return req.getReader();
                        }

                        @Override
                        public Map<String, List<String>> getHeaders() {
                            return Map.of();
                        }
                        // Implement other methods as needed
                    },
                    new HttpResponse() {
                        @Override
                        public void setStatusCode(int code) {
                            resp.setStatus(code);
                        }

                        @Override
                        public int getStatusCode() {
                            return 0;
                        }

                        @Override
                        public void setStatusCode(int code, String message) {
                            resp.setStatus(code, message);
                        }

                        @Override
                        public void setContentType(String type) {
                            resp.setContentType(type);
                        }

                        @Override
                        public Optional<String> getContentType() {
                            return Optional.empty();
                        }

                        @Override
                        public void appendHeader(String header, String value) {
                            resp.addHeader(header, value);
                        }

                        @Override
                        public Map<String, List<String>> getHeaders() {
                            return Map.of();
                        }

                        @Override
                        public java.io.OutputStream getOutputStream() throws java.io.IOException {
                            return resp.getOutputStream();
                        }

                        @Override
                        public BufferedWriter getWriter() throws IOException {
                            return new BufferedWriter(resp.getWriter());
                        }
                    }
            );
        } catch (Exception e) {
            resp.setStatus(500);
            try {
                resp.getWriter().write("Internal error: " + e.getMessage());
            } catch (Exception ignore) {
            }
        }
    }
}