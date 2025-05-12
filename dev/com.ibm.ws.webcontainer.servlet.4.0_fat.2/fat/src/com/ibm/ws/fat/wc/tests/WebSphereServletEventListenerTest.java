/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests the legacy WebSphere servlet event API
 *
 * https://openliberty.io/docs/latest/reference/javadoc/api/servlet-4.0.com.ibm.websphere.servlet.event.html?path=25.0.0.4/com.ibm.websphere.appserver.api.servlet_1.1-javadoc/com/ibm/websphere/servlet/event/package-summary.html
 *
 * These APIs were from WAS 4.0+ time frame. Application should use the servlet standard APIs instead.
 *
 * Since these APIs are still around, this test is added to cover the test gap in this area.
 */
@RunWith(FATRunner.class)
public class WebSphereServletEventListenerTest {

    private static final Logger LOG = Logger.getLogger(WebSphereServletEventListenerTest.class.getName());
    private static final String APP_NAME = "WebSphereServletEvent";

    @Server("servlet40_webSphereServletEvent")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet40_webSphereServletEvent");
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "websphere.servlet", "websphere.servlet.listener");
        server.startServer(WebSphereServletEventListenerTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for Tests.");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void test_ServletContext_Attributes() throws Exception {
        LOG.info("====== <test_ServletContext_Attributes> ======");

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + APP_NAME + "/ServletEvent";
        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                /*
                 * expecting response:
                 * Context attribute from WebSphere API [WEBSPHERE Servlet API ApplicationListener.]
                 * Context attribute from Standard API [STANDARD Servlet API ServletContextListener.]
                 */

                assertTrue("Expecting BOTH [WEBSPHERE] and [STANDARD] from response but found << " + responseText + ">> ",
                           responseText.contains("WEBSPHERE Servlet API") && responseText.contains("STANDARD Servlet API"));
            }
        }
    }
}
