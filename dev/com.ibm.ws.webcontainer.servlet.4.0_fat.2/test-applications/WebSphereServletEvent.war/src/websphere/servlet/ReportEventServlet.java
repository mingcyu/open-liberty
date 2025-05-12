package websphere.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import websphere.servlet.listener.StandardServletContextListener;
import websphere.servlet.listener.WebSphereApplicationListener;

/**
 * Tests the legacy WebSphere servlet event API
 *
 * https://openliberty.io/docs/latest/reference/javadoc/api/servlet-4.0.com.ibm.websphere.servlet.event.html
 *
 * These APIs were from WAS 4.0+ time frame. Application should use the servlet standard APIs instead.
 *
 * Since these APIs are still around, this test is added to cover the test gap in this area.
 */

@WebServlet(urlPatterns = "/ServletEvent")
public class ReportEventServlet extends HttpServlet {
    private static final String CLASS_NAME = ReportEventServlet.class.getName();

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletOutputStream outputStream = resp.getOutputStream();
        _log("doGet");

        //retrieve the context attributes set by standard and WebSphere servlet API.
        String WebSphereAPI = (String) req.getServletContext().getAttribute(WebSphereApplicationListener.WEBSPHERE_ATT);
        String StandardAPI = (String) req.getServletContext().getAttribute(StandardServletContextListener.STANDARD_ATT);

        outputStream.println("Context attribute from WebSphere API [" + WebSphereAPI + "]");
        outputStream.println("Context attribute from Standard API [" + StandardAPI + "]");
    }

    private void _log(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }
}
