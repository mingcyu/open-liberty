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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents a &lt;webApplication> configuration element
 */
public class EnterpriseApplication extends Application {

    private String contextRoot;
    private String annotationScanLibrary;
    private String defaultClientModule;
    private String webModuleClassPathLoader;

    /**
     * @return the contextRoot
     */
    public String getContextRoot() {
        return contextRoot;
    }

    /**
     * @param contextRoot the contextRoot to set
     */
    @XmlAttribute(name = "context-root")
    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    /**
     * @param annotationScanLibrary the annotationScanLibrary option to set
     */
    @XmlAttribute
    public void setAnnotationScanLibrary(String annotationScanLibrary) {
        this.annotationScanLibrary = annotationScanLibrary;
    }

    /**
     * @return the annotationScanLibrary
     */
    public String getAnnotationScanLibrary() {
        return annotationScanLibrary;
    }

    /**
     * @param defaultClientModule the defaultClientModule option to set
     */
    @XmlAttribute
    public void setDefaultClientModule(String defaultClientModule) {
        this.defaultClientModule = defaultClientModule;
    }

    /**
     * @return the defaultClientModule
     */
    public String getDefaultClientModule() {
        return defaultClientModule;
    }

    /**
     * @param webModuleClassPathLoader the webModuleClassPathLoader option to set
     */
    @XmlAttribute
    public void setWebModuleClassPathLoader(String webModuleClassPathLoader) {
        this.webModuleClassPathLoader = webModuleClassPathLoader;
    }

    /**
     * @return the webModuleClassPathLoader
     */
    public String getWebModuleClassPathLoader() {
        return webModuleClassPathLoader;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("EnterpriseApplication{");
        buf.append(super.toString());
        if (contextRoot != null)
            buf.append("contextRoot=\"" + contextRoot + "\" ");
        if (annotationScanLibrary != null)
            buf.append("annotationScanLibrary=\"" + annotationScanLibrary + "\" ");
        if (defaultClientModule != null)
            buf.append("defaultClientModule=\"" + defaultClientModule + "\" ");
        if (webModuleClassPathLoader != null)
            buf.append("webModuleClassPathLoader=\"" + webModuleClassPathLoader + "\" ");
        buf.append("}");

        return buf.toString();
    }

    @Override
    public EnterpriseApplication clone() throws CloneNotSupportedException {
        return (EnterpriseApplication) super.clone();
    }

}
