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

import java.io.File;
import java.time.Duration;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.Constants;
import io.openliberty.microprofile.health.file.healthcheck.fat.utils.HealthFileUtils;
import io.openliberty.microprofile.health.internal_fat.shared.HealthActions;

/**
 *
 */
@RunWith(FATRunner.class)
public class MPConfigDefaultValuesTest {

    final static String DEFAULT_SERVER = "HealthServer";
    final static String MPCONFIG_DEFAULT_STARTUP_UP_SERVER = "DefaultStartupUpServer";
    final static String MPCONFIG_DEFAULT_READINESS_UP_SERVER = "DefaultReadinessUpServer";
    final static String MPCONFIG_DEFAULT_ALL_UP_SERVER = "DefaultAllUpServer";

    final static String REGULAR_APP = "RegularApp";
    final static String REGULAR_APP_WAR = REGULAR_APP + ".war";

    final static String DELAYED_APP = "DelayedApp";
    final static String DELAYED_APP_WAR = DELAYED_APP + ".war";

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

    @Server(DEFAULT_SERVER)
    public static LibertyServer defaultServer;

    @Server(MPCONFIG_DEFAULT_STARTUP_UP_SERVER)
    public static LibertyServer mpConfigDefaultStartupUpServer;

    @Server(MPCONFIG_DEFAULT_READINESS_UP_SERVER)
    public static LibertyServer mpConfigDefaultReadinessUpServer;

    @Server(MPCONFIG_DEFAULT_ALL_UP_SERVER)
    public static LibertyServer mpConfigAllUpServer;

    @BeforeClass
    public static void beforeClass() {

    }

