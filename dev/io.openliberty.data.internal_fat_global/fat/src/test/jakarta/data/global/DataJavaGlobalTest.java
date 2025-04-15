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
package test.jakarta.data.global;

import static org.junit.Assert.assertEquals;

import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class DataJavaGlobalTest extends FATServletClient {
    /**
     * Error messages, typically for invalid repository methods, that are
     * intentionally caused by tests to cover error paths.
     * These are ignored when checking the messages.log file for errors.
     */
    private static final String[] EXPECTED_ERROR_MESSAGES = //
                    new String[] {
                    };

    @Server("io.openliberty.data.internal.fat.global")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive DataGlobalRestApp = ShrinkWrap
                        .create(WebArchive.class, "DataGlobalRestApp.war")
                        .addPackage("test.jakarta.data.global.rest");
        ShrinkHelper.exportAppToServer(server, DataGlobalRestApp);

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(EXPECTED_ERROR_MESSAGES);
    }

    /**
     * Verify that an entity can be found in the database by querying on its Id.
     */
    @Test
    public void testFindById() throws Exception {
        String path = "/DataGlobalRestApp/data/reminder/id/10";
        JsonObject json = new HttpRequest(server, path).run(JsonObject.class);

        String err = "unexpected response: " + json;

        assertEquals(err, 10, json.getInt("id"));
        assertEquals(err, "Testing 123", json.getString("message"));
    }

}
