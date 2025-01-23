/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package tests;

import static org.junit.Assert.assertNotNull;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.tx.jta.ut.util.LastingXAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.transaction.fat.util.SetupRunner;
import com.ibm.ws.transaction.fat.util.TxFATServletClient;
import com.ibm.ws.transaction.fat.util.TxTestContainerSuite;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.database.container.DatabaseContainerUtil;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import web.AuthServlet;

@RunWith(FATRunner.class)
public class ContainerAuthTest extends FATServletClient {
    private static final Class<?> c = ContainerAuthTest.class;

    // Move to TxFATServletClient
    protected LibertyServer[] serversToCleanup;
    protected String[] toleratedMsgs = new String[] { ".*" };

    // Move to TxFATServletClient
    @After
    public void cleanup() throws Exception {
        try {
            // If any servers have been added to the serversToCleanup array, we'll stop them now
            // test is long gone so we don't care about messages & warnings anymore
            if (serversToCleanup != null && serversToCleanup.length > 0) {
                final String serverNames[] = new String[serversToCleanup.length];
                int i = 0;
                for (LibertyServer s : serversToCleanup) {
                    serverNames[i++] = s.getServerName();
                }
                Log.info(TxFATServletClient.class, "cleanup", "Cleaning " + String.join(", ", serverNames));
                FATUtils.stopServers(toleratedMsgs, serversToCleanup);
            } else {
                Log.info(TxFATServletClient.class, "cleanup", "No servers to stop");
            }

            if (serversToCleanup != null && serversToCleanup.length > 0) {
                // Clean up XA resource files
                serversToCleanup[0].deleteFileFromLibertyInstallRoot("/usr/shared/" + LastingXAResourceImpl.STATE_FILE_ROOT);

                // Remove tranlog DB
                serversToCleanup[0].deleteDirectoryFromLibertyInstallRoot("/usr/shared/resources/data");
            }
        } finally {
            serversToCleanup = null;
            toleratedMsgs = new String[] { ".*" };
        }
    }

    public static final String APP_NAME = "containerAuth";
    public static final String SERVLET_NAME = APP_NAME + "/AuthServlet";

    @Server("containerAuth")
    @TestServlet(servlet = AuthServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverConAuth;

    @Server("containerAuthBadUser")
    @TestServlet(servlet = AuthServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverConAuthBadUser;

    @Server("containerAuthEmbed")
    @TestServlet(servlet = AuthServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverEmbed;

    @Server("containerAuthEmbedBadUser")
    @TestServlet(servlet = AuthServlet.class, contextRoot = APP_NAME)
    public static LibertyServer serverEmbedBadUser;

    private static ShrinkHelper.DeployOptions[] NO_DEPLOY_OPTIONS = new ShrinkHelper.DeployOptions[] {};

    public static SetupRunner runner = new SetupRunner() {
        @Override
        public void run(LibertyServer s) throws Exception {
            setUp(s);
        }
    };

    @BeforeClass
    public static void init() throws Exception {
        Log.info(c, "init", "BeforeClass");

        final WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "web.*");
        ShrinkHelper.exportAppToServer(serverConAuth, app, NO_DEPLOY_OPTIONS);
        ShrinkHelper.exportAppToServer(serverConAuthBadUser, app, NO_DEPLOY_OPTIONS);
        ShrinkHelper.exportAppToServer(serverEmbed, app, NO_DEPLOY_OPTIONS);
        ShrinkHelper.exportAppToServer(serverEmbedBadUser, app, NO_DEPLOY_OPTIONS);
    }

    public static void setUp(LibertyServer server) throws Exception {
        JdbcDatabaseContainer<?> testContainer = TxTestContainerSuite.testContainer;
        //Get driver name
        server.addEnvVar("DB_DRIVER", DatabaseContainerType.valueOf(testContainer).getDriverName());

        //Setup server DataSource properties
        DatabaseContainerUtil.setupDataSourceProperties(server, testContainer);

        server.setServerStartTimeout(FATUtils.LOG_SEARCH_TIMEOUT);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

            @Override
            public Void run() throws Exception {
                ShrinkHelper.cleanAllExportedArchives();
                return null;
            }
        });
    }

    @Test
    @AllowedFFDC(value = { "javax.resource.spi.SecurityException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.resource.spi.ResourceAllocationException" })
    public void testContainerAuth() throws Exception {

        serversToCleanup = new LibertyServer[] { serverConAuth };

        // Start serverConAuth
        FATUtils.startServers(runner, serverConAuth);

        // Check for key string to see whether it succeeded
        String configStrRestart = serverConAuth.waitForStringInTrace("ContainerAuthData is configured");
        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", configStrRestart);
        // Do a little tx work
        runTest(serverConAuth, SERVLET_NAME, "testUserTranLookup");
    }

    @Test
    @AllowedFFDC(value = { "javax.resource.spi.SecurityException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.resource.spi.ResourceAllocationException" })
    public void testContainerAuthBadUser() throws Exception {

        serversToCleanup = new LibertyServer[] { serverConAuthBadUser };

        // Start serverConAuthBadUser
        FATUtils.startServers(runner, serverConAuthBadUser);

        // Server appears to have started ok. Check for key string to see whether recovery has succeeded
        String configStrRestart = serverConAuthBadUser.waitForStringInTrace("ContainerAuthData is configured");
        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", configStrRestart);
        // Do a little tx work
        runTest(serverConAuthBadUser, SERVLET_NAME, "testUserTranLookup");

        // Container authentication is configured but to an invalid user name. The recovery log should fail.
        String logFailStr = serverConAuthBadUser.waitForStringInLog("CWRLS0008_RECOVERY_LOG_FAILED");
        assertNotNull("Recovery log did not fail", logFailStr);
    }

    @Test
    @AllowedFFDC(value = { "javax.resource.spi.SecurityException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.resource.spi.ResourceAllocationException" })
    public void testContainerAuthEmbed() throws Exception {

        serversToCleanup = new LibertyServer[] { serverEmbed };

        // Start serverEmbed
        FATUtils.startServers(runner, serverEmbed);
        // Check for key string to see whether it succeeded
        String configStrRestart = serverEmbed.waitForStringInTrace("ContainerAuthData is configured");
        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", configStrRestart);

        // Do a little tx work
        runTest(serverEmbed, SERVLET_NAME, "testUserTranLookup");
    }

    @Test
    @AllowedFFDC(value = { "javax.resource.spi.SecurityException", "com.ibm.ws.recoverylog.spi.InternalLogException",
                           "javax.resource.spi.ResourceAllocationException" })
    public void testContainerAuthEmbedBadUser() throws Exception {

        serversToCleanup = new LibertyServer[] { serverEmbedBadUser };

        // Start serverEmbedBadUser
        FATUtils.startServers(runner, serverEmbedBadUser);
        // Check for key string to see whether it succeeded
        String configStrRestart = serverEmbedBadUser.waitForStringInTrace("ContainerAuthData is configured");
        // There should be a match so fail if there is not.
        assertNotNull("Container authentication has unexpectedly not been configured", configStrRestart);

        // Do a little tx work
        runTest(serverEmbedBadUser, SERVLET_NAME, "testUserTranLookup");

        // Container authentication is configured but to an invalid user name. The recovery log should fail.
        String logFailStr = serverEmbedBadUser.waitForStringInLog("CWRLS0008_RECOVERY_LOG_FAILED");
        assertNotNull("Recovery log did not fail", logFailStr);
    }
}
