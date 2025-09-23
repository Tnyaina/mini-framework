import java.io.*;
import java.net.URL;

import jakarta.servlet.*; 
import jakarta.servlet.http.*; 
 
public class FrontServlet extends HttpServlet { 
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException { 
        response.setContentType("text/html");
        String url = request.getRequestURI();
        String context = request.getContextPath();
        String path = url.substring(context.length());
        
        URL resource = getServletContext().getResource(path);
        if (resource != null && (path.endsWith(".jsp") || path.endsWith(".html"))) {
            RequestDispatcher rd = request.getRequestDispatcher(path);
            rd.forward(request, response);
        } else {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println("<html><body>");
            response.getWriter().println("<p>404 not found: " + url + "</p>");
            response.getWriter().println("</body></html>");
        }    
    }    
}
