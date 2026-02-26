package com.k12.infrastructure.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Force OpenTelemetry to create a Tracer bean to trigger span export.
 *
 * Quarkus 3.31.2's OpenTelemetry extension creates spans but fails to export them.
 * This bean forces the OpenTelemetry SDK to initialize the tracer which
 * triggers the batch span processor to export spans.
 *
 * CRITICAL: Injecting OpenTelemetry as a method parameter forces Quarkus to
 * initialize the OpenTelemetry extension during CDI startup. Without this,
 * the extension never activates and GlobalOpenTelemetry.get() returns a no-op
 * instance that creates zero spans.
 */
@ApplicationScoped
public class OpenTelemetryProducer {

    @Produces
    @Singleton
    public Tracer produceTracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("k12-backend", "1.0.0");
    }
}
