/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.tests.anno.caching.extrascan;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

public class AnnotationScanInJarTestModeEarLib extends AbstractAnnotationScanInJarTest {

	@Override
	public List<String> getMessagesToSearchFor() {
		
		List<String> messages = new ArrayList<String>();
		messages.add("onStartup method in war file");
		messages.add("onStartup method found via jar file");
		
		return messages;
	}

	@Override
	public String getConfigMode() {
		return "earLibraries";
	}

}
