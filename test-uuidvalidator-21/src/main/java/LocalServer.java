import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class LocalServer {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8081);
        ServletContextHandler handler = new ServletContextHandler();
        handler.addServlet(FunctionServlet.class, "/*");
        server.setHandler(handler);
        server.start();
        System.out.println("Server started at http://localhost:8081");
        server.join();
    }
}