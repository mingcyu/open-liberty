/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsp23.fat.tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.rules.repeater.JakartaEEAction;
/**
 * Testing the ignoreWriteAfterCommit config created for TS018163099.  This is a behavior difference between Liberty and tWAS.
 * On tWAS, a connections remained active during closure when an error occurred. On Liberty, this behavior was changed.
 * There isn't enough documentation or version history to explain the difference.
 * 
 * This test attempts to verify the config by seeing if a connection reset error occurs (meaning the socket was closed).
 * The index.jsp is requested, but redirects you to another JSP. However, the index.jsp still writes out more data via the footer.jsp. 
 * This causes a MessageSentException.  A single socket is used to minic the reused connection.
 * 
 * Additional Note - This can be replicated via a curl command via --retry. 
 *  
 */
// No need to run against cdi-2.0 since these tests don't use CDI at all.
@Mode(TestMode.FULL)
@SkipForRepeat({"CDI-2.0"}) 
@RunWith(FATRunner.class)
public class JSPChannelTest {

    private static final String APP_NAME = "WriteAfterRedirect";

    private static final Logger LOG = Logger.getLogger(JSPChannelTest.class.getName());

    @Server("ignoreWriteAfterCommitServer")
    public static LibertyServer server;

    //Deploy the app at the very start of the test
    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war");
    }

    /**
     *  ignoreWriteAfterCommit True Scenario.  
     *  We expect the socket connection to stay active, 
     *  so we'll see the "Successfully redirected!" message in responses.
     *
     * @throws Exception if something goes horribly wrong
     */
    @ExpectedFFDC("com.ibm.wsspi.genericbnf.exception.MessageSentException")
    @Test
    public void testIgnoreWriteAfterCommitTrue() throws Exception {
        try {
            updateHTTPOptions(true); 

            server.startServer(JSPChannelTest.class.getSimpleName() + ".log");

            String address = server.getHostname() + ":" + server.getHttpDefaultPort();

            // sendRedirect changed in EE11, so we'll request a slightly different page
            String page = JakartaEEAction.isEE11OrLaterActive() ? "indexEE11.jsp" : "index.jsp";

            String request = "GET /" + APP_NAME + "/" + page + " HTTP/1.1\r\n" +
                             "Host: " + address + "\r\n" +
                             "Keep-Alive: timeout=5, max=200\r\n" +
                             "\r\n";

            String redirect_request = "GET /" + APP_NAME + "/page2.jsp " + "HTTP/1.1\r\n" +
                                      "Host: " + address + "\r\n" +
                                      "Connection: close\r\n" +
                                      "\r\n";

            Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort());
            socket.setKeepAlive(true);

            BufferedReader bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream os = socket.getOutputStream();

            os.write(request.getBytes());
            Thread.sleep(500); // time in-between requests
            os.write(redirect_request.getBytes());

            // Wait a few seconds to let the connection close if an error occurs
            Thread.sleep(3000);

            // Read the responses:
            boolean containsMessage = false;
            String line;
            while ((line = bReader.readLine()) != null) {
                LOG.info(line);
                if (line.contains("Successfully redirected!")) {
                    containsMessage = true;
                }
            }
            Assert.assertTrue("The redirect failed!", containsMessage);
        } finally {
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        }

    }

    /**
     *  ignoreWriteAfterCommit False Scenario.  
     *  We expect the socket connection to be destroyed
     *  so we'll see a "Connection reset" exception.
     *
     * @throws Exception if something goes horribly wrong
     */
    @ExpectedFFDC("com.ibm.wsspi.genericbnf.exception.MessageSentException")
    @Test
    public void testIgnoreWriteAfterCommitFalse() throws Exception {
        try {
            updateHTTPOptions(false);
            server.startServer(JSPChannelTest.class.getSimpleName() + ".log");

            String address = server.getHostname() + ":" + server.getHttpDefaultPort();
            
            // sendRedirect changed in EE11
            String page = JakartaEEAction.isEE11OrLaterActive() ? "indexEE11.jsp" : "index.jsp";

            String request = "GET /" + APP_NAME + "/" + page + " HTTP/1.1\r\n" +
                             "Host: " + address + "\r\n" +
                             "Keep-Alive: timeout=5, max=200\r\n" +
                             "\r\n";

            String redirect_request = "GET /" + APP_NAME + "/page2.jsp " + "HTTP/1.1\r\n" +
                                      "Host: " + address + "\r\n" +
                                      "Connection: close\r\n" +
                                      "\r\n";

            Socket socket = new Socket(server.getHostname(), server.getHttpDefaultPort());
            socket.setKeepAlive(true); 

            BufferedReader bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream os = socket.getOutputStream();

            os.write(request.getBytes());
            Thread.sleep(500); // time in-between requests
            os.write(redirect_request.getBytes());

            // Wait a few seconds to let the connection close if an error occurs
            Thread.sleep(3000);

            // Read the responses:
            String line;
            boolean containsMessage = false;
            while ((line = bReader.readLine()) != null) {
                LOG.info(line);
                if (line.contains("Successfully redirected!")) {
                    containsMessage = true;
                }
            }
            // If Successfully redirected! is not read, then connection was closed.
            Assert.assertFalse("The redirect failed!", containsMessage);
        } catch(SocketException e) { 
            // If the connection was reset that means the server closed it. 
            Assert.assertEquals("Expected `Connection reset` message not found!", "Connection reset", e.getMessage());
        } finally {
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        }
    }

    private void updateHTTPOptions(Boolean persistValue) throws Exception {
        ServerConfiguration c = server.getServerConfiguration();
        ConfigElementList<HttpEndpoint> h = c.getHttpEndpoints();
        for (HttpEndpoint httpEndpoint : h) {
            LOG.info("Using httpEndpoint: " + h);
            httpEndpoint.getHttpOptions().setIgnoreWriteAfterCommit(persistValue);
        }
        server.updateServerConfiguration(c);
    }

}
