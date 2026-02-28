package com.k12.tenant.infrastructure.rest.mapper;

import com.k12.tenant.infrastructure.rest.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps Bean Validation exceptions to our standard ErrorResponse format.
 * This ensures validation errors from @Valid annotations match our custom error response structure.
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        // Get the first violation and determine the error code
        String errorCode = "VALIDATION_ERROR";
        String message = "Validation failed";

        if (!exception.getConstraintViolations().isEmpty()) {
            ConstraintViolation<?> violation =
                    exception.getConstraintViolations().iterator().next();
            String property = violation.getPropertyPath().toString();
            String propertyLower =
                    property.substring(property.lastIndexOf('.') + 1).toLowerCase();

            // Map property to specific error code
            errorCode = switch (propertyLower) {
                case "email" -> "INVALID_EMAIL";
                case "password" -> "INVALID_PASSWORD";
                case "name" -> "INVALID_NAME";
                case "permissions" -> "INVALID_PERMISSIONS";
                default -> "VALIDATION_ERROR";
            };

            message = violation.getMessage();
        }

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(errorCode, message, Response.Status.BAD_REQUEST.getStatusCode()))
                .build();
    }
}
