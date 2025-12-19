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

@MultipartConfig(
    maxFileSize = 10485760,      // 10 MB max par fichier
    maxRequestSize = 20971520,   // 20 MB max pour la requête totale
    fileSizeThreshold = 1048576  // 1 MB - seuil pour stockage temporaire
)
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
                Object[] args = ParameterResolver.resolveParameters(mapping.getMethod(), request, pathVariables);

                // Trouver la Map injectée dans les arguments
                Map<String, Object> paramMap = null;
                for (Object arg : args) {
                    if (arg instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) arg;
                        paramMap = map;
                        break;
                    }
                }

                Object result = mapping.getMethod().invoke(controllerInstance, args);

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