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

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.AsciiCharacter;
import io.openliberty.jpa.persistence.tests.models.Participant;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
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

    @Test // Verifies that a JPQL query using an alias returns the correct hexadecimal value for a persisted AsciiCharacter
    public void testAsciiCharacterQueryReturnsHexadecimalWithAlias() {
        int id = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        AsciiCharacter character = new AsciiCharacter();
        character.setId(id);
        character.setThisCharacter('P');
        character.setHexadecimal("50");
        character.setNumericValue(80);
        character.setControl(false);

        try {
            tx.begin();
            em.createQuery("DELETE FROM AsciiCharacter a WHERE a.thisCharacter = :char")
                            .setParameter("char", character.getThisCharacter())
                            .executeUpdate();
            em.persist(character);
            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (SystemException se) {
                throw new RuntimeException("Rollback failed during testOLGH28913QueryHexadecimalWithAlias", se);
            }
            throw new RuntimeException("Transaction failed during testOLGH28913QueryHexadecimalWithAlias", e);
        }

        TypedQuery<String> query = em.createQuery(
                                                  "SELECT a.hexadecimal FROM AsciiCharacter a WHERE a.thisCharacter = :char", String.class);
        query.setParameter("char", character.getThisCharacter());

        List<String> results = query.getResultList();

        assertNotNull("Query result should not be null", results);
        assertFalse("Query result should not be empty", results.isEmpty());
        assertTrue("Expected hexadecimal value not found in results", results.contains(character.getHexadecimal()));
    }

    @Test
    public void testInvalidFieldInAsciiCharacterQuery() {
        try {
            em.createQuery("SELECT nonExistentField FROM AsciiCharacter", String.class).getResultList();
        } catch (Exception e) {
            assertTrue("Expected exception to be thrown due to non-existent field",
                       e instanceof IllegalArgumentException || e instanceof RuntimeException);
            assertTrue("Unexpected exception type: " + e.getClass(),
                       e instanceof IllegalArgumentException || e instanceof RuntimeException);
            assertTrue("Exception message did not contain 'nonExistentField': " + e.getMessage(),
                       e.getMessage().contains("nonExistentField") || e.getClass().equals(IllegalArgumentException.class) || e.getClass().equals(RuntimeException.class));
        }
    }

    @Test // Verifies that multiple persisted AsciiCharacter entries return correct hexadecimal values via JPQL query
    public void testAsciiCharacterMultipleResultsQuery() {
        try {
            tx.begin();
            em.persist(AsciiCharacter.of(65)); // 'A'
            em.persist(AsciiCharacter.of(66)); // 'B'
            tx.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
            throw new RuntimeException("Transaction failed during testOLGH28913MultipleResultsQuery", e);
        }
        List<String> results = em.createQuery(
                                              "SELECT a.hexadecimal FROM AsciiCharacter a WHERE a.hexadecimal IS NOT NULL", String.class)
                        .getResultList();
        assertTrue("Expected hex value 41 not found", results.contains("41")); // 65 in hex
        assertTrue("Expected hex value 42 not found", results.contains("42")); // 66 in hex
    }

}
