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

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.MicroProfileActions;
import junit.framework.Assert;

@WebServlet("/StatelessRetryServlet")
public class FaultToleranceOnEJBServlet extends FATServlet {

    @EJB
    private FaultTolerenceInterceptorOnEJB faultTolerenceInterceptorOnEJB;

    @Test
    //The fault tolerance CDI Extension does not fire events for methods on an EJB on these versions
    @SkipForRepeat({ MicroProfileActions.MP13_ID, MicroProfileActions.MP20_ID })
    public void testRetryOnStatelessBean(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        faultTolerenceInterceptorOnEJB.unstableMethod();
        Assert.assertTrue(faultTolerenceInterceptorOnEJB.isPassed());
    }

}
