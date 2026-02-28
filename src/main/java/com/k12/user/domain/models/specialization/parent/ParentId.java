package com.k12.user.domain.models.specialization.parent;

import com.k12.common.domain.model.UserId;
import java.util.UUID;

public record ParentId(UserId value) {
    public ParentId {
        if (value == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
    }

    public static ParentId of(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        return new ParentId(userId);
    }

    public UUID id() {
        return value.value();
    }
}
