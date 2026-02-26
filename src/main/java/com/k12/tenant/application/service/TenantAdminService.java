package com.k12.tenant.application.service;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.error.TenantAdminError;
import com.k12.tenant.infrastructure.rest.dto.CreateTenantAdminRequest;
import com.k12.tenant.infrastructure.rest.dto.TenantAdminResponse;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public interface TenantAdminService {

    Result<TenantAdminResponse, TenantAdminError> createTenantAdmin(
            TenantId tenantId, CreateTenantAdminRequest request);
}
