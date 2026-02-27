package com.k12.infrastructure.rest.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * JAX-RS endpoint to proxy OpenAPI spec with CORS support
 */
@Path("/openapi")
public class OpenApiProxyResource {

    @ConfigProperty(name = "quarkus.http.application-root", defaultValue = "/")
    String applicationRoot;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(hidden = true)
    public Response getOpenApiSpec(@Context UriInfo uriInfo) {
        try {
            // Fetch the spec from the internal Smallryy OpenAPI endpoint with JSON format
            URI internalUri = URI.create("http://localhost:8080/q/openapi.json");
            HttpRequest request =
                    HttpRequest.newBuilder().uri(internalUri).GET().build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return Response.ok(response.body()).type(MediaType.APPLICATION_JSON).build();
        } catch (IOException | InterruptedException e) {
            return Response.serverError()
                    .entity("{\"error\": \"Failed to fetch OpenAPI spec: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
