/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transactional.web;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet("/reenableusertran")
public class ReEnableUserTranTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    @Resource
    private UserTransaction ut;

    @Inject
    private ReEnableUserTranTestBean bean;

    @Test
    public void testNotSupportedFromNotSupported() throws Exception {
        bean.checkReEnablementNotSupportedFromNotSupported();
    }

    @Test
    public void testNotSupportedFromNever() throws Exception {
        bean.checkReEnablementNotSupportedFromNever();
    }

    @Test
    public void testNotSupportedFromRequired() throws Exception {
        bean.checkReEnablementNotSupportedFromRequired();
    }

    @Test
    public void testNotSupportedFromRequiresNew() throws Exception {
        bean.checkReEnablementNotSupportedFromRequiresNew();
    }

    @Test
    public void testNotSupportedFromSupports() throws Exception {
        bean.checkReEnablementNotSupportedFromSupports();
    }

    @Test
    public void testNotSupportedFromMandatory() throws Exception {

        ut.begin();
        bean.checkReEnablementNotSupportedFromMandatory();
    }

    @Test
    public void testNeverFromNever() throws Exception {
        bean.checkReEnablementNeverFromNever();
    }

    @Test
    public void testNeverFromNotSupported() throws Exception {
        bean.checkReEnablementNeverFromNotSupported();
    }

    @Test
    public void testNeverFromSupports() throws Exception {
        bean.checkReEnablementNeverFromSupports();
    }
}