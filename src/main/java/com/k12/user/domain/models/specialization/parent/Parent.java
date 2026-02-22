package com.k12.user.domain.model.specialization.parent;

import java.time.Instant;

/**
 * Aggregate root representing a Parent in the system.
 * TODO: Implement full parent domain model with event sourcing
 */
public record Parent(ParentId parentId, Instant createdAt) {

    public Parent {
        if (parentId == null) {
            throw new IllegalArgumentException("ParentId cannot be null");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
