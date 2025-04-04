/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package com.ibm.ws.security.token.ltpa.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.LTPA;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.ValidationKeys;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;

@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LTPAValidationKeyTests {

    // Initialize needed strings for the tests
    protected static String METHODS = null;
    protected static final String APP_NAME = "ltpavalidationKeyTestServer";
    protected static final String PROGRAMMATIC_API_SERVLET = "ProgrammaticAPIServlet";
    protected static final String authTypeForm = "FORM";
    protected static final String cookieName = "LtpaToken2";

    // Initialize two liberty servers for form login
    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.validationKeyTestServer1");
    private static LibertyServer server2 = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.validationKeyTestServer2");

    private static final Class<?> thisClass = LTPAValidationKeyTests.class;

    // Initialize the user
    private static final String validUser = "user1";
    private static final String validPassword = "user1pwd";

    private static final String[] serverShutdownMessages = { "CWWKS4102E", "CWWKG0058E", "CWWKG0083W", "CWWKS4106E", "CWWKS4109W", "CWWKS4110E", "CWWKS4111E", "CWWKS4112E",
                                                             "CWWKS4113W",
                                                             "CWWKS4114W", "CWWKS4115W", "CWWKS1859E" };

    // Initialize the FormLogin Clients
    private static FormLoginClient server1FlClient1;
    private static FormLoginClient server2FlClient1;

    // Define server.xml files
    private static final String DEFAULT_SERVER_XML = "server.xml";
    private static String DEFAULT_FIPS_SERVER1_XML = "server1FIPS.xml";
    private static String DEFAULT_FIPS_SERVER2_XML = "server2FIPS.xml";
    private static String BACKUP_SERVER1XML = "server1nonFIPS.xml";
    private static String BACKUP_SERVER2XML = "server2nonFIPS.xml";

    // Define the paths to the key files
    private static final String DEFAULT_KEY_PATH = "resources/security/ltpa.keys";
    private static final String VALIDATION_KEY1_PATH = "resources/security/validation1.keys";
    private static final String VALIDATION_KEY2_PATH = "resources/security/validation2.keys";
    private static final String VALIDATION_KEY3_PATH = "resources/security/validation3.keys";
    private static final String BAD_SHARED_VALIDATION_KEY2_PATH = "resources/security/validation4.keys";
    private static final String DIFFERENT_PW_VALIDATION_KEY_PATH = "resources/security/validation9.keys";
    private static final String VALIDATION_KEY1 = "validation1.keys";
    private static final String VALIDATION_KEY4 = "validation4.keys";
    private static final String VALIDATION_KEY9 = "validation9.keys";
    private static final String LTPA_LIBERTY_PASSWORD = "{xor}EzY9Oi0rJg==";
    private static String LTPA_DEFAULT_PASSWORD = "{xor}Lz4sLCgwLTs=";
    private static String LTPA_FIPS_DEFAULT_PASSWORD = "{xor}CDo9Hgw=";

    List<String> PREBUILT_KEYS = Arrays.asList(DIFFERENT_PW_VALIDATION_KEY_PATH, BAD_SHARED_VALIDATION_KEY2_PATH,
                                               VALIDATION_KEY3_PATH,
                                               VALIDATION_KEY2_PATH, VALIDATION_KEY1_PATH);

    /**  */

    // Define the paths to the alternate key files
    private static String ALT_VALIDATION_KEY1_PATH = "alternate/validation1.keys";
    private static String ALT_VALIDATION_KEY2_PATH = "alternate/validation2.keys";
    private static String ALT_VALIDATION_KEY3_PATH = "alternate/validation3.keys";
    private static String ALT_VALIDATION_KEY4_PATH = "alternate/validation4.keys";
    private static String ALT_VALIDATION_KEY9_PATH = "alternate/validation9.keys";

    // Define the paths to the alternate key files
    private static String ALT_FIPS_VALIDATION_KEY1_PATH = "alternateFIPS/validation1.keys";
    private static String ALT_FIPS_VALIDATION_KEY2_PATH = "alternateFIPS/validation2.keys";
    private static String ALT_FIPS_VALIDATION_KEY3_PATH = "alternateFIPS/validation3.keys";
    private static String ALT_FIPS_VALIDATION_KEY4_PATH = "alternateFIPS/validation4.keys";
    private static String ALT_FIPS_VALIDATION_KEY9_PATH = "alternateFIPS/validation9.keys";

    // Define fipsEnabled
    private static final boolean fipsEnabled;

    static {
        boolean isFipsEnabled = false;
        try {
            isFipsEnabled = server1.isFIPS140_3EnabledAndSupported() && server2.isFIPS140_3EnabledAndSupported();
        } catch (Exception e) {
            e.printStackTrace();
        }
        fipsEnabled = isFipsEnabled;

        if (fipsEnabled) {
            BACKUP_SERVER1XML = DEFAULT_FIPS_SERVER1_XML;
            BACKUP_SERVER2XML = DEFAULT_FIPS_SERVER2_XML;
            ALT_VALIDATION_KEY1_PATH = ALT_FIPS_VALIDATION_KEY1_PATH;
            ALT_VALIDATION_KEY2_PATH = ALT_FIPS_VALIDATION_KEY2_PATH;
            ALT_VALIDATION_KEY3_PATH = ALT_FIPS_VALIDATION_KEY3_PATH;
            ALT_VALIDATION_KEY4_PATH = ALT_FIPS_VALIDATION_KEY4_PATH;
            ALT_VALIDATION_KEY9_PATH = ALT_FIPS_VALIDATION_KEY9_PATH;
            LTPA_DEFAULT_PASSWORD = LTPA_FIPS_DEFAULT_PASSWORD;
        }
    }

    @Rule
    public TestRule passwordChecker1 = new LeakedPasswordChecker(server1);
    public TestRule passwordChecker2 = new LeakedPasswordChecker(server2);

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        // Function to make it easier to see when each test starts and ends
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nEntering test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nExiting test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }
    };

    @BeforeClass
    public static void setUpClass() throws Exception {

        // Copy validation key file (validation1.keys) to the server
        server2.useSecondaryHTTPPort();

        // Copy an ltpa.keys file into each server to prevent generation of keys at server start
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server1);
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH, server2);
        renameServerFileInLibertyRoot(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, false, server1);
        renameServerFileInLibertyRoot(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, false, server2);

        LibertyServer[] servers = { server1, server2 };

        for (LibertyServer server : servers) {

            server.setupForRestConnectorAccess();
            if (fipsEnabled) {
                File fipsServerXml;
                if (server == server1) {
                    fipsServerXml = new File(server.pathToAutoFVTTestFiles + DEFAULT_FIPS_SERVER1_XML);
                } else {
                    fipsServerXml = new File(server.pathToAutoFVTTestFiles + DEFAULT_FIPS_SERVER2_XML);
                }
                File serverXml = new File(server.pathToAutoFVTTestFiles + DEFAULT_SERVER_XML);
                Files.copy(fipsServerXml.toPath(), serverXml.toPath(), StandardCopyOption.REPLACE_EXISTING);
                server.copyFileToLibertyServerRoot(DEFAULT_SERVER_XML);
            }
            server.startServer(true);

            assertNotNull("Featurevalid did not report update was complete",
                          server.waitForStringInLogUsingMark("CWWKF0008I"));
            assertNotNull("Security service did not report it was ready",
                          server.waitForStringInLogUsingMark("CWWKS0008I"));
            assertNotNull("The application did not report is was started",
                          server.waitForStringInLogUsingMark("CWWKZ0001I"));
            // Wait for the LTPA configuration to be ready
            assertNotNull("Expected LTPA configuration ready message not found in the log.",
                          server.waitForStringInLogUsingMark("CWWKS4105I"));

        }

        server1FlClient1 = new FormLoginClient(server1, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
        server2FlClient1 = new FormLoginClient(server2, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");

    }

    @Before
    public void setUp() throws Exception {

        // Reset the ltpa.keys file to allow for clean start of subsequent tests
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server1);
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH, server2);
        renameServerFileInLibertyRoot(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, false, server1);
        renameServerFileInLibertyRoot(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, false, server2);

        LibertyServer[] servers = { server1, server2 };

        for (LibertyServer server : servers) {
            if (!server.isStarted()) {
                // Delete on setUp so that old keys are saved when the server is backed up by the framework
                for (String path : PREBUILT_KEYS) {
                    deleteFileIfExists(path, true, server);
                }
                server.startServer(true);
            }
        }

        //Copy over server.xml on set up to restore the server.xml between tests
        server1.copyFileToLibertyServerRoot(BACKUP_SERVER1XML);
        renameKeyAndWaitForMessage(BACKUP_SERVER1XML, DEFAULT_SERVER_XML, server1, "CWWKG001[7-8]I");

        server2.copyFileToLibertyServerRoot(BACKUP_SERVER2XML);
        renameKeyAndWaitForMessage(BACKUP_SERVER2XML, DEFAULT_SERVER_XML, server2, "CWWKG001[7-8]I");

        for (LibertyServer server : servers) {
            moveLogMarkForServer(server);
        }

    }

    @After
    public void after() throws Exception {
        server1FlClient1.resetClientState();
        server2FlClient1.resetClientState();

        resetServer(server1);
        resetServer(server2);
    }

    public void resetConnection() {

    }

    @AfterClass
    public static void tearDown() throws Exception {
        server1FlClient1.releaseClient();
        server2FlClient1.releaseClient();
    }

    /**
     * Verify that an SSO cookie retrieved from authentication on one server can be used on a second server using a validation key where both
     * servers use the same keysPassword for the LTPA primary keys.
     *
     * Steps:
     * <OL>
     * <LI> Configure Server #1 and Server #2 to contain different primary LTPA Keys with the same LTPA keys passwords and with monitorValidationKeysDir set to True
     * <LI> Access a simple servlet with form login using valid credentials on Server #1 and retrieve the SSO cookie
     * <LI> Copy the LTPA primary key from Server #1 and place it in Server #2
     * <LI> Attempt to access the simple servlet with form login on Server #2 using the SSO cookie from Server #1
     * </OL>
     *
     * Expected Results:
     * <OL>
     * <LI> Successful authentication to simple servlet application on server #1 using valid credentials
     * <LI> Successful retrieval of SSO cookie from server #1
     * <LI> Successful copy and rename of ltpa.keys file to server #2 to be used as validation keys
     * <LI> Successful authentication to simple servlet application on server #2 using the SSO cookie
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testValidationKeys_sameKeyPW_monitorDir_true() throws Exception {

        // Configure the servers
        configureServer("true", "10", true, server1);
        configureServer("true", "10", true, server2);

        // Copy valid ltpa keys to each server, the ltpa keys are configured using the same keysPassword
        server1.setMarkToEndOfLog();
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server1);
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server1.waitForStringInLogUsingMark("CWWKS4105I"));

        server2.setMarkToEndOfLog();
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH, server2);
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server2.waitForStringInLogUsingMark("CWWKS4105I"));

        // Replace the randomly generated LTPA keys with the known valid ltpa keys and assert the change occurs
        renameKeyAndWaitForLtpaConfigReady(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, server1);
        renameKeyAndWaitForLtpaConfigReady(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, server2);

        // Initial login to simple servlet for form login1
        server1FlClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Retrieve the SSO cookie from the login
        String server1Cookie = server1FlClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", server1Cookie);

        server2.setMarkToEndOfLog();
        // Copy the ltpa.keys file from server #1 to server #2 (server #2 now contains the ltpa.keys file from server #1 as a validation key)
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server2);

        // Wait for the LTPA configuration to be ready after the change
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server2.waitForStringInLogUsingMark("CWWKS4105I"));

        // Attempt to login to the simple servlet on server #2 and assert that the login is successful
        server2FlClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, server1Cookie);
    }

    /**
     * Verify that an SSO cookie retrieved from authentication on one server can be used on a second server using a validation key where both servers use the same
     * keysPassword for the LTPA primary keys and with the server monitorValidationKeysDir variable set to false.
     *
     * Steps:
     * <OL>
     * <LI> Configure Server #1 and Server #2 to contain different primary LTPA Keys with the same LTPA keys passwords with monitorValidationKeysDir set to False
     * <LI> Access a simple servlet with form login using valid credentials on Server #1 and retrieve the SSO cookie
     * <LI> Copy the LTPA primary key from Server #1 and place it in Server #2
     * <LI> Attempt to access the simple servlet with form login on Server #2 using the SSO cookie from Server #1
     * <LI> Configure the previously added validation key and password in Server #2 server.xml
     * <LI> Attempt to access the simple servlet with form login on Server #2 using the SSO cookie from Server #1
     * </OL>
     *
     * Expected Results:
     * <OL>
     * <LI> Successful authentication to simple servlet application on server #1 using valid credentials
     * <LI> Successful retrieval of SSO cookie from server #1
     * <LI> Successful copy of ltpa.keys file to server #2 to be used as validation keys (Server #2 contains the primary key of server #1 as a validation key)
     * <LI> Unsuccessful authentication to simple servlet application on server #2 using SSO cookie due to monitorKeysValidationDir not recognizing unspecified keys
     * <LI> Successful configuration of validation key
     * <LI> Successful authentication to simple servlet application on server #2 using SSO cookie
     * </OL>
     */
    @Mode(TestMode.FULL)
    @Test
    public void testValidationKeys_sameKeyPW_monitorDir_false() throws Exception {

        // Configure the servers
        configureServer("false", "10", false, server1);
        configureServer("false", "10", false, server2);

        // Copy valid ltpa keys to each server, the ltpa keys are configured using different keysPassword
        // Configuration message is not recognized because directory is not being monitored for changes
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server1);
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH, server2);

        // Replace the server generated LTPA keys with the known valid ltpa keys and assert the change occurs
        renameKeyAndWaitForLtpaConfigReady(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, server1);
        renameKeyAndWaitForLtpaConfigReady(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, server2);

        // Initial login to simple servlet for form login1
        server1FlClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookie from the login
        String server1Cookie = server1FlClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", server1Cookie);

        server2.setMarkToEndOfLog();

        // Copy the ltpa.keys file to server #2 (server #2 contains the ltpa.keys file from server #1 as a validation key)
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server2);

        // Attempt to login to the simple servlet on server #2 and assert that the login fails because with monitorValidationKeysDir means the server does not recognize unspecified keys file
        assertTrue("An invalid cookie should result in an authorization challenge",
                   server2FlClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, server1Cookie));

        // Dynamically add a configured validation key element into the server2 configuration
        ServerConfiguration server2Config = server2.getServerConfiguration();
        LTPA ltpa2 = server2Config.getLTPA();
        setLTPAValidationKey(ltpa2, VALIDATION_KEY1, LTPA_DEFAULT_PASSWORD);

        // Update the server configuration to recognize the changes
        updateConfigDynamically(server2, server2Config);
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server2.waitForStringInLogUsingMark("CWWKS4105I"));

        // Attempt to login to the simple servlet on server #2 and assert that the login is successful (uses validation key)
        server2FlClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, server1Cookie);
    }

    /**
     * Verify that an SSO cookie retrieved from authentication on one server can be used on a second server using a validation key where both servers use different
     * keysPassword for the LTPA primary keys.
     *
     * Steps:
     * <OL>
     * <LI> Configure Server #1 and Server #2 to contain different primary LTPA Keys with different LTPA keys passwords with monitorValidationKeysDir set to True
     * <LI> Configure Server #1 with the correct password of the key to-be added (Server #1 will be configured with a different password than server #2)
     * <LI> Access a simple servlet with form login using valid credentials on Server #1 and retrieve the SSO cookie
     * <LI> Copy the LTPA primary key from Server #1 and place it in Server #2
     * <LI> Configure the previously added validation key and password in Server #2 server.xml
     * <LI> Attempt to access the simple servlet with form login on Server #2 using the SSO cookie from Server #1
     * </OL>
     *
     * Expected Results:
     * <OL>
     * <LI> Successful authentication to simple servlet application on server #1 using valid credentials
     * <LI> Successful retrieval of SSO cookie from server #1
     * <LI> Successful copy of ltpa.keys file to server #2 to be used as validation keys
     * <LI> Successful authentication to simple servlet application on server #2 using SSO cookie
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testValidationKeys_differentKeyPW_monitorDir_true() throws Exception {

        // Configure the servers
        configureServer("true", "10", true, server1);
        configureServer("true", "10", true, server2);

        // Change the default keysPassword configured in server.xml to that of the added ltpa keys file (Liberty)
        ServerConfiguration serverConfig = server1.getServerConfiguration();
        LTPA ltpa = serverConfig.getLTPA();
        setLTPAKeyPasswordElement(ltpa, LTPA_LIBERTY_PASSWORD);
        updateConfigDynamically(server1, serverConfig);

        // Copy valid ltpa keys to each server, the ltpa keys are configured using different keysPassword
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY9_PATH, server1);

        // Replace the server generated LTPA keys with the known valid ltpa keys and assert the change occurs
        renameKeyAndWaitForLtpaConfigReady(DIFFERENT_PW_VALIDATION_KEY_PATH, DEFAULT_KEY_PATH, server1);

        server2.setMarkToEndOfLog();
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY2_PATH, server2);
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server2.waitForStringInLogUsingMark("CWWKS4105I"));

        renameKeyAndWaitForLtpaConfigReady(VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, server2);

        // Initial login to simple servlet for form login1
        server1FlClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookie from the login
        String server1Cookie = server1FlClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", server1Cookie);

        // Dynamically add the validation key element into the server2 configuration
        ServerConfiguration server2Config = server2.getServerConfiguration();
        LTPA ltpa2 = server2Config.getLTPA();
        setLTPAValidationKey(ltpa2, VALIDATION_KEY9, LTPA_LIBERTY_PASSWORD);
        updateConfigDynamically(server2, server2Config);

        // Copy the ltpa.keys file to server #2 and test authentication should be successful because monitorValidationkeysDir monitors unlisted keys files
        server2.setMarkToEndOfLog();
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY9_PATH, server2);
        assertNotNull("Expected LTPA configuration ready message not found in the log.",
                      server2.waitForStringInLogUsingMark("CWWKS4105I"));

        // Attempt to login to the simple servlet on server #2 and assert that the login is successful (uses validation key)
        server2FlClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, server1Cookie);
    }

    /**
     * Verify that an SSO cookie retrieved from authentication on one server fails on a server with an incorrectly formatted primary key and different/wrong validation key.
     * An FFDC will be created for server 2 because an invalid key (badly formatted) can not be decrypted, we can allow it to validate the SSO failure.
     *
     * Steps:
     * <OL>
     * <LI> Server #1 and Server #2 contain different primary LTPA keys with same LTPA keys passwords
     * <LI> Place an invalid primary and validation key into Server #2 (different than primary key of Server #1)
     * <LI> Access a simple servlet with form login using valid credentials on Server #1
     * <LI> Attempt to access the simple servlet with form login on Server #2 using the SSO cookie from Server #1
     * </OL>
     *
     * Expected Results:
     * <OL>
     * <LI> Successful copy and rename to ltpa.keys in server 1
     * <LI> Successful copy and rename of files to ltpa.keys and validation4.keys in server 2
     * <LI> Authentication should be successful and retrieve the SSO cookie
     * <LI> Authentication fails because the SSO token cannot be decrypted by the primary and validation key
     * </OL>
     *
     */

    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    //FFDC because validation3.keys is an incorrectly formatted corrupted key which will cause an Illegal Argument exceptions
    public void testValidationKeys_invalid_validationKey() throws Exception {

        // Configure both servers
        configureServer("true", "10", true, server1);
        configureServer("true", "10", true, server2);

        // Copy valid ltpa keys to server1 and invalid primary and valid key to server 2
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server1);
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY3_PATH, server2);
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY4_PATH, server2);

        //replace the server generated LTPA key with a known valid ltpa key
        renameKeyAndWaitForLtpaConfigReady(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, server1);
        renameKeyAndWaitForMessage(VALIDATION_KEY3_PATH, DEFAULT_KEY_PATH, server2, "CWWKS4106E");

        // Initial login to simple servlet for form login1
        server1FlClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookie from the login
        String server1Cookie = server1FlClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", server1Cookie);

        // Change the default keysPassword to that of the added ltpa keys file (Liberty)
        ServerConfiguration server2Config = server2.getServerConfiguration();
        LTPA ltpa2 = server2Config.getLTPA();
        setLTPAValidationKey(ltpa2, VALIDATION_KEY4, LTPA_DEFAULT_PASSWORD);
        updateConfigDynamically(server2, server2Config);

        // Attempt to login to the simple servlet on server #2 and assert that the login  fails as expected
        assertTrue("An invalid cookie should result in an authorization challenge",
                   server2FlClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, server1Cookie));
    }

    /**
     * Verify that copying a fips key in a non fips environment (and vice versa) will result in the correct argument exception as the key is not formatted correctly.
     *
     * Steps:
     * <OL>
     * <LI> Server #1 contains primary fips LTPA keys(in non fips) and non fips LTPA key(in fips)
     * <LI> Place a fips validation key (in non fips) and non fips validation key(in fips)
     * </OL>
     *
     * Expected Results:
     * <OL>
     * <LI> non-fips: com.ibm.websphere.ltpa.3DESKey property is missing exception
     * <LI> fips: com.ibm.websphere.ltpa.sharedKey property is missing exception
     * </OL>
     */

    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC({ "java.lang.IllegalArgumentException" })
    public void testLTPA_fipskey_in_nonfips_and_vice_versa() throws Exception {

        if (!fipsEnabled) {

            // Copy valid fips primary and validation keys into server 1
            copyFileToServerResourcesSecurityDir(ALT_FIPS_VALIDATION_KEY1_PATH, server1);
            copyFileToServerResourcesSecurityDir(ALT_FIPS_VALIDATION_KEY2_PATH, server1);

            // Configure the server, and replace the generated LTPA key with the fips key
            configureServer("true", "10", true, server1);
            renameKeyAndWaitForMessage(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, server1, "CWWKS4102E");
        }

        else {
            // Copy valid non fips primary and validation keys into server 1
            //Have to use the full path as keys get overriden on the top
            copyFileToServerResourcesSecurityDir("alternate/validation1.keys", server1);
            copyFileToServerResourcesSecurityDir("alternate/validation2.keys", server1);

            // Configure the server, and replace the generated LTPA key with the non fips key
            configureServer("true", "10", true, server1);
            renameKeyAndWaitForMessage(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, server1, "CWWKS4102E");

        }

    }

    /**
     * Verify that an SSO cookie retrieved from authentication on one server fails in a server with the correct validation key but with an incorrectly configured password.
     *
     * Steps:
     * <OL>
     * <LI> Server #1 and Server #2 contain the same primary LTPA keys and validation key respectively with different keys passwords
     * <LI> Access a simple servlet with form login using valid credentials on server #1
     * <LI> Replace the `password` property of the validation key with 'garbage' values on server #2
     * <LI> Attempt to access the SimpleServlet with form login on server #2 using the same cookie token from Server #1
     * </OL>
     *
     *
     * Expected Results:
     * <OL>
     * <LI> Succesful file copy and file rename to ltpa.keys and to validation1.keys in server 1 and server 2 respectively.
     * <LI> Authentication should be successful and retrieve the SSO cookie on server 1
     * <LI> Authentication fails on server 2, the keypassword is different
     * </OL>
     */

    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC({ "javax.crypto.BadPaddingException" })
    public void testValidationKeys_different_Validation_keysPW() throws Exception {

        configureServer("true", "10", true, server1);
        configureServer("true", "10", true, server2);

        // Copy valid ltpa keys to each server, the ltpa keys are configured using different keysPassword
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server1);
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server2);
        renameKeyAndWaitForLtpaConfigReady(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, server1);

        // Initial login to simple servlet for form login1
        server1FlClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookie from the login
        String server1Cookie = server1FlClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", server1Cookie);

        // Change the default password  to another value
        ServerConfiguration server2Config = server2.getServerConfiguration();
        LTPA ltpa2 = server2Config.getLTPA();
        setLTPAValidationKey(ltpa2, "validation1.keys", "garbage");
        updateConfigDynamically(server2, server2Config);

        // Attempt to login to the simple servlet on server #2 and assert that the login fails
        assertTrue("An invalid cookie should result in an authorization challenge",
                   server2FlClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, server1Cookie));
    }

    /**
     * Verify that an SSO cookie retrieved from authentication on one server works in another server where the validation key is set correctly, then fails when
     * the validation key switched to a wrong/invalid key.
     *
     * Steps:
     * <OL>
     * <LI> Server #1 and Server #2 contain different primary LTPA keys with different LTPA keys passwords.
     * <LI> Copy a valid key into server1 as a primary key
     * <LI> Copy an invalid key into server2 as a primary key
     * <LI> Access a simple servlet with form login using valid credentials on server #1
     * <LI> Copy the LTPA Primary key from Server #1 and place it in Server #2, renamed as validation1.key
     * <LI> Attempt to access the SimpleServlet with form login on server #2 using the same cookie token from Server #1
     * <LI> Delete existing valid validation1.keys on server 2
     * <LI> Copy an invalid validation key into Server #2, validation4.keys on server 2
     * <LI> Access a simple servlet with form login using the same server 1 cookie on server #2
     * </OL>
     *
     * Expected results:
     * <OL>
     * <LI> Succesful rename of ltpa.key containing a valid and invalid key in server 1 and server 2 respectively
     * <LI> Authentication should be successful and retrieve the SSO cookie on server 1
     * <LI> Successful validation1.keys copy created on server2
     * <LI> Authentication should be successful on server 2 using the SSO cookie on server 1
     * <LI> Successful deletion of validation1.keys on server 2
     * <LI> Successful validation4.keys copy created on server2
     * <LI> Authentication fails because keys file are invalid on server 2
     * </OL>
     */

    @Mode(TestMode.FULL)
    @Test
    public void testValidationKeys_switch_validationKeys() throws Exception {

        // Configure both servers
        configureServer("true", "10", true, server1);
        configureServer("true", "10", true, server2);

        // Copy valid ltpa keys to server1. Copy invalid keys to server 2.
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server1);
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY4_PATH, server2);

        // Replace the server generated LTPA keys with the known validation keys
        renameKeyAndWaitForLtpaConfigReady(VALIDATION_KEY1_PATH, DEFAULT_KEY_PATH, server1);
        renameKeyAndWaitForLtpaConfigReady(BAD_SHARED_VALIDATION_KEY2_PATH, DEFAULT_KEY_PATH, server2);

        // Change the default keysPassword to that of the added ltpa keys file
        ServerConfiguration serverConfig = server1.getServerConfiguration();
        LTPA ltpa = serverConfig.getLTPA();
        setLTPAKeyPasswordElement(ltpa, LTPA_DEFAULT_PASSWORD);
        updateConfigDynamically(server1, serverConfig);

        // Initial login to simple servlet for form login1
        server1FlClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookie from the login
        String server1Cookie = server1FlClient1.getCookieFromLastLogin();
        assertNotNull("Expected SSO Cookie 1 is missing.", server1Cookie);

        ServerConfiguration server2Config = server2.getServerConfiguration();
        LTPA ltpa2 = server2Config.getLTPA();

        //Copy over validation1.keys
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY1_PATH, server2);

        // Change the default keysPassword to that of the added ltpa keys file
        setLTPAValidationKey(ltpa2, VALIDATION_KEY1, LTPA_DEFAULT_PASSWORD);
        updateConfigDynamically(server2, server2Config);

        // Attempt to login to the simple servlet on server #2 and assert that the login is successful (uses validation key)
        server2FlClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, server1Cookie);

        //Delete validation1.keys
        deleteFileIfExists(VALIDATION_KEY1_PATH, true, server2);

        //Copy over validation4.keys
        copyFileToServerResourcesSecurityDir(ALT_VALIDATION_KEY4_PATH, server2);
        setLTPAValidationKey(ltpa2, VALIDATION_KEY4, LTPA_DEFAULT_PASSWORD);

        updateConfigDynamically(server2, server2Config);

        // Attempt to login to the simple servlet on server #2 and assert that the login fails
        assertTrue("An invalid cookie should result in an authorization challenge",
                   server2FlClient1.accessProtectedServletWithInvalidCookie(FormLoginClient.PROTECTED_SIMPLE, server1Cookie));

    }

    public void moveLogMarkForServer(LibertyServer server) throws Exception {
        server.setMarkToEndOfLog();
    }

    // Function to do the server configuration for all the tests.
    public void configureServer(String monitorValidationKeysDir, String monitorInterval, Boolean waitForLTPAConfigReadyMessage, LibertyServer server) throws Exception {
        configureServer(monitorValidationKeysDir, monitorInterval, waitForLTPAConfigReadyMessage, true, server);
    }

    public void configureServer(String monitorValidationKeysDir, String monitorInterval, Boolean waitForLTPAConfigReadyMessage, boolean setLogMarkToEnd,
                                LibertyServer server) throws Exception {
        configureServer("polled", monitorValidationKeysDir, monitorInterval, waitForLTPAConfigReadyMessage, true, server);
    }

    /**
     * Function to do the server configuration for all the tests.
     * Assert that the server has with a default ltpa.keys file.
     *
     * @param updateTrigger
     * @param monitorValidationKeysDir
     * @param monitorInterval
     * @param waitForLTPAConfigReadyMessage
     * @param setLogMarkToEnd
     *
     * @throws Exception
     */
    public void configureServer(String updateTrigger, String monitorValidationKeysDir, String monitorInterval, Boolean waitForLTPAConfigReadyMessage,
                                boolean setLogMarkToEnd, LibertyServer server) throws Exception {
        moveLogMarkForServer(server);
        // Get the server configuration
        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        LTPA ltpa = serverConfiguration.getLTPA();

        // Check if the configuration needs to be updated
        boolean configurationUpdateNeeded = false;

        configurationUpdateNeeded = setLTPAupdateTriggerElement(ltpa, updateTrigger)
                                    | setLTPAmonitorValidationKeysDirElement(ltpa, monitorValidationKeysDir)
                                    | setLTPAmonitorIntervalElement(ltpa, monitorInterval);

        // Apply server configuration update if needed
        if (configurationUpdateNeeded) {
            updateConfigDynamically(server, serverConfiguration);

            if (updateTrigger.equals("polled") && monitorValidationKeysDir.equals("true") && monitorInterval.equals("0")) {
                // Wait for a warning message message to be logged
                assertNotNull("Expected LTPA configuration warning message not found in the log.",
                              server.waitForStringInLogUsingMark("CWWKS4113W"));
            }

            if (waitForLTPAConfigReadyMessage) {
                // Wait for the LTPA configuration to be ready after the change
                assertNotNull("Expected LTPA configuration ready message not found in the log.",
                              server.waitForStringInLogUsingMark("CWWKS4105I"));
            }
        }

        // Assert that a default ltpa.keys file is generated
        assertFileWasCreated(DEFAULT_KEY_PATH, server);
        server.setKeysAndJVMOptsForFips();
        if (setLogMarkToEnd)
            server.setMarkToEndOfLog();
        server.setMarkToEndOfLog();
    }

    // Function to configure the password for validation keys
    public boolean setLTPAvalidationKeyPasswordElement(LTPA ltpa, String value) {
        ConfigElementList<ValidationKeys> validationKeys = ltpa.getValidationKeys();
        ValidationKeys validationKey = validationKeys.get(0);

        // Check if null
        if (validationKey.password == null) {
            validationKey.password = value;
            return true; // Config update is needed
        }

        if (!validationKey.password.equals(value)) {
            validationKey.password = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the fileName for validation keys
    public boolean setLTPAValidationKey(LTPA ltpa, String keyName, String password) {
        ConfigElementList<ValidationKeys> validationKeys = ltpa.getValidationKeys();

        ValidationKeys newValidationKey = new ValidationKeys();
        newValidationKey.fileName = keyName;
        newValidationKey.password = password;
        newValidationKey.validUntilDate = "2099-01-01T00:00:00Z";

        validationKeys.add(newValidationKey);

        return true;

    }

    // Function to configure the password for LTPA primary keys
    public boolean setLTPAKeyPasswordElement(LTPA ltpa, String value) {
        // Check if null
        if (ltpa.keysPassword == null) {
            ltpa.keysPassword = value;
            return true; // Config update is needed
        }

        if (!ltpa.keysPassword.equals(value)) {
            ltpa.keysPassword = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;

    }

    // Function to configure the fileName for validation keys
    public boolean setLTPAvalidationKeyFileNameElement(LTPA ltpa, String value) {
        ConfigElementList<ValidationKeys> validationKeys = ltpa.getValidationKeys();
        ValidationKeys validationKey = validationKeys.get(0);

        // Check if null
        if (validationKey.fileName == null) {
            validationKey.fileName = value;
            return true; // Config update is needed
        }

        if (!validationKey.fileName.equals(value)) {
            validationKey.fileName = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to set the monitorValidationKeysDir to true or false
    public boolean setLTPAmonitorValidationKeysDirElement(LTPA ltpa, String value) {
        if (!ltpa.monitorValidationKeysDir.equals(value)) {
            ltpa.monitorValidationKeysDir = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure monitorInterval to a specific value
    public boolean setLTPAmonitorIntervalElement(LTPA ltpa, String value) {
        if (!ltpa.monitorInterval.equals(value)) {
            ltpa.monitorInterval = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the expiration time for the LTPA token to a specific value
    public boolean setLTPAexpiryElement(LTPA ltpa, String value) {
        if (!ltpa.expiration.equals(value)) {
            ltpa.expiration = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to configure the updateTrigger to a specific value
    public boolean setLTPAupdateTriggerElement(LTPA ltpa, String value) {
        if (!ltpa.updateTrigger.equals(value)) {
            ltpa.updateTrigger = value;
            return true; // Config update is needed
        }
        return false; // Config update is not needed;
    }

    // Function to update the server configuration dynamically
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());

        server.updateServerConfiguration(config);
        //CWWKG0017I: The server configuration was successfully updated in {0} seconds.
        //CWWKG0018I: The server configuration was not updated. No functional changes were detected.
        server.waitForStringInLogUsingMark("CWWKG001[7-8]I");

        // Wait for feature update to be completed or LTPA configuration to get ready
        Thread.sleep(2000);
    }

    /**
     * Delete the file if it exists. If we can't delete it, then
     * throw an exception as we need to be able to delete these files.
     *
     * @param filePath
     *
     * @throws Exception
     */
    private static void deleteFileIfExists(String filePath, boolean checkFileIsGone, LibertyServer server) throws Exception {
        Log.info(thisClass, "deleteFileIfExists", "filepath: " + filePath);
        if (fileExists(filePath, 1, server)) {
            Log.info(thisClass, "deleteFileIfExists", "file exists, deleting...");
            server.deleteFileFromLibertyServerRoot(filePath);

            // Double check to make sure the file is gone
            if (checkFileIsGone && fileExists(filePath, 1, server))
                throw new Exception("Unable to delete file: " + filePath);
        }
    }

    /**
     * Assert that file was created otherwise print a message saying it's an intermittent failure.
     *
     * @param filePath
     *
     * @throws Exception
     */
    private void assertFileWasCreated(String filePath, LibertyServer server) throws Exception {
        assertTrue("The file was not created as expected. If this is an intermittent failure, then increase the wait time.",
                   fileExists(filePath, server));
    }

    /**
     * Check to see if the file exists. We will wait a bit to ensure
     * that the system was not slow to flush the file.
     *
     * @param filePath
     *
     * @throws Exception
     */
    private static boolean fileExists(String filePath, LibertyServer server) throws Exception {
        return fileExists(filePath, 5, server);
    }

    /**
     * Check to see if the file exists. We will retry a few times to ensure
     * that the system was not slow to flush the file.
     *
     * @param filePath
     * @param numberOfTries
     *
     * @throws Exception
     */
    private static boolean fileExists(String filePath, int numberOfTries, LibertyServer server) throws Exception {
        boolean exists = false;
        boolean exceptionHasBeenPrinted = false;
        int count = 0;
        do {
            // Sleep 2 seconds
            if (count != 0) {
                Thread.sleep(3000);
                Log.info(thisClass, "fileExists", "waiting 2s...");
            }
            try {
                exists = server.getFileFromLibertyServerRoot(filePath).exists();
            } catch (Exception e) {
                // The file does not exist if there's an exception
                Log.info(thisClass, "fileExists", "The file does not exist");
                exists = false;
                // We don't want to print the same exception over and over again... so we'll only print it one time.
                if (!exceptionHasBeenPrinted) {
                    e.printStackTrace();
                    exceptionHasBeenPrinted = true;
                }
            }
            count++;
        }
        // Wait up to 10 seconds for the key file to appear
        while ((!exists) && count < numberOfTries);

        return exists;
    }

    /**
     * Copies a file to the "server/resources/security/" directory
     *
     * @param sourceFile
     *
     * @throws Exception
     */
    private static void copyFileToServerResourcesSecurityDir(String sourceFile, LibertyServer server) throws Exception {
        Log.info(thisClass, "copyFileToServerResourcesSecurityDir", "sourceFile: " + sourceFile);
        String serverRoot = server.getServerRoot();
        String securityResources = serverRoot + "/resources/security";
        server.setServerRoot(securityResources);
        server.copyFileToLibertyServerRoot(sourceFile);
        server.setServerRoot(serverRoot);
    }

    /**
     * Rename the file if it exists. If we can't rename it, then
     * throw an exception as we need to be able to rename these files.
     * If checkFileIsGone is true, then we will double check to make
     * sure the file is gone.
     *
     * @param filePath
     * @param newFilePath
     * @param checkFileIsGone
     *
     * @throws Exception
     */
    private static void renameServerFileInLibertyRoot(String filePath, String newFilePath, boolean checkFileIsGone, LibertyServer server) throws Exception {
        Log.info(thisClass, "renameFileIfExists", "\nfilepath: " + filePath + "\nnewFilePath: " + newFilePath);

        if (fileExists(newFilePath, 1, server)) {
            LibertyFileManager.moveLibertyFile(server.getFileFromLibertyServerRoot(filePath), server.getFileFromLibertyServerRoot(newFilePath));
        } else {
            server.renameLibertyServerRootFile(filePath, newFilePath);
        }

        // Double check to make sure the file is gone
        if (checkFileIsGone && fileExists(filePath, 1, server))
            throw new Exception("Unable to rename file: " + filePath);
    }

    /**
     * Reset the server to the default configuration
     *
     * @throws Exception
     */
    private void resetServer(LibertyServer server) throws Exception {
        Log.info(thisClass, "resetServer", "entering");

        ServerConfiguration serverConfig = server.getServerConfiguration();
        LTPA ltpa = serverConfig.getLTPA();
        if (fipsEnabled) {
            setLTPAKeyPasswordElement(ltpa, "{xor}CDo9Hgw=");
        } else {
            setLTPAKeyPasswordElement(ltpa, "{xor}Lz4sLCgwLTs=");
        }
        updateConfigDynamically(server, serverConfig);
        server.stopServer(serverShutdownMessages);

        Log.info(thisClass, "resetServer", "exiting");
    }

    private void renameKeyAndWaitForLtpaConfigReady(String oldName, String newName, LibertyServer serv) throws Exception {
        renameKeyAndWaitForMessage(oldName, newName, serv, "CWWKS4105I");
    }

    private void renameKeyAndWaitForMessage(String oldName, String newName, LibertyServer serv, String messageRegex) throws Exception {
        if (fileExists(oldName, 1, serv)) {
            serv.setMarkToEndOfLog();
            renameServerFileInLibertyRoot(oldName, newName, true, serv);
            assertNotNull("Expected message '" + messageRegex + "' was not found in log.",
                          serv.waitForStringInLogUsingMark(messageRegex));
        } else {
            fail("File '" + oldName + "' cannot be renamed because it does not exist");
        }
    }
}
