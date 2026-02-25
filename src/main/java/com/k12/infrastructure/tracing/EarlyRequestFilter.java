package com.k12.infrastructure.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * EARLIEST POSSIBLE instrumentation point - runs BEFORE ALL framework processing.
 *
 * Execution order:
 * 1. @PreMatching filters (this class - earliest JAX-RS point)
 * 2. JAX-RS routing
 * 3. Security filters (JWT validation)
 * 4. JAX-RS resource methods
 * 5. Business logic
 *
 * @PreMatching ensures this runs BEFORE JAX-RS routing!
 */
@PreMatching
@Provider
public class EarlyRequestFilter implements ContainerRequestFilter {

    @Inject
    Tracer tracer;

    // ThreadLocal to store span across request/response
    private static final ThreadLocal<Span> EARLY_SPAN = new ThreadLocal<>();
    private static final ThreadLocal<Scope> EARLY_SCOPE = new ThreadLocal<>();

    public static Span getEarlySpan() {
        return EARLY_SPAN.get();
    }

    public static void clearEarlySpan() {
        EARLY_SPAN.remove();
        EARLY_SCOPE.remove();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Create span that captures EVERYTHING from this point forward
        Span span = tracer.spanBuilder("HTTP_REQUEST_PRE_SECURITY")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        // Store in ThreadLocal for later access
        EARLY_SPAN.set(span);

        // Make this the active span
        Scope scope = span.makeCurrent();
        EARLY_SCOPE.set(scope);
    }
}
