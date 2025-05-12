package websphere.servlet.listener;

import javax.servlet.ServletContext;

import com.ibm.websphere.servlet.event.ApplicationEvent;
import com.ibm.websphere.servlet.event.ApplicationListener;

/*
 * WebSphere Servlet Event ApplicationListener.
 * Register using the WebContainer's "listeners" in server.xml
 *
 *      <webContainer listeners="websphere.servlet.listener.WebSphereApplicationListener"/>
 */

public class WebSphereApplicationListener implements ApplicationListener {
    private static final String CLASS_NAME = WebSphereApplicationListener.class.getName();
    public static final String WEBSPHERE_ATT = "WEBSPHERE_API";

    ServletContext context = null;

    void init() {
        log("WebSphere ApplicationListener Demo");
    }

    @Override
    public void onApplicationAvailableForService(ApplicationEvent arg0) {
        log("onApplicationAvailableForService, ServletContext from ApplicationEvent [" + context);
    }

    @Override
    public void onApplicationEnd(ApplicationEvent arg0) {
        log("onApplicationEnd, ServletContext from ApplicationEvent [" + context);
    }

    @Override
    public void onApplicationStart(ApplicationEvent arg0) {
        context = arg0.getServletContext();
        log("onApplicationStart, ServletContext using WebSphere ApplicationEvent [" + context);

        //servlet will retrieve this context attribute
        context.setAttribute(WebSphereApplicationListener.WEBSPHERE_ATT, "WEBSPHERE Servlet API ApplicationListener.");
        log("onApplicationStart, set context attribute using WebSphere API [" + context.getAttribute(WEBSPHERE_ATT) + "]");
    }

    @Override
    public void onApplicationUnavailableForService(ApplicationEvent arg0) {
        log("onApplicationUnavailableForService, ServletContext from ApplicationEvent [" + context);
    }

    private void log(String s) {
        System.out.println(CLASS_NAME + ": " + s);
    }
}
