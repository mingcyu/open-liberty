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
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

public class ReEnableUserTranTestBean {

    @Resource
    private UserTransaction ut;

    @Inject
    private ReEnableUserTranTestBean2 bean;

    @Transactional(TxType.NOT_SUPPORTED)
    public void checkReEnablementNotSupportedFromNotSupported() throws Exception {
        bean.checkNotSupportedReEnablesUserTran();
    }

    @Transactional(TxType.NEVER)
    public void checkReEnablementNotSupportedFromNever() throws Exception {
        bean.checkNotSupportedReEnablesUserTran();
    }

    @Transactional(TxType.REQUIRED)
    public void checkReEnablementNotSupportedFromRequired() throws Exception {
        bean.checkNotSupportedReEnablesUserTran();
    }

    @Transactional(TxType.REQUIRES_NEW)
    public void checkReEnablementNotSupportedFromRequiresNew() throws Exception {
        bean.checkNotSupportedReEnablesUserTran();
    }

    @Transactional(TxType.SUPPORTS)
    public void checkReEnablementNotSupportedFromSupports() throws Exception {
        bean.checkNotSupportedReEnablesUserTran();
    }

    @Transactional(TxType.MANDATORY)
    public void checkReEnablementNotSupportedFromMandatory() throws Exception {
        bean.checkNotSupportedReEnablesUserTran();
    }

    @Transactional(TxType.NEVER)
    public void checkReEnablementNeverFromNever() throws Exception {
        bean.checkNeverReEnablesUserTran();
    }

    @Transactional(TxType.NOT_SUPPORTED)
    public void checkReEnablementNeverFromNotSupported() throws Exception {
        bean.checkNeverReEnablesUserTran();
    }

    @Transactional(TxType.SUPPORTS)
    public void checkReEnablementNeverFromSupports() throws Exception {
        bean.checkNeverReEnablesUserTran();
    }
}