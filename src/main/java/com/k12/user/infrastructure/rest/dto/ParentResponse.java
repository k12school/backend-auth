package com.k12.user.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Response DTO for Parent user details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParentResponse(
        String userId,
        String email,
        String name,
        String role,
        String tenantId,
        String phoneNumber,
        String address,
        String emergencyContact,
        String status,
        Instant createdAt) {

    public static ParentResponse from(
            com.k12.user.domain.models.specialization.parent.Parent parent, com.k12.user.domain.models.User user) {
        return new ParentResponse(
                user.userId().value().toString(),
                user.emailAddress().value(),
                user.name().value(),
                user.userRole().iterator().next().name(),
                user.tenantId().value(),
                parent.phoneNumber(),
                parent.address(),
                parent.emergencyContact(),
                user.status().name(),
                parent.createdAt());
    }
}
