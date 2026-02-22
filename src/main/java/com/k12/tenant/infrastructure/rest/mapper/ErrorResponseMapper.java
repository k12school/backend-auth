package com.k12.tenant.infrastructure.rest.mapper;

import static com.k12.tenant.infrastructure.rest.mapper.ErrorResponseMapper.ErrorType.BAD_REQUEST;
import static com.k12.tenant.infrastructure.rest.mapper.ErrorResponseMapper.ErrorType.CONFLICT;
import static com.k12.tenant.infrastructure.rest.mapper.ErrorResponseMapper.ErrorType.INTERNAL_SERVER_ERROR;
import static com.k12.tenant.infrastructure.rest.mapper.ErrorResponseMapper.ErrorType.UNPROCESSABLE_ENTITY;

import com.k12.tenant.domain.models.error.TenantError;
import com.k12.tenant.infrastructure.rest.dto.ErrorResponse;
import jakarta.ws.rs.core.Response;

/**
 * Maps domain errors to HTTP responses using result.fold() pattern.
 */
public final class ErrorResponseMapper {

    private ErrorResponseMapper() {}

    /**
     * Converts a TenantError to a JAX-RS Response.
     *
     * @param error The domain error
     * @return HTTP Response with appropriate status code
     */
    public static Response toResponse(TenantError error) {
        ErrorType type = classifyError(error);
        return Response.status(type.status)
                .entity(ErrorResponse.of(type.name, error.message(), type.status))
                .build();
    }

    private static ErrorType classifyError(TenantError error) {
        return switch (error) {
            // 400 - Bad Request (validation errors)
            case TenantError.ValidationError e -> BAD_REQUEST;
            case TenantError.NameError e -> BAD_REQUEST;
            case TenantError.SubdomainError e -> BAD_REQUEST;

            // 409 - Conflict (already exists, version conflicts)
            case TenantError.ConflictError e -> CONFLICT;
            case TenantError.ConcurrencyError e -> CONFLICT;

            // 422 - Unprocessable Entity (business rule violations)
            case TenantError.TenantStatusError e -> UNPROCESSABLE_ENTITY;

            // 500 - Internal Server Error (persistence errors)
            case TenantError.PersistenceError e -> INTERNAL_SERVER_ERROR;
        };
    }

    enum ErrorType {
        BAD_REQUEST("ValidationError", Response.Status.BAD_REQUEST.getStatusCode()),
        CONFLICT("ConflictError", Response.Status.CONFLICT.getStatusCode()),
        UNPROCESSABLE_ENTITY("DomainError", 422),
        INTERNAL_SERVER_ERROR("PersistenceError", Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        final String name;
        final int status;

        ErrorType(String name, int status) {
            this.name = name;
            this.status = status;
        }
    }
}
