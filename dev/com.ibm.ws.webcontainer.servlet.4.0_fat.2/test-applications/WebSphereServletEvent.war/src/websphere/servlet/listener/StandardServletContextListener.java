package websphere.servlet.listener;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/*
 * Standard Servlet API.
 * Register either via @WebListener
 *      or web.xml <listener> element, for example:
 *              <listener>
                  <listener-class>com.webtier.listener.StandardServletContextListener</listener-class>
                </listener>
 */

@WebListener
public class StandardServletContextListener implements ServletContextListener {
    private static final String CLASS_NAME = StandardServletContextListener.class.getName();
    public static final String STANDARD_ATT = "STANDARD_API";
    ServletContext context = null;

    void init() {
        log("Standard API ServletContextListener init");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log("contextDestroyed, ServletContext using standard ServletContextListener [" + context + "]");
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        context = sce.getServletContext();

        log("contextInitialized, ServletContext using standard ServletContextListener [" + context + "]");
        context.setAttribute(StandardServletContextListener.STANDARD_ATT, "STANDARD Servlet API ServletContextListener.");
        log("contextInitialized, Set context attribute using standard API [" + context.getAttribute(StandardServletContextListener.STANDARD_ATT) + "]");
    }

    private void log(String s) {
        System.out.println(CLASS_NAME + ": " + s);
    }
}
