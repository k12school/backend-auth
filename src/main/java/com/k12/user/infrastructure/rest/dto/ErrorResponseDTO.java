package com.k12.user.infrastructure.rest.dto;

import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Error response for user operations")
public record ErrorResponseDTO(String error, String message, Instant timestamp) {
    public static ErrorResponseDTO from(com.k12.user.domain.error.AuthenticationError e) {
        return new ErrorResponseDTO(
                e.getClass().getSimpleName().toUpperCase().replace("AUTHENTICATIONERROR.", ""),
                e.message(),
                Instant.now());
    }

    public static ErrorResponseDTO from(com.k12.user.domain.models.error.UserError e) {
        String errorType =
                switch (e) {
                    case com.k12.user.domain.models.error.UserError.UserStatusError error -> "USER_STATUS_ERROR";
                    case com.k12.user.domain.models.error.UserError.EmailError error -> "EMAIL_ERROR";
                    case com.k12.user.domain.models.error.UserError.PasswordError error -> "PASSWORD_ERROR";
                    case com.k12.user.domain.models.error.UserError.RoleError error -> "ROLE_ERROR";
                    case com.k12.user.domain.models.error.UserError.NameError error -> "NAME_ERROR";
                    case com.k12.user.domain.models.error.UserError.ValidationError error -> "VALIDATION_ERROR";
                    case com.k12.user.domain.models.error.UserError.NotFoundError error -> "NOT_FOUND_ERROR";
                    case com.k12.user.domain.models.error.UserError.ConflictError error -> "CONFLICT_ERROR";
                    case com.k12.user.domain.models.error.UserError.PersistenceError error -> "PERSISTENCE_ERROR";
                };
        return new ErrorResponseDTO(errorType, e.message(), Instant.now());
    }
}
