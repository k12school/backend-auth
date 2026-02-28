package com.k12.user.infrastructure.rest.mapper;

import com.k12.user.domain.models.error.UserError;
import com.k12.user.infrastructure.rest.dto.ErrorResponseDTO;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Maps domain errors to HTTP responses using result.fold() pattern.
 */
public final class UserErrorResponseMapper {

    private UserErrorResponseMapper() {}

    /**
     * Converts a UserError to a JAX-RS Response.
     *
     * @param error The domain error
     * @return HTTP Response with appropriate status code
     */
    public static Response toResponse(UserError error) {
        int statusCode = classifyError(error);
        return Response.status(statusCode).entity(ErrorResponseDTO.from(error)).build();
    }

    private static int classifyError(UserError error) {
        return switch (error) {
            // 400 - Bad Request (validation errors)
            case UserError.ValidationError e -> Status.BAD_REQUEST.getStatusCode();
            case UserError.EmailError e -> Status.BAD_REQUEST.getStatusCode();
            case UserError.PasswordError e -> Status.BAD_REQUEST.getStatusCode();
            case UserError.NameError e -> Status.BAD_REQUEST.getStatusCode();

            // 409 - Conflict (already exists)
            case UserError.ConflictError e -> Status.CONFLICT.getStatusCode();

            // 404 - Not Found
            case UserError.NotFoundError e -> Status.NOT_FOUND.getStatusCode();

            // 422 - Unprocessable Entity (business rule violations)
            case UserError.RoleError e -> {
                if (e == UserError.RoleError.SUPER_ADMIN_ROLE_IMMUTABLE) {
                    yield Status.FORBIDDEN.getStatusCode();
                }
                yield 422; // UNPROCESSABLE_ENTITY
            }
            case UserError.UserStatusError e -> 422; // UNPROCESSABLE_ENTITY

            // 500 - Internal Server Error (persistence errors)
            case UserError.PersistenceError e -> Status.INTERNAL_SERVER_ERROR.getStatusCode();
        };
    }
}
