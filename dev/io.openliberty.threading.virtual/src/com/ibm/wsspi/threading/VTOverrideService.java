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
package com.ibm.wsspi.threading;

/**
 * This spi allows disabling the usage of VirtualThreads within the control of the Managed Executor framework
 */
public interface VTOverrideService {

    /**
     * This gives the extension the ability to allow or dis-allow usage of Virtual Threads within Liberty's control
     */
    public boolean allowManagedExecutorVirtualThreads();
}
