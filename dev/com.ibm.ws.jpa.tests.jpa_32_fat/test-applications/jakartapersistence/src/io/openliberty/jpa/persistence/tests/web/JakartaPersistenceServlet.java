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

import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.Product;
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
     * Specifies the precedence of null values within query result sets.
     * https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a5587
     *
     * @throws Exception
     */
    @Test
    public void testNullPrecedenceWithJPQL() throws Exception {
        deleteAllEntities(Product.class);
        Product product1 = Product.of("testSnapshot", "product1", 10.50f);
        Product product2 = Product.of(null, "product2", 20.50f);
        Product product3 = Product.of("sample products", "product3", 30.50f);
        tx.begin();
        em.persist(product1);
        em.persist(product2);
        em.persist(product3);
        tx.commit();

        /*
         * Specifies the precedence of null values within query result sets.
         */
        List<Product> productsNullFirst;
        try {

            tx.begin();
            productsNullFirst = em.createQuery("FROM Product ORDER BY description DESC NULLS FIRST",
                                               Product.class)
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertEquals(3, productsNullFirst.size());
        assertEquals("product2", productsNullFirst.get(0).name);

        /*
         * Null values occur at the end of the result set.
         */
        List<Product> productsNullLast;
        try {

            tx.begin();
            productsNullLast = em.createQuery("FROM Product ORDER BY description DESC NULLS LAST",
                                              Product.class)
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertEquals(3, productsNullLast.size());
        assertEquals("product2", productsNullLast.get(2).name);

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
