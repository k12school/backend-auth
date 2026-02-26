package com.k12.tenant.application.service;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.error.TenantAdminError;
import com.k12.tenant.infrastructure.rest.dto.CreateTenantAdminRequest;
import com.k12.tenant.infrastructure.rest.dto.TenantAdminResponse;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Application service for creating tenant administrators.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class TenantAdminService {

    /**
     * Creates a new tenant administrator for the specified tenant.
     *
     * @param tenantId the tenant ID to associate the admin with
     * @param request the create tenant admin request
     * @return Result containing TenantAdminResponse on success, or TenantAdminError on failure
     */
    public Result<TenantAdminResponse, TenantAdminError> createTenantAdmin(
            TenantId tenantId,
            CreateTenantAdminRequest request) {
        // Implementation in Task 7
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
