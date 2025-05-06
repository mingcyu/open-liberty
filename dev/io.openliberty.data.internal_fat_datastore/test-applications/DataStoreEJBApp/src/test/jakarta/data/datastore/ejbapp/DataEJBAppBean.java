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
package test.jakarta.data.datastore.ejbapp;

import static org.junit.Assert.assertEquals;

import java.util.function.Consumer;

import jakarta.ejb.Singleton;
import jakarta.inject.Inject;

import org.junit.Test;

import test.jakarta.data.datastore.lib.ServerDSEntity;

@Singleton
public class DataEJBAppBean implements Consumer<String> {
    @Inject
    EJBAppDSRefRepo ejbAppRepo;

    @Override
    public void accept(String testName) {
        System.out.println("DataEJBAppBean is running " + testName);

        assertEquals(false, ejbAppRepo.lookFor(testName).isPresent());

        EJBAppEntity e = EJBAppEntity.of(testName);
        e = ejbAppRepo.persist(e);
        assertEquals(testName, e.testName);
        assertEquals(testName.length(), e.nameLength);

        e = ejbAppRepo.lookFor(testName).orElseThrow();
        assertEquals(testName, e.testName);
        assertEquals(testName.length(), e.nameLength);
    }
}
