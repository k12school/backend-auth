package com.k12.tenant.domain.models;

import static com.k12.tenant.domain.models.error.TenantError.NameError.EMPTY;
import static com.k12.tenant.domain.models.error.TenantError.NameError.NAME_TOO_LONG;
import static com.k12.tenant.domain.models.error.TenantError.NameError.NAME_TOO_SHORT;

import com.k12.common.domain.model.Result;
import com.k12.tenant.domain.models.error.TenantError;

/**
 * Value object representing a tenant's name.
 */
public record TenantName(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;

    /**
     * Factory method to create a validated TenantName using ROP pattern.
     *
     * @param value The name value
     * @return Result containing TenantName on success, or TenantError on failure
     */
    public static Result<TenantName, TenantError> of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Result.failure(EMPTY);
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH) {
            return Result.failure(NAME_TOO_SHORT);
        }
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(NAME_TOO_LONG);
        }
        return Result.success(new TenantName(trimmed));
    }
}
