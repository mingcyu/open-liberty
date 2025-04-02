/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.File;
import com.ibm.websphere.simplicity.config.JdbcDriver;
import com.ibm.websphere.simplicity.config.Library;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_embedded;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public abstract class JTAAppAbstractTests extends AbstractSpringTests {
    @Override
    public boolean expectWebApplication() {
        return false;
    }

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {
        ConfigElementList<Library> libraries = config.getLibraries();
        libraries.clear();
        ConfigElementList<DataSource> dataSources = config.getDataSources();

        File derbyJar = new File();
        derbyJar.setName("${shared.resource.dir}/derby/derby.jar");
        Library derbyLib = new Library();
        derbyLib.setId("DerbyLib");
        derbyLib.getFiles().add(derbyJar);
        libraries.add(derbyLib);

        JdbcDriver derbyDriver = new JdbcDriver();
        derbyDriver.setLibraryRef("DerbyLib");

        Properties_derby_embedded derbyDS1Props = new Properties_derby_embedded();
        derbyDS1Props.setCreateDatabase("create");
        derbyDS1Props.setDatabaseName("memory:myDB1");

        DataSource derbyDS1 = new DataSource();
        derbyDS1.setId("DerbyDS1");
        derbyDS1.setJndiName("jdbc/DerbyDS1");
        derbyDS1.setType("javax.sql.XADataSource");
        derbyDS1.getJdbcDrivers().add(derbyDriver);
        derbyDS1.getProperties_derby_embedded().add(derbyDS1Props);
        dataSources.add(derbyDS1);

        Properties_derby_embedded derbyDS2Props = new Properties_derby_embedded();
        derbyDS2Props.setCreateDatabase("create");
        derbyDS2Props.setDatabaseName("memory:myDB2");

        DataSource derbyDS2 = new DataSource();
        derbyDS2.setId("DerbyDS2");
        derbyDS2.setJndiName("jdbc/DerbyDS2");
        derbyDS2.setType("javax.sql.XADataSource");
        derbyDS2.getJdbcDrivers().add(derbyDriver);
        derbyDS2.getProperties_derby_embedded().add(derbyDS2Props);
        dataSources.add(derbyDS2);
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_TRANSACTIONS;
    }
}
