package com.k12.user.domain.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class UserTest {

    @Test
    public void testUserCreationWithTenant() {
        TenantId tenantId = TenantId.generate();
        User user = new User(
                UserId.generate(),
                EmailAddress.of("test@example.com"),
                PasswordHash.of("$2a$12$UJ3TGU9.Po1qYDEBuNgeout1LgBuxDLgfxebbyoAPmewn5Evj0Q.6"),
                Set.of(UserRole.STUDENT),
                UserStatus.ACTIVE,
                UserName.of("Test User"),
                tenantId);

        assertEquals(tenantId, user.tenantId());
    }
}
