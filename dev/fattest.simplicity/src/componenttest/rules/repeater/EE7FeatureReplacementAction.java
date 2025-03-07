/*******************************************************************************
 * Copyright (c) 2018,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.rules.repeater;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class EE7FeatureReplacementAction extends FeatureReplacementAction {

    public static final String ID = "EE7_FEATURES";

    static final String[] EE7_FEATURES_ARRAY = { "appClientSupport-1.0",
                                                 "javaee-7.0",
                                                 "webProfile-7.0",
                                                 "javaeeClient-7.0",
                                                 "servlet-3.1",
                                                 "javaMail-1.5",
                                                 "cdi-1.2",
                                                 "jca-1.7",
                                                 "jcaInboundSecurity-1.0",
                                                 "jpa-2.1",
                                                 "jpaContainer-2.1",
                                                 "batch-1.0",
                                                 "batchManagement-1.0",
                                                 "beanValidation-1.1",
                                                 "jaxrs-2.0",
                                                 "jaxrsClient-2.0",
                                                 "jsf-2.2",
                                                 "jsfContainer-2.2",
                                                 "appSecurity-2.0",
                                                 "jsonp-1.0",
                                                 "jsp-2.3",
                                                 "ejb-3.2",
                                                 "ejbHome-3.2",
                                                 "ejbLite-3.2",
                                                 "ejbPersistentTimer-3.2",
                                                 "ejbRemote-3.2",
                                                 "ejbTest-1.0",
                                                 "el-3.0",
                                                 "jmsMdb-3.2",
                                                 "concurrent-1.0",
                                                 "jaxb-2.2",
                                                 "managedBeans-1.0",
                                                 "mdb-3.2",
                                                 "componenttest-1.0",
                                                 "txtest-1.0",
                                                 "websocket-1.1",
                                                 "jms-2.0",
                                                 "wasJmsClient-2.0",
                                                 "wasJmsServer-1.0",
                                                 "wasJmsSecurity-1.0",
                                                 "jaxws-2.2" };

    public static final Set<String> EE7_FEATURE_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE7_FEATURES_ARRAY)));

    public EE7FeatureReplacementAction() {
        super(EE7_FEATURE_SET);
        removeFeatures(EE6FeatureReplacementAction.EE6_FEATURE_SET);
        removeFeatures(EE8FeatureReplacementAction.EE8_FEATURE_SET);
        removeFeatures(JakartaEE9Action.EE9_FEATURE_SET);
        forceAddFeatures(false);
        withID(ID);
    }

    @Override
    public String toString() {
        return "Set all features to EE7 compatibility";
    }

}
