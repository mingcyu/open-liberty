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
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.transaction.UserTransaction;

public class ReEnableUserTranTestBean2 {

    @Resource
    private UserTransaction ut;

    @Transactional(TxType.NOT_SUPPORTED)
    public void checkNotSupportedReEnablesUserTran() throws Exception {

        ut.getStatus();
    }

    @Transactional(TxType.NEVER)
    public void checkNeverReEnablesUserTran() throws Exception {

        ut.getStatus();
    }
}