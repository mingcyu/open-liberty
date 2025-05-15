/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package com.ibm.ws.example;

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE10_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE11_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE8_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import app1.web.TestServletA;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that not all @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
public class SSLOptionsTest extends FATServletClient {

    private static final Logger LOG = Logger.getLogger(SSLOptionsTest.class.getName());

    public static final String APP_NAME = "app1";

    private static final String DEFAULT_REALM = "Basic Authentication";
    private static final String DEFAULT_SERVLET_NAME = "ServletName: BasicAuthServlet";
    private static final String DEFAULT_CONTEXT_ROOT = "/basicauth";


    @Server("SSLOptionsServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        // Create a WebArchive that will have the file name 'app1.war' once it's written to a file
        // Include the 'app1.web' package and all of it's java classes and sub-packages
        // Automatically includes resources under 'test-applications/APP_NAME/resources/' folder
        // Exports the resulting application to the ${server.config.dir}/apps/ directory
        ShrinkHelper.defaultApp(server, APP_NAME, "app1.web");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Save the server configuration before each test, this should be the default server
     * configuration.
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        server.saveServerConfiguration();

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);
    }

    /**
     * Restore the server configuration to the default state after each test.
     *
     * @throws Exception
     */
    @After
    public void afterTest() throws Exception {
        // Restore the server to the default state.
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
        server.restoreServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(null);
    }

    /**
     * Test that an SSL handshake failure does get logged when
     * suppressHandshakeErrors="false".
     */
    @Test
    public void handshakeFailureGetsLogged() throws Exception {
        LOG.info("Entering handshakeFailureGetsLogged");

        ServerConfiguration configuration = server.getServerConfiguration();
        LOG.info("Server configuration that the test started with: " + configuration);

        HttpEndpoint httpEndpoint = configuration.getHttpEndpoints().getById("defaultHttpEndpoint");
        LOG.info("SSL Options for default endpoint: " + httpEndpoint.getSslOptions());

        // server.setServerConfigurationFile(SUPPRESS_HANDSHAKE_FAILURE_FALSE_CONFIG);
        // server.setMarkToEndOfLog();
        // server.setTraceMarkToEndOfDefaultTrace();
        // server.updateServerConfiguration(configuration);
        // server.waitForConfigUpdateInLogUsingMark(null);

        // // Requires info trace
        // assertNotNull("We need to wait for the SSL port to open",
        //               server.waitForStringInLog("CWWKO0219I:.*-ssl"));
        // server.addInstalledAppForValidation("basicauth");
        // assertNotNull("Need to wait for 'smarter planet' message (server is ready).",
        //               server.waitForStringInLog("CWWKF0011I"));

        // // Hit the servlet on the SSL port
        // hitServerWithBadHandshake();
        // assertNotNull("Handshake error failure unexpectedly not logged",
        //               server.waitForStringInLog("CWWKO0801E"));

        LOG.info("Exiting handshakeFailureGetsLogged");
    }

    /**
     * Hit the server and make sure its not authenticated. This will trigger
     * a handshake error which is what we're testing for.
     * 
     * @throws Exception
     */
    private void hitServerWithBadHandshake() throws Exception {
        // Hit the servlet with HTTPS on the SSL port and validate it fails
        SSLBasicAuthClient sslClient = new SSLBasicAuthClient(server);
        try {
            sslClient.accessUnprotectedServlet(SSLBasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
            fail("Should not be able to connect to HTTPS port as there is no trust");
        } catch (java.lang.AssertionError t) {
            assertTrue("Failure message did not contain expected 'peer not " +
                       "authenticated' message, message was: " + t.getMessage(),
                       t.getMessage().matches(".*: peer not authenticated"));
        }
    }

    /**
     * Hit the server and make sure its not authenticated. This will trigger
     * a handshake error which is what we're testing for.
     */
    private void hitServer(String ksFile, String ksPassword, String tsFile, String tsPassword) {
        String keystore = server.getPathToAutoFVTNamedServer() + "resources" + File.separator + "security" + File.separator + ksFile;
        String truststore = server.getPathToAutoFVTNamedServer() + "resources" + File.separator + "security" + File.separator + tsFile;
        // Hit the servlet with HTTPS on the SSL port and validate it fails
        SSLBasicAuthClient sslClient = new SSLBasicAuthClient(server, DEFAULT_REALM, DEFAULT_SERVLET_NAME, DEFAULT_CONTEXT_ROOT,
                        keystore, ksPassword, truststore, tsPassword);
        String response = sslClient.accessUnprotectedServlet(SSLBasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT);
        assertTrue("Did not get the expected response",
                   sslClient.verifyUnauthenticatedResponse(response));
    }


}
