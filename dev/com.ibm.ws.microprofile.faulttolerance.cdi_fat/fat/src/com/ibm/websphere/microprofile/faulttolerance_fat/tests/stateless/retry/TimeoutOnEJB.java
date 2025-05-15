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

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

@Stateful
public class TimeoutOnEJB {

    private static final int TIMEOUT_DURATION = 1000;

    private boolean passed = false;

    public boolean isPassed() {
        return passed;
    }

    //A simple test that makes sure the counter works.
    @Timeout(TIMEOUT_DURATION)
    public void testMethod() throws TimeoutException {
        try {
            Thread.sleep(TIMEOUT_DURATION * 2);
        } catch (InterruptedException e) {
            passed = true;
            return;
        }
        passed = false;
    }

}
