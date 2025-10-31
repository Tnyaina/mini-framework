package servlet;

import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import annotation.Get;
import annotation.Post;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import util.ControllerScanner;
import util.Mapping;

public class FrontServlet extends HttpServlet {
    
    private Map<String, Mapping> urlMappings = new HashMap<>();
    
    @Override
    public void init() throws ServletException {
        try {
            
            List<Class<?>> controllers = ControllerScanner.getAllControllers();
            System.out.println("Controleurs trouves: " + controllers.size());
            
            for (Class<?> controller : controllers) {
                System.out.println("Controleur: " + controller.getName());
                for (Method method : controller.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Get.class)) {
                        String url = method.getAnnotation(Get.class).value();
                        System.out.println("  GET " + url + " -> " + method.getName());
                        urlMappings.put("GET:" + url, new Mapping(controller, method));
                    }
                    if (method.isAnnotationPresent(Post.class)) {
                        String url = method.getAnnotation(Post.class).value();
                        System.out.println("  POST " + url + " -> " + method.getName());
                        urlMappings.put("POST:" + url, new Mapping(controller, method));
                    }
                }
            }
            System.out.println("Total mappings: " + urlMappings.size());
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
        Mapping mapping = urlMappings.get(key);
        
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        if (mapping != null) {
            // URL reconnue
            out.println("<h2>URL reconnue</h2>");
            out.println("<p><strong>Controleur:</strong> " + mapping.getControllerClass().getSimpleName() + "</p>");
            out.println("<p><strong>Methode:</strong> " + mapping.getMethod().getName() + "</p>");
        } else {
            // URL non reconnue -> 404
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("<h2>Erreur 404 - Page non trouvee</h2>");
            out.println("<p>L'URL <strong>" + url + "</strong> n'est pas reconnue.</p>");
        }
    }
}