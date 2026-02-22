package com.k12.user.domain.model.specialization.teacher;

import com.k12.common.domain.model.UserId;

/**
 * Value object representing a unique identifier for a Teacher.
 * TeacherId always contains the UserId - they are always the same value.
 */
public record TeacherId(UserId userId) {

    public TeacherId {
        if (userId == null) {
            throw new IllegalArgumentException("TeacherId cannot be null");
        }
    }

    public static TeacherId of(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("TeacherId cannot be null");
        }
        return new TeacherId(userId);
    }
}
