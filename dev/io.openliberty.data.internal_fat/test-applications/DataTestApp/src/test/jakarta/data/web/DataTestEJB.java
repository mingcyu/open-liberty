/**
 *
 */
package test.jakarta.data.web;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * A singleton startup EJB that is included inside the war file.
 */
@Startup
@Singleton
public class DataTestEJB {

    //TODO renable after fixing deadlock in DataProvider
//    @Inject
//    Animals animals;

    @PostConstruct
    public void init() {
//        animals.findAll();
    }
}
