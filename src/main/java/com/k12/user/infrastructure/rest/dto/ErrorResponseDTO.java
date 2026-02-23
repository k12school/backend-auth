package com.k12.user.infrastructure.rest.dto;

import java.time.Instant;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Error response for authentication failures")
public record ErrorResponseDTO(String error, String message, Instant timestamp) {
    public static ErrorResponseDTO from(com.k12.user.domain.error.AuthenticationError e) {
        return new ErrorResponseDTO(
                e.getClass().getSimpleName().toUpperCase().replace("AUTHENTICATIONERROR.", ""),
                e.message(),
                Instant.now());
    }
}
