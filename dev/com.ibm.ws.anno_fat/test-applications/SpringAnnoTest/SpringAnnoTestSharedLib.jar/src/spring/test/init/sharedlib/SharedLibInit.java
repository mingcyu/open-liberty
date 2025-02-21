package spring.test.init.sharedlib;


import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.springframework.web.WebApplicationInitializer;

public class SharedLibInit implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {      
        System.out.println("AnnotationScanInJarTest test output: onStartup method found via shared library file");
    }

}
