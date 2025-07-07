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
package com.ibm.ws.springboot.support.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class JmsSpringBootAppTests30 extends JmsAbstractTests {
    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("servlet-6.0", "messaging-3.1", "jndi-1.0", "componenttest-2.0", "springBoot-3.0", "jdbc-4.2"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @ExpectedFFDC("jakarta.servlet.ServletException")
    @Test
    public void testJmsSpringBootApplicationWithTransaction() throws Exception {
        testJmsWithTransaction();
    }
}
