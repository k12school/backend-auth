package com.k12.infrastructure.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Filter to track HTTP request/response payload sizes.
 * Records payload byte counts using Micrometer DistributionSummary.
 */
@Provider
public class PayloadSizeFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Inject
    MeterRegistry registry;

    private static final String REQUEST_SIZE_TIME = "payloadSizeFilter.requestTime";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Track request payload size
        long contentLength = requestContext.getLength();
        if (contentLength > 0) {
            String uri = extractUri(requestContext.getUriInfo().getRequestUri().getPath());
            DistributionSummary.builder("http.request.payload.bytes")
                    .description("HTTP request payload size in bytes")
                    .tags("uri", uri)
                    .register(registry)
                    .record(contentLength);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        // Track response payload size
        long contentLength = responseContext.getLength();
        if (contentLength > 0) {
            String uri = extractUri(requestContext.getUriInfo().getRequestUri().getPath());
            DistributionSummary.builder("http.response.payload.bytes")
                    .description("HTTP response payload size in bytes")
                    .tags("uri", uri)
                    .tag("status", String.valueOf(responseContext.getStatus()))
                    .register(registry)
                    .record(contentLength);
        }
    }

    /**
     * Extract URI path and normalize for metrics tags.
     * Converts /api/tenants/123 -> /api/tenants/{id}
     */
    private String extractUri(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }

        // Normalize UUID patterns
        return path.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "{id}");
    }
}
