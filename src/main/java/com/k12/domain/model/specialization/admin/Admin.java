package com.k12.domain.model.specialization.admin;

import com.k12.domain.model.specialization.admin.valueobjects.Permission;
import java.time.Instant;
import java.util.Set;

/**
 * Aggregate root representing an Admin in the system.
 * TODO: Implement full admin domain model with event sourcing
 */
public record Admin(AdminId adminId, Set<Permission> permissions, Instant createdAt) {

    public Admin {
        if (adminId == null) {
            throw new IllegalArgumentException("AdminId cannot be null");
        }
        if (permissions == null) {
            permissions = Set.of();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
