/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry21.internal.rest;

import javax.servlet.ServletResponse;
import javax.ws.rs.ext.Provider;

import io.opentelemetry.api.trace.SpanContext;

@Provider
public class TelemetryServletFilter extends io.openliberty.microprofile.telemetry20.internal.rest.TelemetryServletFilter {

    @Override
    protected void setHeaderTraceIDHeader(SpanContext currentSpanContext, ServletResponse response) {
        //No Op on Telemetry 2.1
    }

}
