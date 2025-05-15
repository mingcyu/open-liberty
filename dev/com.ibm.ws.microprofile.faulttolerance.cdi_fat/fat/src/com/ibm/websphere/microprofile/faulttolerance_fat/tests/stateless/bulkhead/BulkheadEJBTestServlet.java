/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.bulkhead;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.Test;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.bulkhead.BarrierFactory.Barrier;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/bulkheadejb")
public class BulkheadEJBTestServlet extends FATServlet {

    private static final int COMPLETION_TIMEOUT = 1000;

    @Inject
    private BulkheadEJB bean;

    @Resource
    private ManagedExecutorService executor;

    @Test
    @ExpectedFFDC("org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException")
    public void testBulkhead() {
        try (BarrierFactory bf = new BarrierFactory()) {
            Barrier b1 = bf.create();
            Future<?> f1 = executor.submit(() -> bean.test(b1));
            b1.assertAwaited();

            Barrier b2 = bf.create();
            Future<?> f2 = executor.submit(() -> bean.test(b2));
            b2.assertAwaited();

            Barrier b3 = bf.create();
            Future<?> f3 = executor.submit(() -> bean.test(b3));
            b3.assertAwaited();

            // Bulkhead is now full

            Barrier b4 = bf.create();
            Future<?> f4 = executor.submit(() -> bean.test(b4));
            assertThrowsEjbWrapped(BulkheadException.class, f4);

            // Free up one space

            b1.complete();
            assertCompletes(f1);

            Barrier b5 = bf.create();
            Future<?> f5 = executor.submit(() -> bean.test(b5));

            // Allow everything to complete and check it was successful
            b2.complete();
            assertCompletes(f2);
            b3.complete();
            assertCompletes(f3);
            b5.complete();
            assertCompletes(f5);
        }
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
}
