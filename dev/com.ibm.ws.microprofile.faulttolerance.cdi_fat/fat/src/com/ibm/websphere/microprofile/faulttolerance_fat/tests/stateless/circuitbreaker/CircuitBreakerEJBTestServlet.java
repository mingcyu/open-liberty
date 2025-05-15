/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.circuitbreaker;

import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.circuitbreaker.CircuitBreakerEJB.Response.RETURN;
import static com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.circuitbreaker.CircuitBreakerEJB.Response.THROW;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/circuitbreakerejb")
public class CircuitBreakerEJBTestServlet extends FATServlet {

    @Inject
    private CircuitBreakerEJB bean;

    @Test
    @ExpectedFFDC("org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException")
    public void testEjbCircuitBreaker() {
        assertReturns(() -> bean.test(RETURN));
        assertReturns(() -> bean.test(RETURN));
        assertReturns(() -> bean.test(RETURN));
        assertReturns(() -> bean.test(RETURN));
        assertThrows(TestException.class, () -> bean.test(THROW));
        assertThrows(TestException.class, () -> bean.test(THROW));
        // 50% failure rate in last four requests -> circuit opens
        assertThrowsEjbWrapped(CircuitBreakerOpenException.class, () -> bean.test(RETURN));
        assertThrowsEjbWrapped(CircuitBreakerOpenException.class, () -> bean.test(THROW));
        // Wait for circuit to half-open
        sleep(1000);
        // Circuit half-open
        assertReturns(() -> bean.test(RETURN));
        // Circuit closed
        assertReturns(() -> bean.test(RETURN));
        assertThrows(TestException.class, () -> bean.test(THROW));
    }

    private <E extends Exception> E assertThrows(Class<E> expected, ThrowingRunnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("Exception not thrown");
        } catch (Exception e) {
            assertThat("Thrown exception is not of the correct type", e, instanceOf(expected));
            return expected.cast(e);
        }
    }

    private <E extends Exception> E assertThrowsEjbWrapped(Class<E> expected, ThrowingRunnable runnable) {
        EJBException e = assertThrows(EJBException.class, runnable);
        assertThat("Thrown exception is not of the correct type", e.getCause(), instanceOf(expected));
        return expected.cast(e.getCause());
    }

    private <T> void assertReturns(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception thrown: " + e, e);
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
    }

    public static interface ThrowingRunnable {
        public void run() throws Exception;
    }

}
