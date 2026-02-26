package com.k12.infrastructure.rest.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Temporary test resource to verify OpenTelemetry Java Agent instrumentation.
 * DELETE THIS after confirming telemetry exports to SigNoz.
 */
@Path("/otel-test")
@Produces(MediaType.TEXT_PLAIN)
public class OtelTestResource {

    @GET
    public String test() {
        return "ok";
    }
}
