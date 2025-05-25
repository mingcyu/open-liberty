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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import io.openliberty.jpa.persistence.tests.models.Person;
import io.openliberty.jpa.persistence.tests.models.Organization;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JakartaPersistence32")
public class JakartaPersistenceServlet extends FATServlet {
    @PersistenceContext(unitName = "JakartaPersistenceUnit")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Test
    public void testSetOperationsJPQL() throws Exception{
        deleteAllEntities(Person.class);
        deleteAllEntities(Organization.class);

        Person person1 = Person.of(1L, "AAA");
        Person person2 = Person.of(2L, "BBB");
        tx.begin();
        em.persist(person1);
        em.persist(person2);
        tx.commit();

        Organization org1 = Organization.of(3L, "BBB");
        Organization org2 = Organization.of(4L, "CCC");
        tx.begin();
        em.persist(org1);
        em.persist(org2);
        tx.commit();

        tx.begin();
        try {
            // UNION
            List<String> unionResult = em.createQuery(
                                                    "SELECT p.name FROM Person p " +
                                                    "UNION " +
                                                    "SELECT o.name FROM Organization o", String.class)
                            .getResultList();
            assertEquals(Arrays.asList("AAA", "BBB", "CCC"), unionResult);

            // INTERSECT
            List<String> intersectResult = em.createQuery(
                                                        "SELECT p.name FROM Person p " +
                                                        "INTERSECT " +
                                                        "SELECT o.name FROM Organization o", String.class)
                            .getResultList();
            assertEquals(Arrays.asList("BBB"), intersectResult);

            // EXCEPT
            List<String> exceptResult = em.createQuery(
                                                    "SELECT p.name FROM Person p " +
                                                    "EXCEPT " +
                                                    "SELECT o.name FROM Organization o", String.class)
                            .getResultList();
            assertEquals(Arrays.asList("AAA"), exceptResult);
            tx.commit();
        }
        catch(Exception e) {
            tx.rollback();
            throw e;
        }
    }

    private void deleteAllEntities(Class<?> cls) throws Exception {
        tx.begin();
        em.createQuery("DELETE FROM " + cls.getSimpleName())
                        .executeUpdate();
        tx.commit();
    }
}