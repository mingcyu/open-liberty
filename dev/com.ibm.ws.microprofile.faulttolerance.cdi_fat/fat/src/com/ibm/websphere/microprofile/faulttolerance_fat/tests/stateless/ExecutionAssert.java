/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ejb.EJBException;

public class ExecutionAssert {

    private static final int COMPLETION_TIMEOUT = 1000;

    public static interface ThrowingRunnable {
        public void run() throws Exception;
    }

    public static <E extends Exception> E assertThrows(Class<E> expected, ThrowingRunnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("Exception not thrown");
        } catch (Exception e) {
            assertThat("Thrown exception is not of the correct type", e, instanceOf(expected));
            return expected.cast(e);
        }
    }

    public static <E extends Exception> E assertThrowsEjbWrapped(Class<E> expected, ThrowingRunnable runnable) {
        EJBException e = assertThrows(EJBException.class, runnable);
        assertThat("Thrown exception is not of the correct type", e.getCause(), instanceOf(expected));
        return expected.cast(e.getCause());
    }

    public static <T> void assertReturns(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new AssertionError("Unexpected exception thrown: " + e, e);
        }
    }

    public static void assertThrowsEjbWrapped(Class<? extends Exception> expected, Future<?> future) {
        EJBException e = assertThrows(EJBException.class, future);
        assertThat("Wrapped exception has wrong type", e.getCause(), instanceOf(expected));
    }

    public static <T extends Exception> T assertThrows(Class<T> expected, Future<?> future) {
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

    public static <T> T assertCompletes(Future<T> future) {
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
