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

import javax.ejb.Stateless;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@Stateless
public class FaultTolerenceInterceptorOnEJB {

    private boolean passed = false;

    @Retry(maxRetries = 1)
    @Fallback(fallbackMethod = "fallbackMethod")
    public void unstableMethod() throws TestException {
        throw new TestException();
    }

    public void fallbackMethod() {
        passed = true;
    }

    public boolean isPassed() {
        return passed;
    }

}
