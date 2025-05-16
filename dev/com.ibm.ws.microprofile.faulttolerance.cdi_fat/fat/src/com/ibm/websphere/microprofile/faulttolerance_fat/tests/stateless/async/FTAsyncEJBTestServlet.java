/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.async;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.EJBException;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.TestException;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.bulkhead.BarrierFactory;
import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.bulkhead.BarrierFactory.Barrier;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ftasyncejb")
public class FTAsyncEJBTestServlet extends FATServlet {

    private static final int COMPLETION_TIMEOUT = 1000;

    @Inject
    private FTAsyncEJB bean;

    @Test
    public void testAsync() {
        try (BarrierFactory bf = new BarrierFactory()) {
            AtomicInteger counter = new AtomicInteger(0);

            Barrier b1 = bf.create();
            Future<?> f1 = bean.test(counter, b1);
            b1.assertAwaited();

            assertEquals(1, counter.get());

            Barrier b2 = bf.create();
            Future<?> f2 = bean.test(counter, b2);
            b2.assertAwaited();

            assertEquals(2, counter.get());

            b1.complete();
            assertCompletes(f1);

            b2.complete();
            assertCompletes(f2);
        }
    }

    @Test
    public void testAsyncRetry() throws TestException {
        AtomicInteger failureCoundown1 = new AtomicInteger(3);
        assertCompletes(bean.testRetry(failureCoundown1));
        assertEquals(-1, failureCoundown1.get());

        AtomicInteger failureCoundown2 = new AtomicInteger(5);
        assertThrows(TestException.class, bean.testRetry(failureCoundown2));
        assertEquals(1, failureCoundown2.get());
    }

    private void assertThrowsEjbWrapped(Class<? extends Exception> expected, Future<?> future) {
        EJBException e = assertThrows(EJBException.class, future);
        assertThat("Wrapped exception has wrong type", e.getCause(), instanceOf(expected));
    }

    private <T extends Exception> T assertThrows(Class<T> expected, Future<?> future) {
        try {
            Object result = future.get(COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS);
            throw new AssertionError("Expected exception not thrown. Result: " + result);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            assertThat("Thrown exception has wrong type", t, instanceOf(expected));
            return expected.cast(t);
        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted waiting for future", e);
        } catch (TimeoutException e) {
            throw new AssertionError("Timeout waiting for future", e);
        }
    }

    private <T> T assertCompletes(Future<T> future) {
        try {
            return future.get(COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new AssertionError("Future completed with exception: " + e, e);
        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted waiting for future", e);
        } catch (TimeoutException e) {
            throw new AssertionError("Timeout waiting for future", e);
        }
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
