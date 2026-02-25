package com.k12.infrastructure.tracing;

import io.opentelemetry.api.trace.Span;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Completes the early request span when response is sent.
 * Paired with EarlyRequestFilter to capture the full request lifecycle.
 */
@Provider
public class EarlyResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Retrieve the span we created in EarlyRequestFilter
        Span span = EarlyRequestFilter.getEarlySpan();

        if (span != null) {
            // Set status based on HTTP response code
            int status = responseContext.getStatus();
            if (status >= 400) {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "HTTP " + status);
            } else {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            }

            // End the span - this captures the ENTIRE request duration
            span.end();

            // Clean up ThreadLocal
            EarlyRequestFilter.clearEarlySpan();
        }
    }
}
