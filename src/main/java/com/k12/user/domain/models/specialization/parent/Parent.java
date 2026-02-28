package com.k12.user.domain.models.specialization.parent;

import java.time.Instant;

public record Parent(
        ParentId parentId, String phoneNumber, String address, String emergencyContact, Instant createdAt) {

    public Parent {
        if (parentId == null) {
            throw new IllegalArgumentException("ParentId cannot be null");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
