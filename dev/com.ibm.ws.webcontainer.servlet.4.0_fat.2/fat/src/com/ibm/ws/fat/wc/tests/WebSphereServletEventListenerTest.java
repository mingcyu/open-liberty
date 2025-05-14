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

import static org.junit.Assert.fail;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests the legacy WebSphere servlet event API
 *
 * https://openliberty.io/docs/latest/reference/javadoc/api/servlet-4.0.com.ibm.websphere.servlet.event.html?path=25.0.0.4/com.ibm.websphere.appserver.api.servlet_1.1-javadoc/com/ibm/websphere/servlet/event/package-summary.html
 *
 * These APIs were from WAS 4.0+ (?) time frame. Application should use the servlet standard APIs instead!
 *
 * Since these APIs are still around, this test is added to cover the test gap in this area.
 *
 *
 * Response text should look similar like this:
 *
 * [(MAIN) WebSpshereApplicationListener.onApplicationStart
 * >> 1. WebSphereFilterListener.onFilterStartInit for filter [websphere.filter.EvenFilter]
 * << 1. WebSphereFilterListener.onFilterFinishInit for filter [websphere.filter.EvenFilter]
 * >>> 1.1 WebSphereFilterInvocationListener.onFilterStartDoFilter for ServletEvent.getServletName [websphere.filter.EvenFilter]
 * >> 2. WebSphereServletListener.onServletStartInit for ServletEvent.getServletName [websphere.servlet.EventServlet]
 * << 2. WebSphereServletListener.onServletFinishInit for ServletEvent.getServletName [websphere.servlet.EventServlet]
 * >> 2. WebSphereServletListener.onServletAvailableForService for ServletEvent.getServletName [websphere.servlet.EventServlet]
 * >>> 2.2 WebSphereServletInvocationListener.onServletStartService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletEvent]. OutputStream obtained.
 * >>>(service)>>> Context attribute from WebSphere API [WEBSPHERE Servlet API using ApplicationListener.]
 * >>>(service)>>> Context attribute from Standard API [STANDARD Servlet API using ServletContextListener]
 * <<< 2.2 WebSphereServletInvocationListener.onServletFinishService, for request URL [http://localhost:8010/WebSphereServletEvent/ServletEvent]
 * <<< 1.1 WebSphereFilterInvocationListener.onFilterFinishDoFilter for filter [websphere.filter.EvenFilter]
 *
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WebSphereServletEventListenerTest {

    private static final Logger LOG = Logger.getLogger(WebSphereServletEventListenerTest.class.getName());
    private static final String APP_NAME = "WebSphereServletEvent";

    //Verify the response contains all of these keywords (WEBSPHERE Servlet API and STANDARD Servlet API are context attributes,
    //                                                  "websphere.filter.EvenFilter",          is the filter name
    //                                                  "websphere.servlet.EventServlet"        is the servlet name )
    private final String[] expectedKeys = { "WebSpshereApplicationListener.onApplicationStart",
                                            "WebSphereFilterListener.onFilterStartInit",
                                            "WebSphereFilterListener.onFilterFinishInit",
                                            "WebSphereFilterInvocationListener.onFilterStartDoFilter",
                                            "WebSphereServletListener.onServletStartInit",
                                            "WebSphereServletListener.onServletFinishInit",
                                            "WebSphereServletListener.onServletAvailableForService",
                                            "WebSphereServletInvocationListener.onServletStartService",
                                            "WEBSPHERE Servlet API",
                                            "STANDARD Servlet API",
                                            "WebSphereServletInvocationListener.onServletFinishService",
                                            "WebSphereFilterInvocationListener.onFilterFinishDoFilter",
                                            "websphere.filter.EvenFilter",
                                            "websphere.servlet.EventServlet"
    };

    @Server("servlet40_webSphereServletEvent")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet40_webSphereServletEvent");
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "websphere.servlet", "websphere.listener", "websphere.filter");
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

                for (String keyword : expectedKeys) {
                    if (responseText.contains(keyword)) {
                        LOG.info("\n Found [" + keyword + "]");
                    } else {
                        fail("Response does not contain [" + keyword + "]");
                    }

                }
            }
        }
    }
}
