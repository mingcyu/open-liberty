/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.interceptedretry;

import javax.ejb.Stateless;

import org.eclipse.microprofile.faulttolerance.Retry;

import com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.TestException;

@Stateless
public class InterceptedRetryOnEJB {

    public static final int MAX = 5;
    public static final int MAX_PLUS_FENCEPOST = 6; //The intercepter will be called for
    private static int counter = 0;

    //Test that the LoggingInterceptor is called every retry
    @Retry(maxRetries = 10)
    @LogInterceptorBinding
    public int retryEventuallyPass() throws TestException {

        System.out.println("GREP");
        if (counter < MAX) {
            counter++;
            throw new TestException();
        }
        return counter;
    }

}