    public void cleanup(LibertyServer server) throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(FAILS_TO_START_EXPECTED_FAILURES);
        }
    }

    @Test
    public void NoDefaultConfigTest() throws Exception {
        final String METHOD_NAME = "NoDefaultConfigTest";
        LibertyServer server = defaultServer;

        WebArchive regularWAR = ShrinkWrap
                        .create(WebArchive.class, REGULAR_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

        ShrinkHelper.exportDropinAppToServer(server, regularWAR, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        WebArchive DelayedWar = ShrinkWrap
                        .create(WebArchive.class, DELAYED_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/DelayedHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.delayed.health.check.app");

        ShrinkHelper.exportDropinAppToServer(server, DelayedWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        server.startServer();

        // Wait for "CWWKZ0001I: Application RegularApp started "
        Assert.assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0001I.*" + REGULAR_APP));

        /*
         * At this point, Regular WAR is started and kicks off the health processes.
         * But the DelayedApp is still waiting to init (i.e. not started).
         */

        String serverRoot = server.getServerRoot();
        File serverRootDirFile = new File(serverRoot);

        Log.info(getClass(), METHOD_NAME, "Server root directory is: " + serverRootDirFile.getAbsolutePath());

        /*
         * Expect:
         * [X] /health dir
         * [ ] Started
         * [ ] Ready
         * [X] Live
         *
         * Not Expected:
         * [X] Started
         * [X] Ready
         * [ ] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        // SRVE0242I: [HealthDemo] [/HealthDemo] [DelayedServlet]: Initialization successful.
        server.waitForStringInLogUsingMark("SRVE0242I*DelayedServlet");

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
        //Check started and ready created
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.READY_SHOULD_HAVE, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        //Check live file had been updating
        Assert.assertTrue(HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));

        cleanup(server);
    }

    @Test
    public void DefaultStartupNoDefaultReadyConfigTest() throws Exception {
        final String METHOD_NAME = "DefaultStartupNoDefaultReadyConfigTest";
        LibertyServer server = mpConfigDefaultStartupUpServer;

        WebArchive regularWAR = ShrinkWrap
                        .create(WebArchive.class, REGULAR_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

        ShrinkHelper.exportDropinAppToServer(server, regularWAR, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        WebArchive DelayedWar = ShrinkWrap
                        .create(WebArchive.class, DELAYED_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/DelayedHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.delayed.health.check.app");

        ShrinkHelper.exportDropinAppToServer(server, DelayedWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        server.startServer();

        // Wait for "CWWKZ0001I: Application RegularApp started "
        Assert.assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0001I.*" + REGULAR_APP));

        /*
         * At this point, Regular WAR is started and kicks off the health processes.
         * But the DelayedApp is still waiting to init (i.e. not started).
         */

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
         * [] Started
         * [X] Ready
         * [ ] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.READY_SHOULD_NOT_HAVE, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        // SRVE0242I: [HealthDemo] [/HealthDemo] [DelayedServlet]: Initialization successful.
        server.waitForStringInLogUsingMark("SRVE0242I*DelayedServlet");

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

        //Check ready created
        Assert.assertTrue(Constants.READY_SHOULD_HAVE, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        //Check live has been updating
        Assert.assertTrue(HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));

        cleanup(server);
    }

    @Test
    public void DefaultReadyupNoDefaultStartupConfigTest() throws Exception {
        final String METHOD_NAME = "DefaultReadyupNoDefaultStartupConfigTest";
        LibertyServer server = mpConfigDefaultReadinessUpServer;

        WebArchive regularWAR = ShrinkWrap
                        .create(WebArchive.class, REGULAR_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

        ShrinkHelper.exportDropinAppToServer(server, regularWAR, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        WebArchive DelayedWar = ShrinkWrap
                        .create(WebArchive.class, DELAYED_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/DelayedHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.delayed.health.check.app");

        ShrinkHelper.exportDropinAppToServer(server, DelayedWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        server.startServer();

        // Wait for "CWWKZ0001I: Application RegularApp started "
        Assert.assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0001I.*" + REGULAR_APP));

        /*
         * At this point, Regular WAR is started and kicks off the health processes.
         * But the DelayedApp is still waiting to init (i.e. not started).
         */

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
         * [ ] Ready
         * [ ] Live
         */
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertFalse(Constants.STARTED_SHOULD_NOT_HAVE, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.READY_SHOULD_HAVE, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        // SRVE0242I: [HealthDemo] [/HealthDemo] [DelayedServlet]: Initialization successful.
        server.waitForStringInLogUsingMark("SRVE0242I*DelayedServlet");

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

        //Check started created
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE, HealthFileUtils.getStartFile(serverRootDirFile).exists());

        //Check ready and live have been updated
        Assert.assertTrue(HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));
        Assert.assertTrue(HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));
        cleanup(server);
    }

    @Test
    public void DefaultAllUpConfigTest() throws Exception {
        final String METHOD_NAME = "DefaultAllUpConfigTest";
        LibertyServer server = mpConfigAllUpServer;

        WebArchive regularWAR = ShrinkWrap
                        .create(WebArchive.class, REGULAR_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/FileHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.file.healthcheck.app");

        ShrinkHelper.exportDropinAppToServer(server, regularWAR, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        WebArchive DelayedWar = ShrinkWrap
                        .create(WebArchive.class, DELAYED_APP_WAR)
                        .addAsWebInfResource(new File("test-applications/DelayedHealthCheckApp/resources/WEB-INF/web.xml"))
                        .addPackage("io.openliberty.microprofile.health.delayed.health.check.app");

        ShrinkHelper.exportDropinAppToServer(server, DelayedWar, DeployOptions.SERVER_ONLY, DeployOptions.DISABLE_VALIDATION);

        server.startServer();

        // Wait for "CWWKZ0001I: Application RegularApp started "
        Assert.assertNotNull(server.waitForStringInLogUsingMark("CWWKZ0001I.*" + REGULAR_APP));

        /*
         * At this point, Regular WAR is started and kicks off the health processes.
         * But the DelayedApp is still waiting to init (i.e. not started).
         */

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
        Assert.assertTrue(Constants.HEALTH_DIR_SHOULD_HAVE, HealthFileUtils.getHealthDirFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.STARTED_SHOULD_HAVE, HealthFileUtils.getStartFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.READY_SHOULD_HAVE, HealthFileUtils.getReadyFile(serverRootDirFile).exists());
        Assert.assertTrue(Constants.LIVE_SHOULD_HAVE, HealthFileUtils.getLiveFile(serverRootDirFile).exists());

        // SRVE0242I: [HealthDemo] [/HealthDemo] [DelayedServlet]: Initialization successful.
        server.waitForStringInLogUsingMark("SRVE0242I*DelayedServlet");

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

        //Check ready and live have been updated
        Assert.assertTrue(HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getReadyFile(serverRootDirFile), Duration.ofSeconds(8)));
        Assert.assertTrue(HealthFileUtils.isLastModifiedTimeWithinLast(HealthFileUtils.getLiveFile(serverRootDirFile), Duration.ofSeconds(8)));

        cleanup(server);
    }

}
