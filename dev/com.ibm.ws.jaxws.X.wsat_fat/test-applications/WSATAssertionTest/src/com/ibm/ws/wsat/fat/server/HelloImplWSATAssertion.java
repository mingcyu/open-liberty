/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat.server;

import javax.jws.WebService;

@WebService(wsdlLocation="WEB-INF/wsdl/WSATAssertion.wsdl")
public class HelloImplWSATAssertion{
	public String sayHello(){
		return "Hello World!";
	}
	
	public String sayHelloToOther(String username){
		return "Hello " + username;
	}
}
