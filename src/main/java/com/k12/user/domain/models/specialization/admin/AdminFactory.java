package com.k12.user.domain.models.specialization.admin;

import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import java.time.Instant;
import java.util.Set;

public final class AdminFactory {

    public static Admin create(AdminId adminId, Set<Permission> permissions) {
        if (adminId == null) {
            throw new IllegalArgumentException("AdminId cannot be null");
        }
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("Permissions cannot be null or empty");
        }

        return new Admin(adminId, permissions, Instant.now());
    }
}
