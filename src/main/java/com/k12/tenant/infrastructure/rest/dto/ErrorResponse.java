package com.k12.tenant.infrastructure.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Error response DTO.
 */
@Schema(description = "Response payload containing error details")
public record ErrorResponse(
        @Schema(description = "Error type/category", examples = "ValidationError")
        String error,

        @Schema(description = "Detailed error message", examples = "Name is required")
        String message,

        @Schema(description = "HTTP status code", examples = "400")
        int statusCode) {

    public static ErrorResponse of(String error, String message, int statusCode) {
        return new ErrorResponse(error, message, statusCode);
    }
}
