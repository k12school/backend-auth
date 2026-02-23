package com.k12.tenant.domain.models;

import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.EMPTY;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_CANNOT_START_OR_END_WITH_HYPHEN;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_HAS_CONSECUTIVE_HYPHENS;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_INVALID_FORMAT;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_TOO_LONG;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_TOO_SHORT;

import com.k12.common.domain.model.Result;
import com.k12.tenant.domain.models.error.TenantError;

/**
 * Value object representing a tenant's subdomain.
 */
public record Subdomain(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 63;
    private static final String VALID_SUBDOMAIN_PATTERN = "^[a-z0-9]([a-z0-9-]*[a-z0-9])?$";

    /**
     * Factory method to create a validated Subdomain using ROP pattern.
     *
     * @param value The subdomain value
     * @return Result containing Subdomain on success, or TenantError on failure
     */
    public static Result<Subdomain, TenantError> of(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Result.failure(EMPTY);
        }
        String normalized = value.toLowerCase().trim();

        if (normalized.length() < MIN_LENGTH) {
            return Result.failure(SUBDOMAIN_TOO_SHORT);
        }
        if (normalized.length() > MAX_LENGTH) {
            return Result.failure(SUBDOMAIN_TOO_LONG);
        }
        if (!normalized.matches(VALID_SUBDOMAIN_PATTERN)) {
            return Result.failure(SUBDOMAIN_INVALID_FORMAT);
        }
        if (normalized.contains("--")) {
            return Result.failure(SUBDOMAIN_HAS_CONSECUTIVE_HYPHENS);
        }
        if (normalized.startsWith("-") || normalized.endsWith("-")) {
            return Result.failure(SUBDOMAIN_CANNOT_START_OR_END_WITH_HYPHEN);
        }

        return Result.success(new Subdomain(normalized));
    }
}
