package com.k12.telemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Minimal test endpoint to verify OpenTelemetry instrumentation.
 * This SHOULD produce trace spans when OpenTelemetry is active.
 */
@Path("/test")
public class TelemetryTestResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "OpenTelemetry test endpoint - if tracing works, this request produces a span";
    }
}
