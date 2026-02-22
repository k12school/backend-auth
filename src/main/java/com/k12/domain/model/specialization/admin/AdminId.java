package com.k12.domain.model.specialization.admin;

import com.k12.domain.model.common.UserId;

/**
 * Value object representing a unique identifier for an Admin.
 * AdminId always contains the UserId - they are always the same value.
 */
public record AdminId(UserId userId) {

    public AdminId {
        if (userId == null) {
            throw new IllegalArgumentException("AdminId cannot be null");
        }
    }

    public static AdminId of(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("AdminId cannot be null");
        }
        return new AdminId(userId);
    }
}
