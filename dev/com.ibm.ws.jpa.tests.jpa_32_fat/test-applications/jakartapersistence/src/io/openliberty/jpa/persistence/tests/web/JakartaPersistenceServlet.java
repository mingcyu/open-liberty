/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.openliberty.jpa.persistence.tests.models.User;

import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JakartaPersistence32")
public class JakartaPersistenceServlet extends FATServlet {
    @PersistenceContext(unitName = "JakartaPersistenceUnit")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void testSetOperationsJPQL() {
        // UNION
        List<String> unionResult = em.createQuery(
                                                  "SELECT p.name FROM Person p " +
                                                  "UNION " +
                                                  "SELECT o.name FROM Organization o", String.class)
                        .getResultList();
        assertNotNull(unionResult);

        // INTERSECT
        List<String> intersectResult = em.createQuery(
                                                      "SELECT p.name FROM Person p " +
                                                      "INTERSECT " +
                                                      "SELECT o.name FROM Organization o", String.class)
                        .getResultList();
        assertNotNull(intersectResult);

        // EXCEPT
        List<String> exceptResult = em.createQuery(
                                                   "SELECT p.name FROM Person p " +
                                                   "EXCEPT " +
                                                   "SELECT o.name FROM Organization o", String.class)
                        .getResultList();
        assertNotNull(exceptResult);
    }

     /**
     * Method for testing || in JPQL queries.
     * @throws Exception
     */
    @Test
    public void testJpqlConcat() throws Exception {
        deleteAllEntities(User.class);

        User user = new User();
        user.firstName = "John";
        user.lastName = "Doe";
        user.userId = 191111233l;

        tx.begin();
        em.persist(user);
        tx.commit();

        try{
            String concatJPQL = "SELECT u.firstName || ' ' || u.lastName FROM User u where u.lastName = ?1" ;
	        String fullName = em.createQuery(concatJPQL,String.class).setParameter(1, "Doe")
	            					.getSingleResult();

            assertEquals("John Doe", fullName);

        }catch (Exception e) {
            throw e;
        }
    }

    /**
     * Utility method to drop all entities from table.
     *
     * Order to tests is not guaranteed and thus we should be pessimistic and
     * delete all entities when we reuse an entity between tests.
     *
     * @param clazz - the entity class
     */
    private void deleteAllEntities(Class<?> clazz) throws Exception {
        tx.begin();
        em.createQuery("DELETE FROM " + clazz.getSimpleName())
                        .executeUpdate();
        tx.commit();
    }
}
