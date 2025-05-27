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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.Priority;
import io.openliberty.jpa.persistence.tests.models.Ticket;
import io.openliberty.jpa.persistence.tests.models.TicketStatus;
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
     * In previous version, Enumerated annotations were used for mapping Java Enum types to database column values.
     *
     * The Annotation @Enumerated are used with EnumType (ORDINAL or STRING)
     * EnumeratedValue in 3.2, Specifies that an annotated field of a Java enum type is the source of database column values for an enumerated mapping.
     * The annotated field must be declared final, and must be of type:
     * byte, short, or int for EnumType.ORDINAL, or
     * String for EnumType.STRING.
     * https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/enumeratedvalue
     *
     * @throws Exception
     */
    @Test
    public void testEnumeratedValue() throws Exception {

        Ticket ticket1 = Ticket.of(1, "ticket1", TicketStatus.OPEN, Priority.HIGH);
        Ticket ticket2 = Ticket.of(2, "ticket2", TicketStatus.CLOSED, Priority.LOW);
        Ticket ticket3 = Ticket.of(3, "ticket3", TicketStatus.CANCELLED, Priority.MEDIUM);

        // Checking SQL logs whether the mapping is done as below in the insert queries
        // TicketStatus.OPEN ENUM property value will be mapped to Table column value 0
        // Priority.HIGH property value will be mapped to Table column value 'H'
        tx.begin();
        em.persist(ticket1);
        em.persist(ticket2);
        em.persist(ticket3);
        tx.commit();

        /*
         * The INSERT statements present in the log is missing value mapping:
         *
         *
         *
         * [5/27/25, 18:28:22:002 IST] 00000051 id=00000000 eclipselink.query 3 [eclipselink.query] Execute query InsertObjectQuery(id=1 name=ticket1 status=OPEN priority=HIGH)
         * [5/27/25, 18:28:22:003 IST] 00000051 id=00000000 eclipselink.sql 3 [eclipselink.sql] INSERT INTO TICKET (ID, NAME, PRIORITY, STATUS) VALUES (?, ?, ?, ?)
         * bind => [1, ticket1, HIGH, 0]
         * [5/27/25, 18:28:22:011 IST] 00000051 id=00000000 eclipselink.query 3 [eclipselink.query] Execute query InsertObjectQuery(id=2 name=ticket2 status=CLOSED priority=LOW)
         * [5/27/25, 18:28:22:011 IST] 00000051 id=00000000 eclipselink.sql 3 [eclipselink.sql] INSERT INTO TICKET (ID, NAME, PRIORITY, STATUS) VALUES (?, ?, ?, ?)
         * bind => [2, ticket2, LOW, 1]
         * [5/27/25, 18:28:22:012 IST] 00000051 id=00000000 eclipselink.query 3 [eclipselink.query] Execute query InsertObjectQuery(id=3 name=ticket3 status=CANCELLED
         * priority=MEDIUM)
         * [5/27/25, 18:28:22:012 IST] 00000051 id=00000000 eclipselink.sql 3 [eclipselink.sql] INSERT INTO TICKET (ID, NAME, PRIORITY, STATUS) VALUES (?, ?, ?, ?)
         * bind => [3, ticket3, MEDIUM, 2]
         *
         */

        // Persisted Values in MySQL Server do not match the specification description: https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/enumeratedvalue
        //        +----+---------+----------+--------+
        //        | ID | NAME    | PRIORITY | STATUS |
        //        +----+---------+----------+--------+
        //        |  1 | ticket1 | HIGH     |      0 |
        //        |  2 | ticket2 | LOW      |      1 |
        //        |  3 | ticket3 | MEDIUM   |      2 |
        //        +----+---------+----------+--------+

        tx.begin();
        List<Ticket> results = em.createQuery("SELECT t FROM Ticket t ORDER BY t.id", Ticket.class).getResultList();
        tx.commit();

        System.out.println("***** testEnumeratedValue results: " + results);
        // Assert against status value of first element
        assertEquals(TicketStatus.OPEN, results.get(0).getStatus());
        assertFalse(TicketStatus.CLOSED.equals(results.get(0).getStatus()));
        assertFalse(TicketStatus.CANCELLED.equals(results.get(0).getStatus()));
        // Assert against status value of second element
        assertEquals(TicketStatus.CLOSED, results.get(1).getStatus());
        assertFalse(TicketStatus.OPEN.equals(results.get(1).getStatus()));
        assertFalse(TicketStatus.CANCELLED.equals(results.get(1).getStatus()));
        // Assert against status value of third element
        assertEquals(TicketStatus.CANCELLED, results.get(2).getStatus());
        assertFalse(TicketStatus.OPEN.equals(results.get(2).getStatus()));
        assertFalse(TicketStatus.CLOSED.equals(results.get(2).getStatus()));
        // Assert against priority value of first element
        assertEquals(Priority.HIGH, results.get(0).getPriority());
        assertFalse(Priority.MEDIUM.equals(results.get(0).getPriority()));
        assertFalse(Priority.LOW.equals(results.get(0).getPriority()));
        // Assert against priority value of second element
        assertEquals(Priority.LOW, results.get(1).getPriority());
        assertFalse(Priority.HIGH.equals(results.get(1).getPriority()));
        assertFalse(Priority.MEDIUM.equals(results.get(1).getPriority()));
        // Assert against priority value of third element
        assertEquals(Priority.MEDIUM, results.get(2).getPriority());
        assertFalse(Priority.HIGH.equals(results.get(2).getPriority()));
        assertFalse(Priority.LOW.equals(results.get(2).getPriority()));

    }

}
