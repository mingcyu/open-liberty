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
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.stateless.retry;

import javax.ejb.Stateful;

import org.eclipse.microprofile.faulttolerance.Retry;

@Stateful
public class RetryOnEJB {

    private boolean passed = false;
    private int counter = 0;

    public boolean isPassed() {
        return passed;
    }

    //A simple test that makes sure the counter works.
    @Retry(maxRetries = 10)
    public void testMethod() throws TestException {
        if (counter < 5) {
            counter++;
            throw new TestException();
        }
        passed = true;
    }

}
