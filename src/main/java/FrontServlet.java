import java.io.*;
import java.net.URL;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

public class FrontServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = request.getRequestURI();
        String resourceName = url.substring(url.lastIndexOf("/") + 1);
        
        if (chercherRessources(resourceName, request, response)) {
            // Ressource trouvée et transférée
        } else {
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = request.getRequestURI();
        String resourceName = url.substring(url.lastIndexOf("/") + 1);
        
        if (chercherRessources(resourceName, request, response)) {
            // Ressource trouvée et transférée
        } else {
        }
    }

    public boolean chercherRessources(String resourceName, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Vérifier si la ressource existe réellement
        String resourcePath = getServletContext().getRealPath("/" + resourceName);
        
        if (resourcePath != null && new File(resourcePath).exists()) {
            RequestDispatcher dispatcher = getServletContext().getNamedDispatcher("default");
            if (dispatcher != null) {
                dispatcher.forward(request, response);
                return true;
            }
        } else {
            String url = request.getRequestURI();
            response.getWriter().write(url);
        }
        
        return false;
    }
}