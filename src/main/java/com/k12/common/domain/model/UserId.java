package com.k12.domain.model.common;

import java.util.UUID;

public record UserId(UUID value) {

    public static UserId of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        try {
            return new UserId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + value, e);
        }
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }
}
