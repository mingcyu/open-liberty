/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

/**
 * Tests {@link componenttest.containers.registry.Registry} API
 * methods when using the Artifactory registry
 */
public class ArtifactoryRegistryTest {
    private static final String FORCE_EXTERNAL = "fat.test.artifactory.force.external.repo";

    private static final String REGISTRY = "fat.test.artifactory.docker.server";
    private static final String REGISTRY_USER = "fat.test.artifactory.download.user";
    private static final String REGISTRY_PASSWORD = "fat.test.artifactory.download.token";

    @BeforeClass
    public static void setupTests() throws Exception {
        // Avoid writing to the developers docker config
        File testdir = new File(System.getProperty("java.io.tmpdir"), ".docker");

        Field configDir = ArtifactoryRegistry.class.getDeclaredField("configDir");
        configDir.setAccessible(true);
        configDir.set(null, testdir);
    }

    @Before
    public void setupTest() {
        System.clearProperty(FORCE_EXTERNAL);
        System.clearProperty(REGISTRY);
        System.clearProperty(REGISTRY_USER);
        System.clearProperty(REGISTRY_PASSWORD);
    }

    @AfterClass
    public static void teardown() {
        System.clearProperty(FORCE_EXTERNAL);
        System.clearProperty(REGISTRY);
        System.clearProperty(REGISTRY_USER);
        System.clearProperty(REGISTRY_PASSWORD);
    }

    @Test
    public void testGetRegistry() throws Exception {
        // forced external
        System.setProperty(FORCE_EXTERNAL, "true");
        String registry = getConstructor().newInstance().getRegistry();
        assertTrue("Registry should have been blank", registry.isEmpty());

        // unset
        System.setProperty(FORCE_EXTERNAL, "false");
        registry = getConstructor().newInstance().getRegistry();
        assertTrue("Registry should have been blank", registry.isEmpty());

        // valid registry
        System.setProperty(REGISTRY, "artifactory.swg-devops.com");
        registry = getConstructor().newInstance().getRegistry();
        assertEquals("artifactory.swg-devops.com", registry);
    }

    @Test
    public void testGetSetupExceptionAndIsRegistryAvailable() throws Exception {
        // forced external
        System.setProperty(FORCE_EXTERNAL, "true");
        ArtifactoryRegistry registry = getConstructor().newInstance();

        Throwable t = registry.getSetupException();
        assertNotNull(t);
        assertTrue("Throwable should have been an IllegalStateException", t instanceof IllegalStateException);
        assertTrue("Throwable should have contained the force external property ", t.getMessage().contains(FORCE_EXTERNAL));

        assertFalse("Registry should not have been available", registry.isRegistryAvailable());

        // no registry
        System.setProperty(FORCE_EXTERNAL, "false");
        registry = getConstructor().newInstance();

        t = registry.getSetupException();
        assertNotNull(t);
        assertTrue("Throwable should have been an IllegalStateException", t instanceof IllegalStateException);
        assertTrue("Throwable should have contained the registry property ", t.getMessage().contains(REGISTRY));

        assertFalse("Registry should not have been available", registry.isRegistryAvailable());

        // no user
        System.setProperty(REGISTRY, "artifactory.swg-devops.com");
        registry = getConstructor().newInstance();

        t = registry.getSetupException();
        assertNotNull(t);
        assertTrue("Throwable should have been an IllegalStateException", t instanceof IllegalStateException);
        assertTrue("Throwable cause should have contained the user property ", t.getMessage().contains(REGISTRY_USER));

        assertFalse("Registry should not have been available", registry.isRegistryAvailable());

        // no password
        System.setProperty(REGISTRY_USER, "first.last@example.com");
        registry = getConstructor().newInstance();

        t = registry.getSetupException();
        assertNotNull(t);
        assertTrue("Throwable should have been an IllegalStateException", t instanceof IllegalStateException);
        assertTrue("Throwable cause should have contained the password property ", t.getMessage().contains(REGISTRY_PASSWORD));

        assertFalse("Registry should not have been available", registry.isRegistryAvailable());

        // Successful
        System.setProperty(REGISTRY_PASSWORD, "testPassword123");
        registry = getConstructor().newInstance();

        assertTrue("Registry should not have been available", registry.isRegistryAvailable());
    }

