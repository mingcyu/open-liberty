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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.AfterClass;
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
import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.rules.repeater.JakartaEEAction;

/**
 * Testing the ignoreWriteAfterCommit config created for OLGH30757. This is a behavior difference between Liberty and tWAS.
 * On tWAS, a connections remained active during closure when an error occurred. On Liberty, this behavior was changed.
 * There isn't enough documentation or version history to explain the difference.
 * 
 * This test attempts to verify the config by seeing if a connection reset error occurs (meaning the socket was closed).
 * The index.jsp is requested, but redirects you to another JSP. However, the index.jsp still writes out more data via the footer.jsp.
 * This causes a MessageSentException. A single socket is used to mimic the reused connection.
 * 
 * Additional Note - This can be replicated via a curl command via --retry.
 * 
 */
// No need to run against cdi-2.0 since these tests don't use CDI at all.
@Mode(TestMode.FULL)
@SkipForRepeat({ "CDI-2.0" })
@RunWith(FATRunner.class)
public class JSPChannelTest {

    private static final String APP_NAME = "WriteAfterRedirect";

    private static final Logger LOG = Logger.getLogger(JSPChannelTest.class.getName());

    @Server("ignoreWriteAfterCommitServer")
    public static LibertyServer server;

    //Deploy the app at the very start of the test
    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "com.ibm.ws.jsp23.fat.writeafterredirect.servlets");
        server.startServer(JSPChannelTest.class.getSimpleName() + ".log");
        server.waitForStringInLog("CWWKT0016I:.*WriteAfterRedirect.*"); // ensure app has started. 
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (server != null && server.isStarted()) {

            //SRVE0777E - Exception thrown from application class 
            //SRVE8115W: WARNING: Cannot set status. Response already committed.
            //SRVE8094W: WARNING: Cannot set header. Response already committed
            server.stopServer("SRVE8115W", "SRVE8094W", "SRVE0777E"); 
        }
    }

    /**
     * ignoreWriteAfterCommit True Scenario.
     * We expect the socket connection to stay active,
     * so we'll see the "Successfully redirected!" message in responses.
     *
     * @throws Exception if something goes horribly wrong
     */
    
    @Test
    public void testIgnoreWriteAfterCommitTrue() throws Exception {
        Socket socket = null;
        try {
            updateHTTPOptions(true);

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

            socket = new Socket(server.getHostname(), server.getHttpDefaultPort());
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
            if (socket != null) {
                socket.close();
            }
        }

    }

    /**
     * ignoreWriteAfterCommit False Scenario.
     * We expect the socket connection to be destroyed
     * so we'll see a "Connection reset" exception.
     *
     * @throws Exception if something goes horribly wrong
     */
    @ExpectedFFDC("com.ibm.wsspi.genericbnf.exception.MessageSentException")
    @Test
    public void testIgnoreWriteAfterCommitFalse() throws Exception {
        Socket socket = null;
        boolean socketExceptionOccurred = false;
        boolean containsMessage = false;
        boolean tryBlockCompletedWithoutException = false;
        try {
            updateHTTPOptions(false);

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

            socket = new Socket(server.getHostname(), server.getHttpDefaultPort());
            socket.setKeepAlive(true);

            BufferedReader bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream os = socket.getOutputStream();

            os.write(request.getBytes());
            Thread.sleep(500); // time in-between requests
            os.write(redirect_request.getBytes());

            LOG.info("Waiting 3 seconds for server to potentially close connection due to error on first response attempt...");
            // Wait a few seconds to let the connection close if an error occurs
            Thread.sleep(3000);

            // Read the responses:
            LOG.info("Attempting to read responses (expecting SocketException)...");
            String line;

            // Check socket state *before* attempting to read
            if (socket != null) {
                LOG.info("Socket state before reading: closed=" + socket.isClosed() + ", connected=" + socket.isConnected() + ", inputShutdown=" + socket.isInputShutdown() + ", outputShutdown=" + socket.isOutputShutdown());
           }

            while ((line = bReader.readLine()) != null) {
                LOG.info(line);
                if (line.contains("Successfully redirected!")) {
                    containsMessage = true;
                }
            }
            // If Successfully redirected! is not read, then connection was closed.
            //Assert.assertFalse("The redirect was successful when it should not have been!", containsMessage);
            
            // If we get here, readLine() returned null (EOF) without a preceding SocketException.
            // This means the connection closed, but maybe not via the expected exception mechanism.
            // OR worse, the connection stayed open and the second request completed.
            tryBlockCompletedWithoutException = true;
            LOG.severe("Test failed: Try block completed without expected SocketException. Success message found=" + containsMessage);
            LOG.severe("Test failed: Try block completed without expected SocketException. tryBlockCompletedWithoutException=" + tryBlockCompletedWithoutException);

        } catch (SocketException e) {
            // If the connection was reset that means the server closed it. 
            LOG.info("SocketException occurred! -> " + e.getMessage());
            socketExceptionOccurred = true;
            //Assert.assertTrue("Expected SocketException did not occur", socketExceptionOccurred);
        } catch (IOException ioe) {
             // Catch other IOExceptions as they might also indicate the connection closure in some cases
             LOG.info("Caught IOException (may indicate closed connection): " + ioe.getClass().getName() + " - " + ioe.getMessage());
             socketExceptionOccurred = true; // Treat as success if it implies closed pipe

        } catch (Exception e) {
            // Catch any Exception that have happened
            LOG.info("Caught Exception: " + e.getClass().getName() + " - " + e.getMessage());
            //socketExceptionOccurred = true; // Treat as success if it implies closed pipe

       } finally {
            if (socket != null) {
                socket.close();
            }
        }
        Assert.assertTrue(
            "Test Failed: Expected a SocketException (or similar IOException indicating closure) " +
            "because ignoreWriteAfterCommit=false should cause connection closure on MessageSentException/pipeline error." +
            " Instead, try block completed=" + tryBlockCompletedWithoutException + ", success message found=" + containsMessage,
            socketExceptionOccurred); // Check the flag
    }

    /**
     * Tests that ignoreWriteAfterCommit=true does NOT prevent connection closure
     * for plain IOExceptions.
     * 
     * This test verifies that ignoreWriteAfterCommit is specific to
     * MessageSentException
     * and doesn't apply to general IOExceptions.
     */
    @Test
    @AllowedFFDC({ "java.lang.IllegalStateException", "java.io.IOException" })
    public void testIgnoreWriteAfterCommitTrueWithIOException() throws Exception {
        Socket socket = null;
        boolean socketExceptionOccurred = false;

        try {
            updateHTTPOptions(true); // Set ignoreWriteAfterCommit=true

            String address = server.getHostname() + ":" + server.getHttpDefaultPort();

            // Create a persistent connection
            socket = new Socket(server.getHostname(), server.getHttpDefaultPort());
            socket.setSoTimeout(10000); // 10 seconds timeout
            socket.setKeepAlive(true);

            // Send a request for a slow responding JSP
            String abruptRequest = "GET /" + APP_NAME + "/slow-response HTTP/1.1\r\n" +
                    "Host: " + address + "\r\n" +
                    "Connection: keep-alive\r\n" +
                    "\r\n";
            OutputStream os = socket.getOutputStream();
            os.write(abruptRequest.getBytes());
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line;
            boolean foundStartMarker = false;
            while ((line = reader.readLine()) != null) {
                LOG.info("< " + line);
                if (line.contains("Beginning slow response...")) {
                    foundStartMarker = true;
                    break;
                }
            }

            // Check to see if the response has been started
            Assert.assertTrue("Should find start marker in response", foundStartMarker);

            // Now read a small portion of the body to ensure the response is in progress
            char[] buffer = new char[1024];
            int charsRead = reader.read(buffer, 0, buffer.length);
            LOG.info("Read " + charsRead + " chars of response body: " +
                    new String(buffer, 0, charsRead));

            // Wait a bit to ensure the server is in the middle of sending data
            LOG.info("Waiting 2 seconds before triggering broken pipe...");
            Thread.sleep(2000);

            // Now shutdown the stream, but keep the socket open to cause the server's write
            // operations to fail
            LOG.info("Shutting down stream to trigger broken pipe");
            socket.shutdownInput();

            socket.shutdownOutput();

            // Let the server continue to write while the client is not reading
            // This will eventually cause an IOException on the server side during write
            LOG.info("Waiting 3 seconds for server to process broken pipe...");
            Thread.sleep(3000);

            // Now try to send a second request on the same socket
            LOG.info("Sending second request on same socket...");
            String secondRequest = "GET /" + APP_NAME + "/page2.jsp HTTP/1.1\r\n" +
                    "Host: " + address + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            os.write(secondRequest.getBytes());
            os.flush();
            LOG.info("Sent second request");

            // Try to read the response
            boolean foundSuccessMessage = false;
            while ((line = reader.readLine()) != null) {
                LOG.info(line);
                if (line.contains("Successfully redirected!")) {
                    foundSuccessMessage = true;
                    break;
                }
            }

            // If the test gets here without an exception, the test may fail
            // We expected the connection to be closed even with ignoreWriteAfterCommit=true
            Assert.assertFalse("Connection should be closed after IOException, even with ignoreWriteAfterCommit=true",
                    foundSuccessMessage);

        } catch (SocketException e) {
            // We expect a SocketException since the connection should be closed
            LOG.info("Expected SocketException: " + e.getMessage());
            socketExceptionOccurred = true;
            // Verify that the connection was closed (we got a SocketException)
            Assert.assertTrue(
                    "Expected SocketException: ignoreWriteAfterCommit should not prevent connection closure for IOExceptions",
                    socketExceptionOccurred);
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private void updateHTTPOptions(Boolean persistValue) throws Exception {
        ServerConfiguration c = server.getServerConfiguration();
        ConfigElementList<HttpEndpoint> h = c.getHttpEndpoints();
        boolean serverXMLChanged = false; 
        for (HttpEndpoint httpEndpoint : h) {
            LOG.info("Using httpEndpoint: " + h);
            if(httpEndpoint.getHttpOptions().isIgnoreWriteAfterCommit() != persistValue){
                serverXMLChanged = true;
                httpEndpoint.getHttpOptions().setIgnoreWriteAfterCommit(persistValue);
            }
        }
        server.setMarkToEndOfLog();
        server.updateServerConfiguration(c);
        server.waitForConfigUpdateInLogUsingMark(Collections.emptySet(), serverXMLChanged ? "CWWKT0016I:.*WriteAfterRedirect.*" : "");
        server.resetLogMarks();
    }

}
