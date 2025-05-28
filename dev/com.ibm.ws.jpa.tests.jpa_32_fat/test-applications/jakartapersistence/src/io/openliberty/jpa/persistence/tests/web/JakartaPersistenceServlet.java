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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.ConcatEntity;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
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
     * Jakarta 3.2 version supports concat() overload accepting list of expressions ie., concat(List<Expression<String>> expressions)
     *
     * https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/criteria/criteriabuilder
     * https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/criteria/criteriabuilder#concat(java.util.List)
     *
     * @throws Exception
     */
    @Test
    public void testConcatInWhereCriteriaQuery() throws Exception {
        deleteAllEntities(ConcatEntity.class);

        ConcatEntity concatEntity1 = new ConcatEntity();
        concatEntity1.firstName = "John";
        concatEntity1.lastName = "Jacobs";
        concatEntity1.ssn_id = 1L;

        ConcatEntity concatEntity2 = new ConcatEntity();
        concatEntity2.firstName = "Steve";
        concatEntity2.lastName = "Smith";
        concatEntity2.ssn_id = 2L;

        tx.begin();
        em.persist(concatEntity1);
        em.persist(concatEntity2);
        tx.commit();

        tx.begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ConcatEntity> cquery = cb.createQuery(ConcatEntity.class);
        Root<ConcatEntity> root = cquery.from(ConcatEntity.class);
        ParameterExpression<String> strParam1 = cb.parameter(String.class);

        List<jakarta.persistence.criteria.Expression<String>> concatExpression = new ArrayList<>();
        concatExpression.add(root.get("firstName"));
        concatExpression.add(cb.literal(" "));
        concatExpression.add(root.get("lastName"));

        cquery.select(root)
                        .where(cb.equal(cb.concat(concatExpression), strParam1));

        // Use of concat in where clause: Matching case
        List<ConcatEntity> person = em.createQuery(cquery)
                        .setParameter(strParam1, "John Jacobs")
                        .getResultList();
        assertEquals("Expected 1 record that matches full name 'John Jacobs'", 1, person.size());

        // Use of concat in where clause: No Match case
        List<ConcatEntity> personNoMatch = em.createQuery(cquery)
                        .setParameter(strParam1, "John Jacob")
                        .getResultList();
        assertEquals("Expected 0 record that matches full name 'John Jacob'", 0, personNoMatch.size());
        tx.commit();
    }

    /**
     * Jakarta 3.2 version supports concat() overload accepting list of expressions ie., concat(List<Expression<String>> expressions)
     *
     * https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/criteria/criteriabuilder
     * https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/criteria/criteriabuilder#concat(java.util.List)
     *
     * @throws Exception
     */
    @Test
    public void testConcatCriteriaQuery() throws Exception {
        deleteAllEntities(ConcatEntity.class);

        ConcatEntity concatEntity1 = new ConcatEntity();
        concatEntity1.firstName = "John";
        concatEntity1.lastName = "Jacobs";
        concatEntity1.ssn_id = 1L;

        ConcatEntity concatEntity2 = new ConcatEntity();
        concatEntity2.firstName = "Steve";
        concatEntity2.lastName = "Smith";
        concatEntity2.ssn_id = 2L;

        tx.begin();
        em.persist(concatEntity1);
        em.persist(concatEntity2);
        tx.commit();

        tx.begin();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> cquery = cb.createQuery(String.class);
        Root<ConcatEntity> root = cquery.from(ConcatEntity.class);

        // use concat on queried result
        List<jakarta.persistence.criteria.Expression<String>> concatExpression = List.of(root.get("firstName"),
                                                                                         cb.literal(" "),
                                                                                         root.get("lastName"));

        cquery.select(cb.concat(concatExpression));
        cquery.orderBy(cb.desc(root.get("firstName")));

        List<String> fullname = em.createQuery(cquery).getResultList();
        System.out.println("****** testConcatCriteriaQuery: fullname: " + fullname);
        assertEquals("Expected full name 'John Jacobs' for first record", "John Jacobs", fullname.get(1));
        assertEquals("Expected full name 'Steve Smith' for second record", "Steve Smith", fullname.get(0));
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
