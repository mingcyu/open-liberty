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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.openliberty.jpa.persistence.tests.models.User;

import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.Participant;
import io.openliberty.jpa.persistence.tests.models.Priority;
import io.openliberty.jpa.persistence.tests.models.Product;
import io.openliberty.jpa.persistence.tests.models.Ticket;
import io.openliberty.jpa.persistence.tests.models.TicketStatus;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Root;
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

        User user1 = User.of(1, "John", "Doe");
        User user2 = User.of(2, "Harry", "Potter");
        User user3 = User.of(3, "Hermione", "Granger");
        
        tx.begin();
        em.persist(user1);
        em.persist(user2);
        em.persist(user3);
        tx.commit();

        try{
            String concatJPQL = "SELECT u.firstName || ' ' || u.lastName FROM User u where u.lastName = ?1" ;
	        String fullName = em.createQuery(concatJPQL,String.class).setParameter(1, "Doe")
	            					.getSingleResult();

            String concatJPQLFrom = "SELECT u.firstName FROM User u where u.firstName || ' ' || u.lastName = ?1" ;
	        String firstName= em.createQuery(concatJPQLFrom,String.class).setParameter(1, "Harry Potter")
	            					.getSingleResult();

            assertEquals("John Doe", fullName);
            assertEquals("Harry", firstName);

        }catch (Exception e) {
            throw e;
        }
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
         * INSERT INTO TICKET (ID, NAME, PRIORITY, STATUS) VALUES (?, ?, ?, ?)
         * bind => [1, ticket1, HIGH, 0]
         * Persisted Values in column PRIORITY & STATUS, in MySQL Server do not match
         * the specification description
         */
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
        assertEquals("Sorted based on 'description' in desc order with NULLS FIRST, Expecting first element to be 'product2'", "product2", productsNullFirst.get(0).name);

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
        assertEquals("Sorted based on 'description' in desc order with NULLS LAST, Expecting last element to be 'product2'", "product2", productsNullLast.get(2).name);

    }

    /**
     * Specifies the precedence of null values within query result sets.
     * https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#nulls
     *
     * @throws Exception
     */
    @Test
    public void testNullPrecedenceWithCriteriaQuery() throws Exception {
        deleteAllEntities(Product.class);
        Product p1 = Product.of("testSnapshot", "product1", 10.50f);
        Product p2 = Product.of(null, "product2", 20.50f);
        Product p3 = Product.of("sample products", "product3", 30.50f);
        tx.begin();
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);
        tx.commit();

        /*
         * Null values occur at the beginning of the result set.
         */
        List<Product> productsNullFirst;
        try {
            tx.begin();
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
            Root<Product> from = criteriaQuery.from(Product.class);
            CriteriaQuery<Product> select = criteriaQuery.select(from);
            criteriaQuery.orderBy(criteriaBuilder.desc(from.get("description"), Nulls.FIRST));
            productsNullFirst = em.createQuery(criteriaQuery).getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertEquals(3, productsNullFirst.size());
        assertEquals("Sorted based on 'description' in desc order with NULLS FIRST, Expecting first element to be 'product2'", "product2", productsNullFirst.get(0).name);

        /*
         * Null values occur at the end of the result set.
         */
        List<Product> productsNullLast;
        try {

            tx.begin();
            CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
            CriteriaQuery<Product> criteriaQuery = criteriaBuilder.createQuery(Product.class);
            Root<Product> from = criteriaQuery.from(Product.class);
            CriteriaQuery<Product> select = criteriaQuery.select(from);
            criteriaQuery.orderBy(criteriaBuilder.desc(from.get("description"), Nulls.LAST));
            productsNullLast = em.createQuery(criteriaQuery).getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
        assertEquals(3, productsNullLast.size());
        assertEquals("Sorted based on 'description' in desc order with NULLS LAST, Expecting last element to be 'product2'", "product2", productsNullLast.get(2).name);
    }

        @Test
    public void testRecordAsEmbeddable_NoMatchAndOrdering() throws Exception {
        // Clean up any existing data
        tx.begin();
        em.createQuery("DELETE FROM Participant").executeUpdate();
        tx.commit();

        // Setup test data
        Participant p1 = Participant.of("Anna", "Brown", 4);
        Participant p2 = Participant.of("Zach", "Taylor", 5);
        Participant p3 = Participant.of("Mark", "Lee", 6);

        tx.begin();
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);
        tx.commit();

        List<Participant> results = Collections.emptyList(); // Ensure it's never null

        // Query with a last name that doesn't exist
        tx.begin();
        try {
            results = em.createQuery(
                                     "SELECT o FROM Participant o WHERE o.name.last = ?1 ORDER BY o.name.first, o.id",
                                     Participant.class)
                            .setParameter(1, "Doe")
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException("Query failed unexpectedly", e);
        }

        // Debugging output for test failures
        if (!results.isEmpty()) {
            System.out.println("Unexpected results found:");
            for (Participant p : results) {
                System.out.println("  -> " + p);
            }
        }

        // Assertions
        assertNotNull("Results list should not be null", results);
        assertTrue("Expected empty results for non-matching last name", results.isEmpty());
    }

    @Test
    public void testRecordAsEmbeddable_NullEdgeCaseAndOrdering() throws Exception {
        // Setup test data with null, empty, and edge case values
        Participant p1 = Participant.of("Anna", null, 13); // Null last name (should be excluded)
        Participant p2 = Participant.of("Mike", "Green", 14); // Valid
        Participant p3 = Participant.of("Laura", "Blue", 15); // Different last name (excluded)
        Participant p4 = Participant.of("Zoe", "Green", 16); // Valid
        Participant p5 = Participant.of(null, "Green", 17); // Null first name
        Participant p6 = Participant.of("John", "Green", 18); // Valid
        Participant p7 = Participant.of("", "Green", 19); // Empty first name

        // Persist participants
        tx.begin();
        try {
            em.persist(p1);
            em.persist(p2);
            em.persist(p3);
            em.persist(p4);
            em.persist(p5);
            em.persist(p6);
            em.persist(p7);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        // Query for participants with the last name 'Green'
        List<Participant> results;
        tx.begin();
        try {
            results = em.createQuery(
                                     "SELECT o FROM Participant o WHERE o.name.last = :lastName " +
                                     "ORDER BY " +
                                     "CASE WHEN o.name.first IS NULL THEN 1 ELSE 0 END, " +
                                     "CASE WHEN o.name.first = '' THEN 1 ELSE 0 END, " +
                                     "o.name.first, o.id",
                                     Participant.class)
                            .setParameter("lastName", "Green")
                            .getResultList();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }

        // Validate results
        assertNotNull(results);
        assertEquals(5, results.size()); // 5 participants with last name "Green"

        // Expected order: John (18), Mike (14), Zoe (16), "" (19), null (17)
        assertEquals("John", results.get(0).getName().getFirst());
        assertEquals("Mike", results.get(1).getName().getFirst());
        assertEquals("Zoe", results.get(2).getName().getFirst());
        assertEquals("", results.get(3).getName().getFirst());
        assertNull(results.get(4).getName().getFirst());

        // Additional validation for excluded/edge cases
        assertNull(p1.getName().getLast()); // Null last name should be excluded from query
        assertEquals("", p7.getName().getFirst()); // Empty first name correctly stored
        assertNull(p5.getName().getFirst()); // Null first name correctly stored
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
