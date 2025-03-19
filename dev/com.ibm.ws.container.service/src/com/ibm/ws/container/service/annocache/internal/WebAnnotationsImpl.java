/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
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
package com.ibm.ws.container.service.annocache.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.ws.container.service.annocache.FragmentAnnotations;
import com.ibm.ws.container.service.annocache.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ApplicationInfoForContainer;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.container.service.config.WebFragmentsInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/*
 * Web module annotation service implementation.
 *
 * This implementation acts (in effect) as both a Future<AnnotationTargets_Targets>
 * and a Future<InfoStore>, with a three part resolution:
 *
 * 1) An initial adapt is performed on the root adaptable container of a module.
 *    Currently, the module must be a web module.
 *
 * 2) Completion parameters are assigned into the future: These are an application
 *    name, a module name, and a module root classloader.
 *
 * 3) The future is resolved through an appropriate getter.
 *
 * The implementation performs steps using web module rules.
 *
 * Note that the initial adapt call accepts four parameters.  The additional
 * parameters are accepted as debugging assists.
 *
 * The expected usage is for a target module to obtain an annotation services
 * object, and to retain a reference to that services object.
 *
 * The services object has retained state, which is shared between the two
 * obtainable objects.  That allows the class source (which has useful tables
 * of class lookup information) to be shared, and provides storage so that
 * multiple callers obtain the same target or info store objects.
 *
 * Current references are from:
 *
 * com.ibm.ws.webcontainer.osgi.DeployedModImpl.adapt(Class<T>)
 *
 * That adapt implementation provides three entries into the annotation
 * services:
 *
 * *) DeployedModule adapt to ClassSource_Aggregate
 * *) DeployedModule adapt to AnnotationTargets_Targets
 * *) DeployedModule adapt to ClassSource
 *
 * Notification plan:
 *
 * Adaptation to annotation targets requires a possibly time consuming scan.
 *
 * Informational messages are generated for the initiation of a scan, and for the
 * completion of a scan.
 */
public class WebAnnotationsImpl extends ModuleAnnotationsImpl implements WebAnnotations {

    public static enum AnnotationScanLibarayValues {
        earLib, ManifestLib
    }

    public WebAnnotationsImpl(
                              AnnotationsAdapterImpl annotationsAdapter,
                              Container rootContainer, OverlayContainer rootOverlayContainer,
                              ArtifactContainer rootArtifactContainer, Container rootAdaptableContainer,
                              WebModuleInfo webModuleInfo) throws UnableToAdaptException {

        super(annotationsAdapter, rootContainer, rootOverlayContainer, rootArtifactContainer, rootAdaptableContainer, webModuleInfo);

        this.webModuleName = webModuleInfo.getName();
        this.webFragments = rootAdaptableContainer.adapt(WebFragmentsInfo.class);
        // throws UnableToAdaptException

        this.fragmentToPath = new IdentityHashMap<WebFragmentInfo, String>();
        this.pathToFragments = new HashMap<String, WebFragmentInfo>();
    }

    //

    @Override
    public WebModuleInfo getModuleInfo() {
        return (WebModuleInfo) super.getModuleInfo();
    }

    //

    private final String webModuleName;

    @Override
    public String getWebModuleName() {
        return webModuleName;
    }

    //

    private final WebFragmentsInfo webFragments;

    @Override
    public WebFragmentsInfo getWebFragments() {
        return webFragments;
    }

    @Override
    public List<WebFragmentInfo> getOrderedItems() {
        return getWebFragments().getOrderedFragments();
    }

    @Override
    public List<WebFragmentInfo> getExcludedItems() {
        return getWebFragments().getExcludedFragments();
    }

    //

    private final Map<String, WebFragmentInfo> pathToFragments;
    private final Map<WebFragmentInfo, String> fragmentToPath;

    private String getFragmentPath(WebFragmentInfo fragment) {
        return fragmentToPath.get(fragment);
    }

    private String getUniquePath(String fragmentPath) {
        String uniquePath = fragmentPath;

        int count = 1;
        while (pathToFragments.containsKey(uniquePath)) {
            uniquePath = fragmentPath + "_" + count;
            count++;
        }

        return uniquePath;
    }

    private String putUniquePath(WebFragmentInfo fragment, String fragmentPath) {
        String uniqueFragmentPath = getUniquePath(fragmentPath);

        fragmentToPath.put(fragment, uniqueFragmentPath);
        pathToFragments.put(uniqueFragmentPath, fragment);

        return uniqueFragmentPath;
    }

