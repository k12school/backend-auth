package com.k12.user.domain.models.specialization.parent;

import com.k12.common.domain.model.UserId;
import java.time.Instant;

public final class ParentFactory {

    private ParentFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Parent create(UserId userId, String phoneNumber, String address, String emergencyContact) {

        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }

        return new Parent(ParentId.of(userId), phoneNumber, address, emergencyContact, Instant.now());
    }
}
