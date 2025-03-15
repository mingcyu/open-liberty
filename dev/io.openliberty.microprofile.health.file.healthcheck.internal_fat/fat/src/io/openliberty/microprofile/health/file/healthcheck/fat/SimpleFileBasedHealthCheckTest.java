/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.health.file.healthcheck.fat;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.Constants;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.HealthFileUtils;
import io.openliberty.microprofile.health.internal_fat.shared.HealthActions;

/**
 *
 */
@RunWith(FATRunner.class)
@AllowedFFDC("javax.management.InstanceNotFoundException")
public class SimpleFileBasedHealthCheckTest {

    final static String SERVER_NAME = "HealthServer";
    final static String FAIL_START_APP = "FailStartApp";
    final static String FAIL_START_APP_WAR = FAIL_START_APP + ".war";

    final static String FAIL_LIVE_APP = "FailLiveApp";
    final static String FAIL_LIVE_APP_WAR = FAIL_LIVE_APP + ".war";

    final static String FAIL_READY_APP = "FailReadyApp";
    final static String FAIL_READY_APP_WAR = FAIL_READY_APP + ".war";
    final static String TOGGLE_APP = "ToggleApp";
    final static String TOGGLE_APP_WAR = TOGGLE_APP + ".war";

    private static final String[] FAILS_TO_START_EXPECTED_FAILURES = { "CWMMH0052W", "CWMMH0054W", "CWMMH0053W" };

    public static final int APP_STARTUP_TIMEOUT = 120 * 1000;

    private static enum HealthCheck {
        LIVE, READY, STARTED, HEALTH;
    }

    private static enum Status {
        SUCCESS, FAILURE;
    }

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(FeatureReplacementAction.ALL_SERVERS,
                                                             MicroProfileActions.MP61, // mpHealth-4.0 w/ EE9
                                                             MicroProfileActions.MP70_EE10, // mpHealth-4.0 FULL EE10
                                                             MicroProfileActions.MP70_EE11, // mpHealth-4.0 FULL EE11
                                                             HealthActions.MP14_MPHEALTH40, // mpHealth-4.0 FULL EE7
                                                             HealthActions.MP41_MPHEALTH40); //mpHealth-4.0 FULL EE8

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() {

    }

    @Before
    public void before() throws Exception {
        server.removeAllInstalledAppsForValidation();
        server.deleteAllDropinApplications();
    }

    @After
    public void after() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(FAILS_TO_START_EXPECTED_FAILURES);
        }
    }

    @Test
    /*
     * No configuration used.
     */
    public void emptyServerCheck() throws Exception {
        final String METHOD_NAME = "emptyServerCheck";

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");

        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [X] Started
         * [X] Ready
         * [X] Live
         *
         *
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        TimeUnit.SECONDS.sleep(10);

        //Check that live and ready files have been updating.
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_UPDATED,
                          HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_UPDATED, HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));

    }

    @Test
    /*
     * Startup check fails.
     */
    public void failedStartedHealthCheckTest() throws Exception {
        final String METHOD_NAME = "failedStartedHealthCheckTest";

        WebArchive testWAR = ShrinkWrap
                        .create(WebArchive.class, FAIL_START_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app")
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app.start.fail");

        ShrinkHelper.exportDropinAppToServer(server, testWAR, DeployOptions.SERVER_ONLY);

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [ ] Started
         * [X] Ready
         * [X] Live
         *
         * Not Expected:
         * [X] Started
         * [] Ready
         * [ ] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        //Started file should still not be created
        TimeUnit.SECONDS.sleep(10);
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());

        //Check that live and ready are still being updated
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_UPDATED,
                          HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_UPDATED, HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));

    }

    @Test
    /*
     * Liveness check fails.
     */
    public void failedLivenessHealthCheckTest() throws Exception {
        final String METHOD_NAME = "failedLivenessHealthCheckTest";

        WebArchive testWAR = ShrinkWrap
                        .create(WebArchive.class, FAIL_LIVE_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app")
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app.live.fail");

        ShrinkHelper.exportDropinAppToServer(server, testWAR, DeployOptions.SERVER_ONLY);

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [X] Started
         * [X] Ready
         * [ ] Live
         *
         * Not Expected:
         * [ ] Started
         * [ ] Ready
         * [X] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        TimeUnit.SECONDS.sleep(10);

        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        //Check that ready is still being updated
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_UPDATED,
                          HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));
    }

    @Test
    /*
     * Readiness check fails.
     */
    public void failedReadinessHealthCheckTest() throws Exception {
        final String METHOD_NAME = "failedReadinessHealthCheckTest";

        WebArchive app = ShrinkHelper.buildDefaultApp(FAIL_READY_APP, "io.openliberty.microprofile.health.file.healthcheck.app",
                                                      "io.openliberty.microprofile.health.file.healthcheck.app.ready.fail");

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [X] Started
         * [ ] Ready
         * [X] Live
         *
         * Not Expected:
         * [ ] Started
         * [X] Ready
         * [ ] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        TimeUnit.SECONDS.sleep(10);
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());

        //Check that live is still being updated
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_UPDATED, HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));
    }

    @Test
    /*
     * Readiness check fails during runtime.
     */
    public void toggleReadinessFailTest() throws Exception {
        final String METHOD_NAME = "toggleReadinessFailTest";

        WebArchive app = ShrinkWrap
                        .create(WebArchive.class, TOGGLE_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [X] Started
         * [X] Ready
         * [X] Live
         *
         * Not Expected:
         * [ ] Started
         * [ ] Ready
         * [ ] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        URL url = HttpUtils.createURL(server, "/" + TOGGLE_APP + "/HealthAppServlet?ready=false");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        con.connect();
        Assert.assertTrue(con.getResponseCode() == 200);

        TimeUnit.SECONDS.sleep(10);

        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_UPDATED, HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE_UPDATED,
                           HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));

    }

    @Test
    /*
     * Liveness check fails during runtime.
     */
    public void toggleLivenessFailTest() throws Exception {
        final String METHOD_NAME = "toggleLivenessFailTest";

        WebArchive app = ShrinkWrap
                        .create(WebArchive.class, TOGGLE_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

        ShrinkHelper.exportDropinAppToServer(server, app, DeployOptions.SERVER_ONLY);

        server.startServer();

        // Read to run a smarter planet
        server.waitForStringInLogUsingMark("CWWKF0011I");
        assertTrue("Server is not started", server.isStarted());

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [X] Started
         * [X] Ready
         * [X] Live
         *
         * Not Expected:
         * [ ] Started
         * [ ] Ready
         * [ ] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE_CREATED, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE_CREATED, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.READY_SHOULD_HAVE_CREATED, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE_CREATED, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        URL url = HttpUtils.createURL(server, "/" + TOGGLE_APP + "/HealthAppServlet?live=false");
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpUtils.DEFAULT_TIMEOUT, HTTPRequestMethod.GET);
        con.connect();
        Assert.assertTrue(con.getResponseCode() == 200);

        TimeUnit.SECONDS.sleep(10);

        Assert.assertTrue(Constants.READY_SHOULD_HAVE_UPDATED,
                          HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));
        Assert.assertFalse(Constants.LIVE_SHOULD_NOT_HAVE_UPDATED,
                           HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));

    }

}
