/*******************************************************************************
 * Copyright (c) 2007, 2025 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.bindings.defbnd.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NotContextException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalBusiness;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalEJB;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.RemoteBusiness;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.RemoteEJB;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalHome;
import com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestRemoteHome;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> DefaultBindingsTest.
 *
 * <dt><b>Test Description:</b>
 * <dd>The purpose of this test is to test legacy style lookups of component view and
 * business interfaces using both styles of default bindings, short and default component format.
 * <p>
 * <dt><b>Test Matrix:</b>
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testLocalBIShort lookup and call local business interface using short name.
 * <li>testRemoteBIShort lookup and call remote business interface using short name.
 * <li>testLocalCIShort lookup and call local component view interface using short name.
 * <li>testRemoteCIShort lookup and call remote component view interface using short name.
 * <li>testLocalBI lookup and call local business interface using component name.
 * <li>testRemoteBI lookup and call remote business interface using component name.
 * <li>testLocalCI lookup and call local component view interface using component name.
 * <li>testRemoteCI lookup and call remote component view interface using component name.
 * <li>testGlobalSubContexts lookup of java:global sub contexts
 * <li>testEJBLocalSubContexts lookup of ejblocal: sub-contexts
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/DefaultBindingsServlet")
public class DefaultBindingsServlet extends FATServlet {

    private static final Map<String, String> GLOBAL_ROOT_LIST = initGlobalRootList();
    private static final Map<String, String> GLOBAL_APP_LIST = initGlobalAppList();
    private static final Map<String, String> GLOBAL_MOD_LIST = initGlobalModuleList();

    private static final Map<String, String> LOCAL_ROOT_LIST = initLocalRootList();
    private static final Map<String, String> LOCAL_APP_LIST = initLocalAppList();
    private static final Map<String, String> LOCAL_MOD_LIST = initLocalModuleList();

    // NameClassPair list returned for the java:global root context
    private static final Map<String, String> initGlobalRootList() {
        Map<String, String> list = new HashMap<String, String>();
        list.put("EJB2XDefBndTestApp", "javax.naming.Context");
        list.put("EJB3DefBndTestApp", "javax.naming.Context");
        return Collections.unmodifiableMap(list);
    }

    // NameClassPair list returned for the java:global/<application> context
    private static final Map<String, String> initGlobalAppList() {
        Map<String, String> list = new HashMap<String, String>();
        list.put("EJB3DefBndBean", "javax.naming.Context");
        return Collections.unmodifiableMap(list);
    }

    // NameClassPair list returned for the java:global/<application>/<module> context
    private static final Map<String, String> initGlobalModuleList() {
        Map<String, String> list = new HashMap<String, String>();
        list.put("TestBean!com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalHome", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalHome");
        list.put("TestComponentBean!com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestRemoteComponentHome", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestRemoteComponentHome");
        list.put("TestBean!com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalBusiness", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalBusiness");
        list.put("TestBean!com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.RemoteBusiness", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.RemoteBusiness");
        list.put("TestBean!com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestRemoteHome", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestRemoteHome");
        list.put("TestComponentBean!com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.RemoteComponentBusiness", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.RemoteComponentBusiness");
        list.put("TestComponentBean!com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalComponentHome", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalComponentHome");
        list.put("TestComponentBean!com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalComponentBusiness", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalComponentBusiness");
        return Collections.unmodifiableMap(list);
    }

    // NameClassPair list returned for the ejblocal: root context
    private static final Map<String, String> initLocalRootList() {
        Map<String, String> list = new HashMap<String, String>();
        list.put("EJB3DefBndTestApp", "javax.naming.Context");
        list.put("com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalBusiness", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalBusiness");
        list.put("com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalComponentHome", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalComponentHome");
        list.put("my", "javax.naming.Context");
        list.put("ejb", "javax.naming.Context");
        list.put("com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalHome", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalHome");
        list.put("com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalComponentBusiness", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalComponentBusiness");
        return Collections.unmodifiableMap(list);
    }

    // NameClassPair list returned for the ejblocal:/<application> context
    private static final Map<String, String> initLocalAppList() {
        Map<String, String> list = new HashMap<String, String>();
        list.put("EJB3DefBndBean.jar", "javax.naming.Context");
        return Collections.unmodifiableMap(list);
    }

    // NameClassPair list returned for the ejblocal:/<application>/<module> context
    private static final Map<String, String> initLocalModuleList() {
        Map<String, String> list = new HashMap<String, String>();
        list.put("TestBean#com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalHome", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.TestLocalHome");
        list.put("TestBean#com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalBusiness", "com.ibm.ws.ejbcontainer.bindings.defbnd.ejb.LocalBusiness");
        return Collections.unmodifiableMap(list);
    }

    /**
     * lookup and call local business interface using short name.
     */
    @Test
    public void testLocalBIShort() throws Exception {
        LocalBusiness bean = Helper.lookupShortLocal(LocalBusiness.class);
        assertNotNull("1 ---> LocalBusiness short default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote business interface using short name.
     */
    @Test
    public void testRemoteBIShort() throws Exception {
        RemoteBusiness bean = Helper.lookupShortRemote(RemoteBusiness.class);
        assertNotNull("1 ---> RemoteBusiness short default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call local component view interface using short name.
     */
    @Test
    public void testLocalCIShort() throws Exception {
        TestLocalHome homeBean = Helper.lookupShortLocal(TestLocalHome.class);
        assertNotNull("1 ---> LocalHome short default lookup did not succeed.", homeBean);

        LocalEJB bean = homeBean.create();
        assertNotNull("2 ---> LocalHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote component view interface using short name.
     */
    @Test
    public void testRemoteCIShort() throws Exception {
        TestRemoteHome homeBean = Helper.lookupShortRemote(TestRemoteHome.class);
        assertNotNull("1 ---> RemoteHome short default lookup did not succeed.", homeBean);

        RemoteEJB bean = homeBean.create();
        assertNotNull("2 ---> RemoteHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call local business interface using component name.
     */
    @Test
    public void testLocalBI() throws Exception {
        LocalBusiness bean = Helper.lookupDefaultLocal(LocalBusiness.class, "TestBean");
        assertNotNull("1 ---> LocalBusiness standard default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote business interface using component name.
     */
    @Test
    public void testRemoteBI() throws Exception {
        RemoteBusiness bean = Helper.lookupDefaultRemote(RemoteBusiness.class, "TestBean");
        assertNotNull("1 ---> RemoteBusiness standard default lookup did not succeed.", bean);
        String str = bean.getString();
        assertEquals("2 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call local component view interface using component name.
     */
    @Test
    public void testLocalCI() throws Exception {
        TestLocalHome homeBean = Helper.lookupDefaultLocal(TestLocalHome.class, "TestBean");
        assertNotNull("1 ---> LocalHome standard default lookup did not succeed.", homeBean);

        LocalEJB bean = homeBean.create();
        assertNotNull("2 ---> LocalHome create successful.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    /**
     * lookup and call remote component view interface using component name.
     */
    @Test
    public void testRemoteCI() throws Exception {
        TestRemoteHome homeBean = Helper.lookupDefaultRemote(TestRemoteHome.class, "TestBean");
        assertNotNull("1 ---> RemoteHome standard default lookup did not succeed.", homeBean);

        RemoteEJB bean = homeBean.create();
        assertNotNull("2 ---> RemoteHome create did not succeed.", bean);

        String str = bean.getString();
        assertEquals("3 ---> getString() returned unexpected value", "Success", str);
    }

    private void testSubContexts(String rootContext, String rootSeparator, String application, String module, String beanName, String interfaceSeparator,
                                 String interfaceName, Map<String, String> RootList, Map<String, String> AppList, Map<String, String> ModList) throws Exception {
        String appContext = rootContext + rootSeparator + application;
        String modContext = rootContext + rootSeparator + application + "/" + module;
        String jndiName = application + "/" + module + "/" + beanName + interfaceSeparator + interfaceName;
        String jndiNameModule = module + "/" + beanName + interfaceSeparator + interfaceName;
        String jndiNameBean = beanName + interfaceSeparator + interfaceName;

        // ---------------------------------------------------------------------
        // test root context
        // ---------------------------------------------------------------------

        Context rootctx = (Context) new InitialContext().lookup(rootContext);
        assertNotNull("1 --> lookup " + rootContext + " was null", rootctx);

        rootctx = (Context) rootctx.lookup("");
        assertNotNull("2 --> lookup empty in " + rootContext + " was null", rootctx);

        try {
            rootctx = (Context) rootctx.lookup("/");
            fail("3 --> lookup / in " + rootContext + " did not fail : " + rootctx);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + rootContext + "/"));
        }

        NamingEnumeration<NameClassPair> list = rootctx.list("");
        assertNotNull("4 --> list empty for " + rootContext + " was null", list);
        verifyList(list, RootList);

        try {
            list = rootctx.list("/");
            fail("3 --> list / for " + rootContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: /"));
        }

        list = rootctx.list(application);
        assertNotNull("4 --> list " + application + " for " + rootContext + " was null", list);
        verifyList(list, AppList);

        try {
            list = rootctx.list(application + "/");
            fail("3 --> list " + application + "/  for " + rootContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + application + "/"));
        }

        list = rootctx.list(application + "/" + module);
        assertNotNull("4 --> list " + application + "/" + module + " for " + rootContext + " was null", list);
        verifyList(list, ModList);

        try {
            list = rootctx.list(application + "/" + module + "/");
            fail("3 --> list " + application + "/" + module + "/  for " + rootContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + application + "/" + module + "/"));
        }

        NamingEnumeration<Binding> bindings = rootctx.listBindings("");
        assertNotNull("4 --> listBindings empty for " + rootContext + " was null", bindings);
        verifyBindings(bindings, RootList);

        try {
            bindings = rootctx.listBindings("/");
            fail("3 --> listBindings / for " + rootContext + " did not fail : " + bindings);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: /"));
        }

        bindings = rootctx.listBindings(application);
        assertNotNull("4 --> listBindings " + application + " for " + rootContext + " was null", bindings);
        verifyBindings(bindings, AppList);

        try {
            bindings = rootctx.listBindings(application + "/");
            fail("3 --> listBindings " + application + "/  for " + rootContext + " did not fail : " + bindings);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + application + "/"));
        }

        bindings = rootctx.listBindings(application + "/" + module);
        assertNotNull("4 --> listBindings " + application + "/" + module + " for " + rootContext + " was null", bindings);
        verifyBindings(bindings, ModList);

        try {
            bindings = rootctx.listBindings(application + "/" + module + "/");
            fail("3 --> listBindings " + application + "/" + module + "/  for " + rootContext + " did not fail : " + bindings);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + application + "/" + module + "/"));
        }

        LocalBusiness bean = (LocalBusiness) rootctx.lookup(jndiName);
        assertNotNull("2 --> lookup " + jndiName + " in " + rootContext + " was null", bean);
        assertEquals("3 --> getString() returned unexpected value", "Success", bean.getString());

        // ---------------------------------------------------------------------
        // test application context
        // ---------------------------------------------------------------------

        Context appctx = (Context) rootctx.lookup(application);
        assertNotNull("4 --> lookup " + application + " for " + rootContext + " was null", appctx);

        // java:global does not support root context lookup; ejblocal: does
        if ("java:global".equals(rootContext)) {
            try {
                appctx = (Context) rootctx.lookup(appContext);
                fail("3 --> lookup " + appContext + " for " + rootContext + " did not fail : " + appctx);
            } catch (NameNotFoundException ex) {
                assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + rootContext + rootSeparator + appContext));
            }
        } else {
            appctx = (Context) rootctx.lookup(appContext);
            assertNotNull("4 --> lookup " + appContext + " for " + rootContext + " was null", appctx);
        }

        try {
            appctx = (Context) rootctx.lookup(appContext + "/");
            fail("3 --> lookup " + appContext + "/  for " + appContext + "/ did not fail : " + appctx);
        } catch (NameNotFoundException ex) {
            if ("java:global".equals(rootContext)) {
                assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + rootContext + rootSeparator + appContext + "/"));
            } else {
                // ejblocal: lookup strips off an extra ejblocal: prefix
                assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + appContext + "/"));
            }
        }

        appctx = (Context) appctx.lookup("");
        assertNotNull("4 --> lookup empty for " + appContext + " was null", appctx);

        try {
            appctx = (Context) appctx.lookup("/");
            fail("3 --> lookup /  for " + rootContext + "/" + application + " did not fail : " + appctx);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + appContext + "/"));
        }

        list = appctx.list("");
        assertNotNull("4 --> list empty for " + appContext + " was null", list);
        verifyList(list, AppList);

        try {
            list = appctx.list("/");
            fail("3 --> list / for " + appContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: /"));
        }

        try {
            list = appctx.list(application);
            fail("3 --> list " + application + " for " + appContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + application));
        }

        list = appctx.list(module);
        assertNotNull("4 --> list " + module + " for " + appContext + " was null", list);
        verifyList(list, ModList);

        try {
            list = appctx.list(module + "/");
            fail("3 --> list " + module + "/ for " + appContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + module + "/"));
        }

        bindings = appctx.listBindings("");
        assertNotNull("4 --> listBindings empty for " + appContext + " was null", bindings);
        verifyBindings(bindings, AppList);

        try {
            bindings = appctx.listBindings("/");
            fail("3 --> listBindings / for " + appContext + " did not fail : " + bindings);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: /"));
        }

        try {
            bindings = appctx.listBindings(application);
            fail("3 --> listBindings " + application + " for " + appContext + " did not fail : " + bindings);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + application));
        }

        bindings = appctx.listBindings(module);
        assertNotNull("4 --> listBindings " + module + " for " + appContext + " was null", bindings);
        verifyBindings(bindings, ModList);

        try {
            bindings = appctx.listBindings(module + "/");
            fail("3 --> listBindings " + module + "/ for " + appContext + " did not fail : " + bindings);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + module + "/"));
        }

        try {
            bean = (LocalBusiness) appctx.lookup(jndiName);
            fail("3 --> lookup " + jndiName + " for " + appContext + " did not fail : " + bean);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + appContext + "/" + jndiName));
        }

        bean = (LocalBusiness) appctx.lookup(jndiNameModule);
        assertNotNull("2 --> lookup " + jndiNameModule + " in " + appContext + " was null", bean);
        assertEquals("3 --> getString() returned unexpected value", "Success", bean.getString());

        try {
            list = appctx.list(jndiNameModule);
            fail("3 --> list " + jndiNameModule + " for " + appContext + " did not fail : " + list);
        } catch (NotContextException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NotContextException: " + appContext + "/" + jndiNameModule));
        }

        try {
            bindings = appctx.listBindings(jndiNameModule);
            fail("3 --> listBindings " + jndiNameModule + " for " + modContext + " did not fail : " + bindings);
        } catch (NotContextException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NotContextException: " + appContext + "/" + jndiNameModule));
        }

        // ---------------------------------------------------------------------
        // test module context
        // ---------------------------------------------------------------------

        Context modctx = (Context) rootctx.lookup(application + "/" + module);
        assertNotNull("4 --> lookup " + application + "/" + module + " for " + rootContext + " was null", modctx);

        modctx = (Context) appctx.lookup(module);
        assertNotNull("4 --> lookup " + module + " for " + appContext + " was null", modctx);

        // java:global does not support root context lookup; ejblocal: does
        if ("java:global".equals(rootContext)) {
            try {
                modctx = (Context) rootctx.lookup(modContext);
                fail("3 --> lookup " + modContext + " for " + rootContext + " did not fail : " + modctx);
            } catch (NameNotFoundException ex) {
                assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + rootContext + rootSeparator + modContext));
            }
        } else {
            modctx = (Context) rootctx.lookup(modContext);
            assertNotNull("4 --> lookup " + modContext + " for " + rootContext + " was null", modctx);
        }

        modctx = (Context) modctx.lookup("");
        assertNotNull("4 --> lookup empty for " + modContext + " was null", modctx);

        try {
            modctx = (Context) modctx.lookup("/");
            fail("3 --> lookup / for " + modContext + " did not fail : " + modctx);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + modContext + "/"));
        }

        list = modctx.list("");
        assertNotNull("4 --> list empty for " + modContext + " was null", list);
        verifyList(list, ModList);

        try {
            list = modctx.list("/");
            fail("3 --> list / for " + modContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: /"));
        }

        try {
            list = modctx.list(application);
            fail("3 --> list " + application + " for " + modContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + application));
        }

        try {
            list = modctx.list(module);
            fail("3 --> list " + module + " for " + modContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + module));
        }

        bindings = modctx.listBindings("");
        assertNotNull("4 --> listBindings empty for " + modContext + " was null", bindings);
        verifyBindings(bindings, ModList);

        try {
            bindings = modctx.listBindings("/");
            fail("3 --> listBindings / for " + modContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: /"));
        }

        try {
            bindings = modctx.listBindings(application);
            fail("3 --> listBindings " + application + " for " + modContext + " did not fail : " + list);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + application));
        }

        try {
            bean = (LocalBusiness) modctx.lookup(jndiName);
            fail("3 --> lookup " + jndiName + " for " + modContext + " did not fail : " + bean);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + modContext + "/" + jndiName));
        }

        try {
            bean = (LocalBusiness) modctx.lookup(jndiNameModule);
            fail("3 --> lookup " + jndiNameModule + " for " + modContext + " did not fail : " + bean);
        } catch (NameNotFoundException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NameNotFoundException: " + modContext + "/" + jndiNameModule));
        }

        bean = (LocalBusiness) modctx.lookup(jndiNameBean);
        assertNotNull("2 --> lookup " + jndiNameBean + " in " + modContext + " was null", bean);
        assertEquals("3 --> getString() returned unexpected value", "Success", bean.getString());

        try {
            list = modctx.list(jndiNameBean);
            fail("3 --> list " + jndiNameBean + " for " + modContext + " did not fail : " + list);
        } catch (NotContextException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NotContextException: " + modContext + "/" + jndiNameBean));
        }

        try {
            bindings = modctx.listBindings(jndiNameBean);
            fail("3 --> listBindings " + jndiNameBean + " for " + modContext + " did not fail : " + bindings);
        } catch (NotContextException ex) {
            assertTrue("3 --> Expected exception text not present : " + ex, ex.toString().contains("NotContextException: " + modContext + "/" + jndiNameBean));
        }
    }

    private void verifyList(NamingEnumeration<NameClassPair> foundList, Map<String, String> expectedList) throws Exception {
        Map<String, String> expected = new HashMap<String, String>(expectedList);
        int index = 0;
        while (foundList.hasMore()) {
            NameClassPair pair = foundList.next();
            System.out.println("list " + index + " : " + pair.getName() + ", " + pair.getClassName());
            index++;
            String foundName = pair.getName();
            String foundClass = pair.getClassName();
            String expectedClass = expected.get(foundName);
            assertNotNull("   --> Context.list contained unexpected entry : " + foundName + ", " + foundClass, expectedClass);
            assertEquals("   --> Context.list contained entry with incorrect type : " + foundName, expectedClass, foundClass);
            expected.remove(foundName);
        }
        if (expected.size() > 0) {
            String notFound = "";
            for (String expectedKey : expected.keySet()) {
                notFound = expectedKey + ", " + notFound;
            }
            fail("   --> Context.list did not contain all expected names : " + notFound);
        }
    }

    private void verifyBindings(NamingEnumeration<Binding> foundBindings, Map<String, String> expectedBindings) throws Exception {
        Map<String, String> expected = new HashMap<String, String>(expectedBindings);
        int index = 0;
        while (foundBindings.hasMore()) {
            Binding binding = foundBindings.next();
            System.out.println("binding " + index + " : " + binding.getName() + ", " + binding.getClassName());
            index++;
            String foundName = binding.getName();
            String foundClass = binding.getClassName();
            String expectedClass = expected.get(foundName);
            assertNotNull("   --> Context.listBinding contained unexpected entry : " + foundName + ", " + foundClass, expectedClass);
            assertEquals("   --> Context.listBinding contained entry with incorrect type : " + foundName, expectedClass, foundClass);
            expected.remove(foundName);

            if (foundClass.equals(LocalBusiness.class.getName())) {
                LocalBusiness bean = (LocalBusiness) binding.getObject();
                System.out.println("  testing bean obtained from listBindings : " + bean);
                assertNotNull("   --> Context.listBinding for LocalBusiness did not return bean", bean);
                assertEquals("   --> getString() returned unexpected value", "Success", bean.getString());
            } else if (foundClass.equals(Context.class.getName())) {
                System.out.println("  testing context obtained from listBindings : " + binding.getObject());
                assertNotNull("   --> Context.listBinding did not return the Context for a binding : " + binding.getName(), binding.getObject());
            }
        }
        if (expected.size() > 0) {
            String notFound = "";
            for (String expectedKey : expected.keySet()) {
                notFound = expectedKey + ", " + notFound;
            }
            fail("   --> Context.list did not contain all expected names : " + notFound);
        }

    }

    /**
     * lookup of java:global sub contexts.
     */
    @Test
    public void testGlobalSubContexts() throws Exception {
        String rootContext = "java:global";
        String rootSeparator = "/";
        String application = "EJB3DefBndTestApp";
        String module = "EJB3DefBndBean";
        String beanName = "TestBean";
        String interfaceSeparator = "!";
        String interfaceName = LocalBusiness.class.getName();

        testSubContexts(rootContext, rootSeparator, application, module, beanName, interfaceSeparator, interfaceName, GLOBAL_ROOT_LIST, GLOBAL_APP_LIST, GLOBAL_MOD_LIST);
    }

    /**
     * lookup of ejblocal: sub contexts.
     */
    @Test
    public void testEJBLocalSubContexts() throws Exception {
        String rootContext = "ejblocal:";
        String rootSeparator = "";
        String application = "EJB3DefBndTestApp";
        String module = "EJB3DefBndBean.jar";
        String beanName = "TestBean";
        String interfaceSeparator = "#";
        String interfaceName = LocalBusiness.class.getName();

        testSubContexts(rootContext, rootSeparator, application, module, beanName, interfaceSeparator, interfaceName, LOCAL_ROOT_LIST, LOCAL_APP_LIST, LOCAL_MOD_LIST);
    }
}