    //

    @Override
    protected void addInternalToClassSource() {
        String methodName = "addInternalToClassSource";

        if (rootClassSource == null) {
            return;
        }

        ClassSource_Factory classSourceFactory = getClassSourceFactory();
        if (classSourceFactory == null) {
            return;
        }

        //May be empty or missing entries if the relevant scan isn't enabled
        List<Container> extraLibs = getLibsFromAppContainer();

        // The classes folder is processed as if it were a fragment item.

        // Web module internal class path locations are categorized as either:
        //  'SEED': Non-metadata-complete, non-excluded
        //  'PARTIAL': Metadata-complete, non-excluded
        //  'EXCLUDED': Excluded
        //
        // Where 'excluded' means excluded by an absolute ordering element
        // of the web module deployment descriptor.  When an absolute ordering
        // element is present in the descriptor, if the element does not contain
        // an 'others' element, any fragment not listed in the element is an
        // excluded element.  Less class information is used from excluded
        // fragments than is used from partial fragments.

        for (WebFragmentInfo nextFragment : getOrderedItems()) {
            String nextUri = nextFragment.getLibraryURI();
            Container nextContainer = nextFragment.getFragmentContainer();

            //Defensive programming to avoid duplicates
            extraLibs.remove(nextContainer);

            boolean nextIsMetadataComplete;
            ScanPolicy nextPolicy;
            if (nextFragment.isSeedFragment()) {
                nextPolicy = ClassSource_Aggregate.ScanPolicy.SEED;
                nextIsMetadataComplete = false;
            } else {
                nextPolicy = ClassSource_Aggregate.ScanPolicy.PARTIAL;
                nextIsMetadataComplete = true;
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": Fragment [ " + nextFragment + " ]");
                Tr.debug(tc, methodName + ": URI [ " + nextUri + " ]");
                Tr.debug(tc, methodName + ": Container [ " + nextContainer + " ]");
                Tr.debug(tc, methodName + ": Metadata Complete [ " + nextIsMetadataComplete + " ]");
            }

            String nextPrefix;
            if (nextUri.equals("WEB-INF/classes")) {
                // The expectation is that the supplied container is twice nested
                // local child of the module container.
                nextContainer = nextContainer.getEnclosingContainer().getEnclosingContainer();
                nextPrefix = "WEB-INF/classes/";
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + ": Assigned Prefix [ " + nextPrefix + " ]");
                }
            } else {
                nextPrefix = null;
            }

            String nextPath = getContainerPath(nextContainer);
            if (nextPath == null) {
                return; // FFDC in 'getContainerPath'
            }
            nextPath = putUniquePath(nextFragment, nextPath);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": Fragment [ " + nextFragment + " ]");
                Tr.debug(tc, methodName + ": Path [ " + nextPath + " ]");
            }

            if (!addContainerClassSource(nextPath, nextContainer, nextPrefix, nextPolicy)) {
                return; // FFDC in 'addContainerClassSource'
            }
        }

        for (Container c : extraLibs) {
            try {
                //If we ever expand extraLibs to include shared libaries this section will need rewriting.
                Entry entry = c.adapt(Entry.class);
                if (!addContainerClassSource(entry.getPath(), c, ClassSource_Aggregate.ScanPolicy.SEED)) {
                    return; // FFDC in 'addContainerClassSource'
                }
            } catch (UnableToAdaptException e) {
                return;
            }
        }

        for (WebFragmentInfo nextFragment : getExcludedItems()) {
            String nextUri = nextFragment.getLibraryURI();
            Container nextContainer = nextFragment.getFragmentContainer();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": Fragment [ " + nextFragment + " ]");
                Tr.debug(tc, methodName + ": URI [ " + nextUri + " ]");
                Tr.debug(tc, methodName + ": Container [ " + nextContainer + " ]");
                Tr.debug(tc, methodName + ": Excluded [ true ]");
            }

            String nextPath = getContainerPath(nextContainer);
            if (nextPath == null) {
                return; // FFDC in 'getContainerPath'
            }
            nextPath = putUniquePath(nextFragment, nextPath);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + ": Fragment [ " + nextFragment + " ]");
                Tr.debug(tc, methodName + ": Path [ " + nextPath + " ]");
            }

            if (!addContainerClassSource(nextPath, nextContainer, ClassSource_Aggregate.ScanPolicy.EXCLUDED)) {
                return; // FFDC in 'addContainerClassSource'
            }
        }
    }

    private List<Container> getLibsFromAppContainer() {

        //Shared libs are not supported
        //List<Container> sharedLibraries = new LinkedList<Container>();
        List<Container> manifestClasspathLibraries = new LinkedList<Container>();;
        List<Container> earLibraries = new LinkedList<Container>();

        NonPersistentCache cache;
        try {
            cache = getAppContainer().adapt(NonPersistentCache.class);
            // 'adapt' throws UnableToAdaptException
        } catch (UnableToAdaptException e) {
            return null; // FFDC
        }

        Set<AnnotationScanLibarayValues> scanOptions = getAnnotationScanLibarayValues(cache);

        ApplicationClassesContainerInfo appClassesInfo = (ApplicationClassesContainerInfo) cache.getFromCache(ApplicationClassesContainerInfo.class);

        if (scanOptions.contains(AnnotationScanLibarayValues.ManifestLib)) {
            for (ModuleClassesContainerInfo moduleClassesInfo : appClassesInfo.getModuleClassesContainerInfo()) {
                for (ContainerInfo containerInfo : moduleClassesInfo.getClassesContainerInfo()) {
                    if (containerInfo.getType() == ContainerInfo.Type.MANIFEST_CLASSPATH
                        && containerInfo.getContainer() != null) {
                        manifestClasspathLibraries.add(containerInfo.getContainer());
                    }
                }
            }
        }

        if (scanOptions.contains(AnnotationScanLibarayValues.earLib)) { // || scanSharedLibs) { //Shared libs are not supported
            for (ContainerInfo containerInfo : appClassesInfo.getLibraryClassesContainerInfo()) {
                if (scanOptions.contains(AnnotationScanLibarayValues.earLib) && containerInfo.getType() == ContainerInfo.Type.EAR_LIB
                    && containerInfo.getContainer() != null) {
                    earLibraries.add(containerInfo.getContainer());

                    //Shared libs are not supported
                    /*
                     * } else if (scanSharedLibs && containerInfo instanceof LibraryClassesContainerInfo) {
                     * LibraryClassesContainerInfo libContainerInfo = (LibraryClassesContainerInfo) containerInfo;
                     * if (libContainerInfo.getLibraryType() == LibraryContainerInfo.LibraryType.COMMON_LIB) { //TODO, do we need this check?
                     * libContainerInfo.getClassesContainerInfo().stream().map(ContainerInfo::getContainer).filter(Objects::nonNull).forEach(sharedLibraries::add);
                     * }
                     * }
                     */
                }
            }
        }

        List<Container> toReturn = new LinkedList<Container>(); //TODO, reread slack and ensure these are added in the right order
        toReturn.addAll(manifestClasspathLibraries);
        //toReturn.addAll(sharedLibraries);
        toReturn.addAll(earLibraries);
        return toReturn;
    }

    //

    private Set<AnnotationScanLibarayValues> getAnnotationScanLibarayValues(NonPersistentCache cache) {

        Set<AnnotationScanLibarayValues> foundValues = new HashSet<AnnotationScanLibarayValues>();

        ApplicationInfoForContainer applicationInformation = (ApplicationInfoForContainer) cache.getFromCache(ApplicationInfoForContainer.class);
        String rawValues = applicationInformation.getAnnotationScanLibaray();

        for (String s : rawValues.split(",")) {
            if (s.trim().toLowerCase().equals("all")) {
                foundValues.add(AnnotationScanLibarayValues.earLib);
                foundValues.add(AnnotationScanLibarayValues.ManifestLib);
                break;
            } else if (s.trim().toLowerCase().equals("earLibrary")) {
                foundValues.add(AnnotationScanLibarayValues.earLib);
            } else if (s.trim().toLowerCase().equals("manifestClassPath")) {
                foundValues.add(AnnotationScanLibarayValues.ManifestLib);
            }
        }

        return foundValues;
    }

    @Override
    public FragmentAnnotations getFragmentAnnotations(WebFragmentInfo fragment) {
        AnnotationTargets_Targets useTargets = getTargets();
        if (useTargets == null) {
            return null;
        }
        return new FragmentAnnotationsImpl(useTargets, getFragmentPath(fragment));
    }
}
