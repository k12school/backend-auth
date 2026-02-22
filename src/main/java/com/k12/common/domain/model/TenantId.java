package com.k12.common.domain.model;

import java.util.UUID;

/**
 * Tenant identifier value object.
 * Immutable identifier for multitenancy support.
 * Uses UUID format for uniqueness across tenants.
 */
public record TenantId(String value) {

    public TenantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or blank");
        }
    }

    /**
     * Generate a new unique tenant ID.
     *
     * @return new tenant ID
     */
    public static TenantId generate() {
        return new TenantId(UUID.randomUUID().toString());
    }

    /**
     * Create tenant ID from existing string value.
     *
     * @param value tenant ID string
     * @return tenant ID
     */
    public static TenantId of(String value) {
        return new TenantId(value);
    }
}
