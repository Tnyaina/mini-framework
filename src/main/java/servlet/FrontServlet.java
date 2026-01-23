package servlet;

import java.io.*;
import java.util.Map;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.MultipartConfig;
import util.Mapping;
import util.ModelView;
import util.ParameterResolver;
import util.UrlMatcher;
import util.ApiResponse;
import util.JsonConverter;
import util.SecurityConfig;
import util.SecurityChecker;

@MultipartConfig(
    maxFileSize = 10485760,
    maxRequestSize = 20971520,
    fileSizeThreshold = 1048576
)
public class FrontServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        try {
            // Charger configuration sécurité
            SecurityConfig.load();
            
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

        @SuppressWarnings("unchecked")
        Map<String, Mapping> urlMappings = (Map<String, Mapping>) getServletContext().getAttribute("urlMappings");

        Mapping mapping = null;
        String matchedPattern = null;

        for (Map.Entry<String, Mapping> entry : urlMappings.entrySet()) {
            String mappingKey = entry.getKey();
            if (mappingKey.startsWith(httpMethod + ":")) {
                String pattern = mappingKey.substring(httpMethod.length() + 1);
                if (UrlMatcher.matches(pattern, url)) {
                    mapping = entry.getValue();
                    matchedPattern = pattern;
                    break;
                }
            }
        }

        if (mapping != null) {
            try {
                Object controllerInstance = mapping.getControllerClass()
                        .getDeclaredConstructor().newInstance();

                Map<String, String> pathVariables = UrlMatcher.extractPathVariables(matchedPattern, url);
                
                // Charger la session depuis HttpSession vers Map temporaire
                java.util.Map<String, Object> sessionMap = new java.util.HashMap<>();
                HttpSession httpSession = request.getSession(true);
                java.util.Enumeration<String> sessionAttrs = httpSession.getAttributeNames();
                while (sessionAttrs.hasMoreElements()) {
                    String key = sessionAttrs.nextElement();
                    sessionMap.put(key, httpSession.getAttribute(key));
                }
                request.setAttribute("__session_map__", sessionMap);
                
                // ===== VÉRIFICATION SÉCURITÉ =====
                SecurityChecker.SecurityResult securityCheck = 
                    SecurityChecker.checkAccess(mapping.getMethod(), sessionMap);
                
                if (!securityCheck.isAllowed()) {
                    response.setContentType("application/json; charset=UTF-8");
                    response.setStatus(securityCheck.getStatusCode());
                    PrintWriter out = response.getWriter();
                    
                    String errorJson = JsonConverter.toJson(
                        ApiResponse.error(securityCheck.getStatusCode(), securityCheck.getMessage())
                    );
                    out.print(errorJson);
                    out.flush();
                    return;
                }
                // ===== FIN VÉRIFICATION =====
                
                Object[] args = ParameterResolver.resolveParameters(mapping.getMethod(), request, pathVariables);

                // Trouver la Map injectée dans les arguments
                Map<String, Object> paramMap = null;
                for (Object arg : args) {
                    if (arg instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) arg;
                        // Vérifier que ce n'est pas le sessionMap
                        if (map != sessionMap) {
                            paramMap = map;
                            break;
                        }
                    }
                }

                Object result = mapping.getMethod().invoke(controllerInstance, args);

                // Persister le Map session modifié dans HttpSession
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> modifiedSession = 
                    (java.util.Map<String, Object>) request.getAttribute("__session_map__");
                if (modifiedSession != null) {
                    // 1. Supprimer les clés qui ont disparu du Map
                    java.util.Set<String> sessionKeys = new java.util.HashSet<>();
                    java.util.Enumeration<String> attrs = httpSession.getAttributeNames();
                    while (attrs.hasMoreElements()) {
                        sessionKeys.add(attrs.nextElement());
                    }
                    
                    for (String key : sessionKeys) {
                        if (!modifiedSession.containsKey(key)) {
                            httpSession.removeAttribute(key);
                        }
                    }
                    
                    // 2. Ajouter/Modifier les nouvelles valeurs
                    for (Map.Entry<String, Object> entry : modifiedSession.entrySet()) {
                        httpSession.setAttribute(entry.getKey(), entry.getValue());
                    }
                }

                // Vérifier si c'est une API REST
                boolean isRestApi = mapping.getMethod().isAnnotationPresent(annotation.RestAPI.class);

                if (isRestApi) {
                    // Mode REST API : retourner du JSON
                    response.setContentType("application/json; charset=UTF-8");
                    PrintWriter out = response.getWriter();

                    Object jsonData = null;

                    if (result instanceof ModelView) {
                        ModelView mv = (ModelView) result;
                        jsonData = mv.getData();
                    } else if (result instanceof ApiResponse) {
                        jsonData = result;
                    } else {
                        jsonData = ApiResponse.success(result);
                    }

                    String json = JsonConverter.toJson(jsonData);
                    out.print(json);
                    out.flush();

                } else {
                    // Mode classique : JSP
                    if (paramMap != null && !paramMap.isEmpty()) {
                        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                            request.setAttribute(entry.getKey(), entry.getValue());
                        }
                    }

                    if (result instanceof String) {
                        String view = (String) result;
                        String path = view.startsWith("/") ? view : "/" + view;
                        if (!path.contains("."))
                            path += ".jsp";
                        request.getRequestDispatcher(path).forward(request, response);

                    } else if (result instanceof ModelView) {
                        ModelView mv = (ModelView) result;
                        for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                            request.setAttribute(entry.getKey(), entry.getValue());
                        }
                        request.getRequestDispatcher("/" + mv.getView()).forward(request, response);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();

                boolean isRestApi = mapping != null &&
                        mapping.getMethod().isAnnotationPresent(annotation.RestAPI.class);

                if (isRestApi) {
                    response.setContentType("application/json; charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    PrintWriter out = response.getWriter();

                    String errorJson = util.JsonConverter.toJson(
                            util.ApiResponse.error(500, "Erreur serveur: " + e.getMessage()));
                    out.print(errorJson);
                    out.flush();
                } else {
                    response.setContentType("text/html; charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    PrintWriter out = response.getWriter();
                    out.println("<h2>Erreur 500</h2>");
                    out.println("<p>Erreur lors de l'invocation: " + e.getMessage() + "</p>");
                }
            }
        } else {
            response.setContentType("text/html; charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            PrintWriter out = response.getWriter();
            out.println("<h2>Erreur 404 - Page non trouvée</h2>");
            out.println("<p>L'URL <strong>" + url + "</strong> n'est pas reconnue.</p>");
        }
    }
}