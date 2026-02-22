package com.k12.domain.model.tenant;

/**
 * Tenant status enum.
 */
public enum TenantStatus {
    /**
     * Tenant is active and can operate normally.
     */
    ACTIVE,

    /**
     * Tenant is suspended and cannot perform operations.
     */
    SUSPENDED,

    /**
     * Tenant is inactive (soft deleted).
     */
    INACTIVE
}
