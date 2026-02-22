package com.k12.user.domain.models.specialization.parent;

import com.k12.common.domain.model.UserId;

/**
 * Value object representing a unique identifier for a Parent.
 * ParentId always contains the UserId - they are always the same value.
 */
public record ParentId(UserId userId) {

    public ParentId {
        if (userId == null) {
            throw new IllegalArgumentException("ParentId cannot be null");
        }
    }

    public static ParentId of(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("ParentId cannot be null");
        }
        return new ParentId(userId);
    }
}