    @Test
    public void testSupportsRegistry() throws Exception {
        Map<DockerImageName, Boolean> testMap = new HashMap<>();
        // Supported registries
        testMap.put(DockerImageName.parse("docker.io/ubuntu:2.0"), Boolean.TRUE);
        testMap.put(DockerImageName.parse("ghcr.io/alpine:2.5"), Boolean.TRUE);
        testMap.put(DockerImageName.parse("icr.io/rhel:1.0"), Boolean.TRUE);
        testMap.put(DockerImageName.parse("mcr.microsoft.com/debian:5.3.0"), Boolean.TRUE);
        testMap.put(DockerImageName.parse("public.ecr.aws/suse:4.5"), Boolean.TRUE);
//        testMap.put(DockerImageName.parse("quay.io/fedora:8.7"), Boolean.TRUE);

        // Unsupported registries
        testMap.put(DockerImageName.parse("example.com/arch:6.6"), Boolean.FALSE);

        // No registry
        testMap.put(DockerImageName.parse("centos:5.4"), Boolean.FALSE);

        // local / generated images
        testMap.put(DockerImageName.parse("mint:4.12").withRegistry("localhost"), Boolean.FALSE);
        testMap.put(DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), Boolean.FALSE);
        testMap.put(DockerImageName.parse("aes:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), Boolean.FALSE);
        testMap.put(DockerImageName.parse("xor:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), Boolean.FALSE);

        ArtifactoryRegistry registry = getConstructor().newInstance();
        for (Map.Entry<DockerImageName, Boolean> entry : testMap.entrySet()) {
            boolean actual = registry.supportsRegistry(entry.getKey());
            boolean expected = entry.getValue().booleanValue();
            assertEquals("Docker image name " + entry.getKey() +
                         " did not return the expected registry support status", expected, actual);
        }
    }

    @Test
    public void testSupportsRepository() throws Exception {
        Map<DockerImageName, Boolean> testMap = new HashMap<>();
        // Supported repositories
        testMap.put(DockerImageName.parse("wasliberty-docker-remote/ubuntu:2.0"), Boolean.TRUE);
        testMap.put(DockerImageName.parse("wasliberty-ghcr-docker-remote/alpine:2.5"), Boolean.TRUE);
        testMap.put(DockerImageName.parse("wasliberty-icr-docker-remote/rhel:1.0"), Boolean.TRUE);
        testMap.put(DockerImageName.parse("wasliberty-mcr-docker-remote/debian:5.3.0"), Boolean.TRUE);
        testMap.put(DockerImageName.parse("wasliberty-aws-docker-remote/suse:4.5"), Boolean.TRUE);
//        testMap.put(DockerImageName.parse("wasliberty-quay-docker-remote/fedora:8.7"), Boolean.TRUE);

        // Unsupported repositories
        testMap.put(DockerImageName.parse("wasliberty-infrastructure-docker/arch:6.6"), Boolean.FALSE);
        testMap.put(DockerImageName.parse("wasliberty-intops-docker-local/centos:5.4"), Boolean.FALSE);
        testMap.put(DockerImageName.parse("wasliberty-internal-docker-local/mint:4.12"), Boolean.FALSE);

        // Generated images
        testMap.put(DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), Boolean.FALSE);
        testMap.put(DockerImageName.parse("aes:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), Boolean.FALSE);
        testMap.put(DockerImageName.parse("xor:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"), Boolean.FALSE);

        ArtifactoryRegistry registry = getConstructor().newInstance();
        for (Map.Entry<DockerImageName, Boolean> entry : testMap.entrySet()) {
            boolean actual = registry.supportsRepository(entry.getKey());
            boolean expected = entry.getValue().booleanValue();
            assertEquals("Docker image name " + entry.getKey() +
                         " did not return the expected repository support status", expected, actual);
        }
    }

    @Test
    public void testGetMirrorRepository() throws Exception {
        Map<DockerImageName, String> testMap = new HashMap<>();
        // Supported registries
        testMap.put(DockerImageName.parse("docker.io/ubuntu:2.0"), "wasliberty-docker-remote");
        testMap.put(DockerImageName.parse("ghcr.io/alpine:2.5"), "wasliberty-ghcr-docker-remote");
        testMap.put(DockerImageName.parse("icr.io/rhel:1.0"), "wasliberty-icr-docker-remote");
        testMap.put(DockerImageName.parse("mcr.microsoft.com/debian:5.3.0"), "wasliberty-mcr-docker-remote");
        testMap.put(DockerImageName.parse("public.ecr.aws/suse:4.5"), "wasliberty-aws-docker-remote");
//        testMap.put(DockerImageName.parse("quay.io/fedora:8.7"), "wasliberty-quay-docker-remote");

        ArtifactoryRegistry registry = getConstructor().newInstance();
        for (Map.Entry<DockerImageName, String> entry : testMap.entrySet()) {
            String actual = registry.getMirrorRepository(entry.getKey());
            String expected = entry.getValue();
            assertEquals("Docker image name " + entry.getKey() +
                         " did not return the expected mirror", expected, actual);
        }

        List<DockerImageName> testList = new ArrayList<>();
        // Unsupported registries
        testList.add(DockerImageName.parse("example.com/arch:6.6"));

        // No registry
        testList.add(DockerImageName.parse("centos:5.4"));

        // local / generated images
        testList.add(DockerImageName.parse("mint:4.12").withRegistry("localhost"));
        testList.add(DockerImageName.parse("sha256:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"));
        testList.add(DockerImageName.parse("aes:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"));
        testList.add(DockerImageName.parse("xor:5103a25d3efd8c0cbdbc80d358c5b1da91329c53e1fa99c43a8561a87eb61d3b"));

        for (DockerImageName image : testList) {
            try {
                registry.getMirrorRepository(image);
                fail("Should not have supported mirror repository for image: " + image.asCanonicalNameString());
            } catch (IllegalArgumentException e) {
                //pass
            } catch (Exception e) {
                fail("Threw wrong exception when attempting to get mirror repository for image " + image.asCanonicalNameString() +
                     " Exception: " + e.getMessage());
            }
        }
    }

    private static Constructor<ArtifactoryRegistry> getConstructor() throws Exception {
        Constructor<ArtifactoryRegistry> con = ArtifactoryRegistry.class.getDeclaredConstructor();
        con.setAccessible(true);
        return con;
    }
}
