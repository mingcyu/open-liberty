package io.openliberty.microprofile.telemetry20.internal.rest;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ext.Provider;

import io.opentelemetry.api.trace.SpanContext;

@Provider
public class TelemetryServletFilter extends AbstractTelemetry20ServletFilter {

    @Override
    protected void setHeaderTraceIDHeader(SpanContext currentSpanContext, HttpServletResponse response) {
        response.setHeader(ACCESS_TRACE_HEADER_NAME,
                           currentSpanContext.getTraceId() + ":" + currentSpanContext.getSpanId());
    }
}
