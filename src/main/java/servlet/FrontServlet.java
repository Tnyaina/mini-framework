package servlet;

import java.io.*;
import java.util.Map;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import util.Mapping;
import util.ModelView;

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
        Map<String, Mapping> urlMappings = (Map<String, Mapping>) getServletContext().getAttribute("urlMappings");

        Mapping mapping = urlMappings.get(key);

        if (mapping != null) {
            try {
                Object controllerInstance = mapping.getControllerClass()
                        .getDeclaredConstructor().newInstance();

                Object result = mapping.getMethod().invoke(controllerInstance);

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

                    request.getRequestDispatcher(mv.getView()).forward(request, response);
                }

            } catch (Exception e) {
                e.printStackTrace();
                response.setContentType("text/html; charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                PrintWriter out = response.getWriter();
                out.println("<h2>Erreur 500</h2>");
                out.println("<p>Erreur lors de l'invocation: " + e.getMessage() + "</p>");
            }
        } else {
            response.setContentType("text/html; charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            PrintWriter out = response.getWriter();
            out.println("<h2>Erreur 404 - Page non trouvee</h2>");
            out.println("<p>L'URL <strong>" + url + "</strong> n'est pas reconnue.</p>");
        }
    }
}