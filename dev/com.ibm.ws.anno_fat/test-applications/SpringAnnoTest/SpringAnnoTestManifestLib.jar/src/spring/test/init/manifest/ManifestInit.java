package spring.test.init.manifest;


import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.springframework.web.WebApplicationInitializer;

public class ManifestInit implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {      
        System.out.println("AnnotationScanInJarTest test output: onStartup method found via manifest lib file");
    }

}
