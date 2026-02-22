package com.k12.tenant.infrastructure.rest.resource;

import com.k12.common.domain.model.TenantId;
import com.k12.tenant.application.service.TenantService;
import com.k12.tenant.domain.models.events.TenantEvents;
import com.k12.tenant.infrastructure.rest.dto.CreateTenantRequest;
import com.k12.tenant.infrastructure.rest.dto.TenantResponse;
import com.k12.tenant.infrastructure.rest.mapper.ErrorResponseMapper;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Path("/api/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Operations for managing tenants")
public class TenantResource {

    private final TenantService tenantService;

    @POST
    @Operation(
            summary = "Create a new tenant",
            description = "Creates a new tenant with the provided name and subdomain")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Tenant created successfully",
                    content = @Content(schema = @Schema(implementation = TenantResource.TenantEventDTO.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(schema = @Schema(implementation = com.k12.tenant.infrastructure.rest.dto.ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "409",
                    description = "Tenant with the same subdomain already exists",
                    content = @Content(schema = @Schema(implementation = com.k12.tenant.infrastructure.rest.dto.ErrorResponse.class)))
    })
    public Response createTenant(
            @Parameter(description = "Tenant creation request", required = true) @Valid CreateTenantRequest request) {

        var result = tenantService.createTenant(request);
        return result.fold(this::created, ErrorResponseMapper::toResponse);
    }

    @GET
    @Path("/{id}")
    @Operation(
            summary = "Get tenant by ID",
            description = "Retrieves a tenant by its unique identifier")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tenant found",
                    content = @Content(schema = @Schema(implementation = TenantResponse.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tenant not found",
                    content = @Content(schema = @Schema(implementation = com.k12.tenant.infrastructure.rest.dto.ErrorResponse.class)))
    })
    public Response getTenant(
            @Parameter(description = "Tenant ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000") @PathParam("id") String id) {

        TenantId tenantId = new TenantId(id);
        var result = tenantService.getTenant(tenantId);
        return result.fold(
                success -> Response.ok().entity(TenantResponse.from(success)).build(), ErrorResponseMapper::toResponse);
    }

    @POST
    @Path("/{id}/activate")
    @Operation(
            summary = "Activate a tenant",
            description = "Activates a tenant with the given ID, changing its status to ACTIVE")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tenant activated successfully",
                    content = @Content(schema = @Schema(implementation = TenantResource.TenantEventDTO.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tenant not found",
                    content = @Content(schema = @Schema(implementation = com.k12.tenant.infrastructure.rest.dto.ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Tenant cannot be activated (invalid state transition)",
                    content = @Content(schema = @Schema(implementation = com.k12.tenant.infrastructure.rest.dto.ErrorResponse.class)))
    })
    public Response activateTenant(
            @Parameter(description = "Tenant ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000") @PathParam("id") String id) {

        TenantId tenantId = new TenantId(id);
        var result = tenantService.activateTenant(tenantId);
        return result.fold(success -> Response.ok().entity(toDTO(success)).build(), ErrorResponseMapper::toResponse);
    }

    @PUT
    @Path("/{id}/suspend")
    @Operation(
            summary = "Suspend a tenant",
            description = "Suspends a tenant with the given ID, changing its status to SUSPENDED")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tenant suspended successfully",
                    content = @Content(schema = @Schema(implementation = TenantResource.TenantEventDTO.class))),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tenant not found",
                    content = @Content(schema = @Schema(implementation = com.k12.tenant.infrastructure.rest.dto.ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Tenant cannot be suspended (invalid state transition)",
                    content = @Content(schema = @Schema(implementation = com.k12.tenant.infrastructure.rest.dto.ErrorResponse.class)))
    })
    public Response suspendTenant(
            @Parameter(description = "Tenant ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000") @PathParam("id") String id) {

        TenantId tenantId = new TenantId(id);
        var result = tenantService.suspendTenant(tenantId);
        return result.fold(this::ok, ErrorResponseMapper::toResponse);
    }

    @DELETE
    @Path("/{id}")
    @Operation(
            summary = "Delete a tenant",
            description = "Deletes a tenant with the given ID")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Tenant deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tenant not found",
                    content = @Content(schema = @Schema(implementation = com.k12.tenant.infrastructure.rest.dto.ErrorResponse.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Tenant cannot be deleted (invalid state transition)",
                    content = @Content(schema = @Schema(implementation = com.k12.tenant.infrastructure.rest.dto.ErrorResponse.class)))
    })
    public Response deleteTenant(
            @Parameter(description = "Tenant ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000") @PathParam("id") String id) {

        TenantId tenantId = new TenantId(id);
        var result = tenantService.deleteTenant(tenantId);
        return result.fold(this::noContent, ErrorResponseMapper::toResponse);
    }

    private Response created(TenantEvents event) {
        return Response.status(Response.Status.CREATED).entity(toDTO(event)).build();
    }

    private Response ok(TenantEvents event) {
        return Response.status(Response.Status.OK).entity(toDTO(event)).build();
    }

    private Response noContent(TenantEvents event) {
        return Response.noContent().build();
    }

    /**
     * Convert event to simple DTO for response.
     */
    private Object toDTO(TenantEvents event) {
        return switch (event) {
            case TenantEvents.TenantCreated e ->
                new TenantEventDTO(
                        "TenantCreated",
                        e.tenantId().value(),
                        e.name().value(),
                        e.subdomain().value(),
                        e.status().name());
            case TenantEvents.TenantActivated e ->
                new TenantEventDTO("TenantActivated", e.tenantId().value(), null, null, "ACTIVE");
            case TenantEvents.TenantSuspended e ->
                new TenantEventDTO("TenantSuspended", e.tenantId().value(), null, null, "SUSPENDED");
            case TenantEvents.TenantDeactivated e ->
                new TenantEventDTO("TenantDeactivated", e.tenantId().value(), null, null, "INACTIVE");
            case TenantEvents.TenantDeleted e ->
                new TenantEventDTO("TenantDeleted", e.tenantId().value(), null, null, "DELETED");
            case TenantEvents.TenantNameUpdated e ->
                new TenantEventDTO(
                        "TenantNameUpdated", e.tenantId().value(), e.newName().value(), null, null);
            case TenantEvents.TenantSubdomainUpdated e ->
                new TenantEventDTO(
                        "TenantSubdomainUpdated",
                        e.tenantId().value(),
                        null,
                        e.newSubdomain().value(),
                        null);
        };
    }

    /**
     * Simple DTO for event responses.
     */
    @Schema(description = "Tenant event response representing a state change")
    record TenantEventDTO(
            @Schema(description = "Type of event that occurred", example = "TenantCreated") String eventType,
            @Schema(description = "Unique identifier of the tenant", example = "123e4567-e89b-12d3-a456-426614174000") String tenantId,
            @Schema(description = "Name of the tenant", example = "Acme Corporation") String name,
            @Schema(description = "Subdomain of the tenant", example = "acme") String subdomain,
            @Schema(description = "Current status of the tenant", example = "ACTIVE") String status) {}
}
