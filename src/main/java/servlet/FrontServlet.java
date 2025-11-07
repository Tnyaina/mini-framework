package servlet;

import java.io.*;
import java.util.Map;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import util.Mapping;

public class FrontServlet extends HttpServlet {
    
    @Override
    public void init() throws ServletException {
        try {
            Map<String, Mapping> mappings = Mapping.scanControllers();
            getServletContext().setAttribute("urlMappings", mappings);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Erreur scan", e);
        }
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response, "GET");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        processRequest(request, response, "POST");
    }
    
    private void processRequest(HttpServletRequest request, HttpServletResponse response, String httpMethod) 
            throws ServletException, IOException {
        
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String url = uri.substring(contextPath.length());
        
        String key = httpMethod + ":" + url;
        
        @SuppressWarnings("unchecked")
        Map<String, Mapping> urlMappings = 
            (Map<String, Mapping>) getServletContext().getAttribute("urlMappings");
        
        Mapping mapping = urlMappings.get(key);
        
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        if (mapping != null) {
            try {
                // Créer instance du contrôleur
                Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
                
                // Invoquer la méthode
                Object result = mapping.getMethod().invoke(controllerInstance);
                
                // Afficher le résultat
                if (result instanceof String) {
                    out.println((String) result);
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("<h2>Erreur 500</h2>");
                out.println("<p>Erreur lors de l'invocation: " + e.getMessage() + "</p>");
            }
        } else {
            // URL non reconnue -> 404
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("<h2>Erreur 404 - Page non trouvee</h2>");
            out.println("<p>L'URL <strong>" + url + "</strong> n'est pas reconnue.</p>");
        }
    }
}