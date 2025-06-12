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
 * This SPI allows disabling the creation of virtual threads within the control of Liberty.
 * When creation of virtual threads is disabled, Liberty will create platform threads instead.
 */
public interface ThreadTypeOverride {

    /**
     * This gives the extension the ability to allow or disallow creation of virtual threads within Liberty's control
     */
    public boolean allowVirtualThreadCreation();
}
