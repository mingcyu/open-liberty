package spring.test.init.lib;


import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.springframework.web.WebApplicationInitializer;

public class LibInit implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {      
        System.out.println("AnnotationScanInJarTest test output: onStartup method found via jar file");
    }

}
