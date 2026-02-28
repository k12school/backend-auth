package com.k12.user.domain.models.specialization.teacher;

import com.k12.common.domain.model.UserId;
import java.util.UUID;

/**
 * Value object representing a unique identifier for a Teacher.
 * TeacherId wraps a UserId - they are always the same value.
 */
public record TeacherId(UserId value) {

    public TeacherId {
        if (value == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
    }

    public static TeacherId of(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        return new TeacherId(userId);
    }

    public UUID id() {
        return value.value();
    }
}
