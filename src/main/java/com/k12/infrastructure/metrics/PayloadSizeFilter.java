package com.k12.infrastructure.metrics;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Filter to track HTTP request/response payload sizes.
 *
 * DISABLED: Micrometer metrics removed - using OpenTelemetry Java Agent for automatic instrumentation.
 * The Java Agent automatically captures HTTP metrics including request/response sizes.
 *
 * If custom payload size metrics are needed, migrate to OpenTelemetry Meter API.
 */
@Provider
public class PayloadSizeFilter implements ContainerRequestFilter, ContainerResponseFilter {

    // No-op - OpenTelemetry Java Agent provides automatic HTTP instrumentation
    // including request/response size metrics via built-in HTTP server metrics

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Automatic instrumentation by Java Agent
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Automatic instrumentation by Java Agent
    }
}
