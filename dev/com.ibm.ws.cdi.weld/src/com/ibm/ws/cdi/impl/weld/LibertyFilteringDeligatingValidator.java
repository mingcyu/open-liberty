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
package com.ibm.ws.cdi.impl.weld;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.Producer;

import org.jboss.weld.bootstrap.BeanDeployment;
import org.jboss.weld.bootstrap.Validator;
import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.logging.MessageCallback;
import org.jboss.weld.manager.BeanManagerImpl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.impl.weld.validation.LibertyDeligatingValidatorConstructor;
import com.ibm.ws.cdi.internal.archive.liberty.ExtensionArchiveImpl;
import com.ibm.ws.cdi.internal.interfaces.CDIArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereBeanDeploymentArchive;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

//This class contains the common code that filters methods and passes them to the delegate.
//However because weld's Validator is a concrete class with no interface, this deligator
//needs to call its constructor.
//
//The method signature for the constructor changed across versions of weld, so we extend
//LibertyDeligatingValidatorConstructor which is in the versioned bundles. That class
//has a no-args constructor which calls Validator's constructor with the correct method
//signature.
public class LibertyDeligatingValidator extends LibertyDeligatingValidatorConstructor {

    private static final TraceComponent tc = Tr.register(LibertyDeligatingValidator.class);

    private final Validator deligate;
    private final Set<BeanManager> filteredBeanManagers = new HashSet<BeanManager>();

    public LibertyDeligatingValidator(Validator deligate, WebSphereCDIDeployment webSphereCDIDeployment) {
        WebSphereCDIDeploymentImpl deployment = (WebSphereCDIDeploymentImpl) webSphereCDIDeployment;
        this.deligate = deligate;
        for (WebSphereBeanDeploymentArchive bda : deployment.getRuntimeExtensionBDAs()) {

            CDIArchive archive = bda.getArchive();
            if (archive instanceof ExtensionArchiveImpl &&
                ((ExtensionArchiveImpl) archive).applicationBDAsVisible()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "<init>", "BeanManager {0} will not be validated", bda.getBeanManager());
                }
                filteredBeanManagers.add(bda.getBeanManager());
            }
        }
    }

    @Override
    public void cleanup() {
        deligate.cleanup();
    }

    @Override
    public boolean equals(Object obj) {
        return deligate.equals(obj);
    }

    @Override
    public int hashCode() {
        return deligate.hashCode();
    }

    @Override
    public String toString() {
        return deligate.toString();
    }

    @Override
    public void validateBeanNames(BeanManagerImpl arg0) {
        deligate.validateBeanNames(arg0);
    }

    @Override
    public void validateBeans(Collection<? extends Bean<?>> arg0, BeanManagerImpl arg1) {
        deligate.validateBeans(arg0, arg1);
    }

    @Override
    public void validateDecorators(Collection<? extends Decorator<?>> arg0, BeanManagerImpl arg1) {
        deligate.validateDecorators(arg0, arg1);
    }

    @Override
    public void validateDeployment(BeanManagerImpl manager, BeanDeployment deployment) {
        if (!filteredBeanManagers.contains(manager)) {
            deligate.validateDeployment(manager, deployment);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "validateDeployment", "Skipping BeanManager {0}", manager);
        }
    }

    @Override
    public void validateEventMetadataInjectionPoint(InjectionPoint ip) {
        deligate.validateEventMetadataInjectionPoint(ip);
    }

    @Override
    public void validateInjectionPoint(InjectionPoint ij, BeanManagerImpl beanManager) {
        deligate.validateInjectionPoint(ij, beanManager);
    }

    @Override
    public void validateInjectionPointForDefinitionErrors(InjectionPoint arg0, Bean<?> arg1, BeanManagerImpl arg2) {
        deligate.validateInjectionPointForDefinitionErrors(arg0, arg1, arg2);
    }

    @Override
    public void validateInjectionPointForDeploymentProblems(InjectionPoint arg0, Bean<?> arg1, BeanManagerImpl arg2) {
        deligate.validateInjectionPointForDeploymentProblems(arg0, arg1, arg2);
    }

    @Override
    public void validateInjectionPointPassivationCapable(InjectionPoint ij, Bean<?> resolvedBean, BeanManagerImpl beanManager) {
        deligate.validateInjectionPointPassivationCapable(ij, resolvedBean, beanManager);
    }

    @Override
    public void validateInterceptorDecoratorInjectionPointPassivationCapable(InjectionPoint ij, Bean<?> resolvedBean, BeanManagerImpl beanManager, Bean<?> bean) {
        deligate.validateInterceptorDecoratorInjectionPointPassivationCapable(ij, resolvedBean, beanManager, bean);
    }

    @Override
    public void validateInterceptors(Collection<? extends Interceptor<?>> arg0, BeanManagerImpl arg1) {
        deligate.validateInterceptors(arg0, arg1);
    }

    @Override
    public void validateMetadataInjectionPoint(InjectionPoint arg0, Bean<?> arg1, MessageCallback<DefinitionException> arg2) {
        deligate.validateMetadataInjectionPoint(arg0, arg1, arg2);
    }

    @Override
    public void validateProducer(Producer<?> arg0, BeanManagerImpl arg1) {
        deligate.validateProducer(arg0, arg1);
    }

    @Override
    public void validateProducers(Collection<Producer<?>> arg0, BeanManagerImpl arg1) {
        deligate.validateProducers(arg0, arg1);
    }

    @Override
    public void validateSpecialization(BeanManagerImpl arg0) {
        deligate.validateSpecialization(arg0);
    }
}
