/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.faces40.internal.fips;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import com.ibm.ws.common.crypto.CryptoUtils;

public class FIPSInitializer implements ServletContainerInitializer {

    private static String MAC_ALGORITHM = "org.apache.myfaces.MAC_ALGORITHM";
    private static String SESSION_ALGORITHM = "org.apache.myfaces.ALGORITHM";
    private static String VIEWSTATE_ID_ALGORITHM = "org.apache.myfaces.RANDOM_KEY_IN_VIEW_STATE_SESSION_TOKEN_SECURE_RANDOM_ALGORITM";
    private static String CSRF_SESSION_ALGORITHM = "org.apache.myfaces.RANDOM_KEY_IN_CSRF_SESSION_TOKEN_SECURE_RANDOM_ALGORITM";

    @Override
    public void onStartup(Set<Class<?>> clazzes, ServletContext servletContext) throws ServletException {
        System.out.println("FIPSInitializer#entered!");

        if (CryptoUtils.isFIPSEnabled()) {
            // **** Already set as default ***** 
            // if (servletContext.getInitParameter(MAC_ALGORITHM) == null) {
            //     servletContext.setInitParameter(MAC_ALGORITHM, "HmacSHA256");
            //     System.out.println("SET MAC");
            // }
            // if (servletContext.getInitParameter(SESSION_ALGORITHM) == null) {
            //     servletContext.setInitParameter(SESSION_ALGORITHM, "AES");
            //     System.out.println("SET VIEWSTATE");
            // }
            if (servletContext.getInitParameter(VIEWSTATE_ID_ALGORITHM) == null) {
                servletContext.setInitParameter(VIEWSTATE_ID_ALGORITHM, "SHA256DRBG");
                System.out.println("SET SESSION");
            }
	        if (servletContext.getInitParameter(CSRF_SESSION_ALGORITHM) == null) {
                servletContext.setInitParameter(CSRF_SESSION_ALGORITHM, "SHA256DRBG");
                System.out.println("SET CSRF");
            }
	} else {
            System.out.println("FIPS NOT ENABLED!");
        }
         System.out.println("FIPSInitializer#exitted!");
    }
}
