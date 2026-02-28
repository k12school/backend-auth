package com.k12.user.infrastructure.rest.resource;

import com.k12.common.domain.model.UserId;
import com.k12.user.application.UserService;
import com.k12.user.infrastructure.rest.dto.ChangeRoleRequest;
import com.k12.user.infrastructure.rest.dto.CreateUserRequest;
import com.k12.user.infrastructure.rest.dto.UpdateUserRequest;
import com.k12.user.infrastructure.rest.mapper.UserErrorResponseMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"SUPER_ADMIN", "ADMIN"})
public class UserResource {

    @Inject
    UserService userService;

    @POST
    @Operation(summary = "Create a new user", description = "Creates a new user with role-based specialization data")
    public Response createUser(@Valid CreateUserRequest request) {
        // TODO: Extract tenantId from JWT
        var result = userService.createUser(request);
        return result.fold(
                response -> Response.status(Response.Status.CREATED)
                        .entity(response)
                        .build(),
                UserErrorResponseMapper::toResponse);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their unique identifier")
    public Response getUserById(@Parameter(description = "User ID") @PathParam("id") String id) {

        var userId = new UserId(java.util.UUID.fromString(id));
        var result = userService.getUserById(userId);
        return result.fold(response -> Response.ok().entity(response).build(), UserErrorResponseMapper::toResponse);
    }

    @GET
    @Operation(summary = "List users", description = "List users with optional filters")
    public Response listUsers(
            @Parameter(description = "Filter by role") @QueryParam("role") String role,
            @Parameter(description = "Filter by tenant") @QueryParam("tenantId") String tenantId,
            @Parameter(description = "Filter by status") @QueryParam("status") String status) {

        // TODO: Implement filtering logic
        return Response.ok().build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update user fields", description = "Updates user fields within their current role")
    public Response updateUserFields(
            @Parameter(description = "User ID") @PathParam("id") String id, @Valid UpdateUserRequest request) {

        // TODO: Implement
        return Response.ok().build();
    }

    @PUT
    @Path("/{id}/role")
    @Operation(summary = "Change user role", description = "Changes user role with new specialization data")
    public Response changeUserRole(
            @Parameter(description = "User ID") @PathParam("id") String id, @Valid ChangeRoleRequest request) {

        // TODO: Implement
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Soft delete user", description = "Soft deletes a user (sets status to DELETED)")
    public Response softDeleteUser(@Parameter(description = "User ID") @PathParam("id") String id) {

        var userId = new UserId(java.util.UUID.fromString(id));
        var result = userService.softDeleteUser(userId);
        return result.fold(
                success -> Response.status(Response.Status.NO_CONTENT).build(), UserErrorResponseMapper::toResponse);
    }
}
