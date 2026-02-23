package com.k12.tenant.domain.models;

import static com.k12.tenant.domain.models.error.TenantError.NameError.NAME_SAME_AS_CURRENT;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_SAME_AS_CURRENT;
import static com.k12.tenant.domain.models.error.TenantError.TenantStatusError.CANNOT_ACTIVATE_INACTIVE;
import static com.k12.tenant.domain.models.error.TenantError.TenantStatusError.CANNOT_DELETE_ACTIVE;
import static com.k12.tenant.domain.models.error.TenantError.TenantStatusError.TENANT_ALREADY_ACTIVE;
import static com.k12.tenant.domain.models.error.TenantError.TenantStatusError.TENANT_ALREADY_SUSPENDED;
import static org.assertj.core.api.Assertions.assertThat;

import com.k12.common.domain.model.TenantId;
import com.k12.tenant.domain.models.commands.TenantCommands;
import com.k12.tenant.domain.models.events.TenantEvents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Tenant Domain Model Tests")
class TenantTest {

    @Nested
    @DisplayName("Suspend Tenant")
    class SuspendTenantTests {

        @Test
        @DisplayName("Should suspend active tenant successfully")
        void shouldSuspendActiveTenant() {
            // Given
            var tenant = createActiveTenant();
            var command = new TenantCommands.SuspendTenant(tenant.id());

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isInstanceOfSatisfying(TenantEvents.TenantSuspended.class, event -> {
                assertThat(event.tenantId()).isEqualTo(tenant.id());
            });
        }

        @Test
        @DisplayName("Should fail when suspending already suspended tenant")
        void shouldFailWhenSuspendingSuspendedTenant() {
            // Given
            var tenant = createSuspendedTenant();
            var command = new TenantCommands.SuspendTenant(tenant.id());

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(TENANT_ALREADY_SUSPENDED);
        }
    }

    @Nested
    @DisplayName("Activate Tenant")
    class ActivateTenantTests {

        @Test
        @DisplayName("Should activate suspended tenant successfully")
        void shouldActivateSuspendedTenant() {
            // Given
            var tenant = createSuspendedTenant();
            var command = new TenantCommands.ActivateTenant(tenant.id());

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isInstanceOfSatisfying(TenantEvents.TenantActivated.class, event -> {
                assertThat(event.tenantId()).isEqualTo(tenant.id());
            });
        }

        @Test
        @DisplayName("Should fail when activating already active tenant")
        void shouldFailWhenActivatingActiveTenant() {
            // Given
            var tenant = createActiveTenant();
            var command = new TenantCommands.ActivateTenant(tenant.id());

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(TENANT_ALREADY_ACTIVE);
        }

        @Test
        @DisplayName("Should fail when activating inactive tenant")
        void shouldFailWhenActivatingInactiveTenant() {
            // Given
            var tenant = createInactiveTenant();
            var command = new TenantCommands.ActivateTenant(tenant.id());

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(CANNOT_ACTIVATE_INACTIVE);
        }
    }

    @Nested
    @DisplayName("Delete Tenant")
    class DeleteTenantTests {

        @Test
        @DisplayName("Should delete suspended tenant successfully")
        void shouldDeleteSuspendedTenant() {
            // Given
            var tenant = createSuspendedTenant();
            var command = new TenantCommands.DeleteTenant(tenant.id());

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isInstanceOfSatisfying(TenantEvents.TenantDeleted.class, event -> {
                assertThat(event.tenantId()).isEqualTo(tenant.id());
            });
        }

        @Test
        @DisplayName("Should delete inactive tenant successfully")
        void shouldDeleteInactiveTenant() {
            // Given
            var tenant = createInactiveTenant();
            var command = new TenantCommands.DeleteTenant(tenant.id());

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isInstanceOfSatisfying(TenantEvents.TenantDeleted.class, event -> {
                assertThat(event.tenantId()).isEqualTo(tenant.id());
            });
        }

        @Test
        @DisplayName("Should fail when deleting active tenant")
        void shouldFailWhenDeletingActiveTenant() {
            // Given
            var tenant = createActiveTenant();
            var command = new TenantCommands.DeleteTenant(tenant.id());

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(CANNOT_DELETE_ACTIVE);
        }
    }

    @Nested
    @DisplayName("Update Name")
    class UpdateNameTests {

        @Test
        @DisplayName("Should update name successfully")
        void shouldUpdateNameSuccessfully() {
            // Given
            var tenant = createActiveTenant();
            var newName = TenantName.of("New Tenant Name").get();
            var command = new TenantCommands.UpdateName(tenant.id(), newName);

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isInstanceOfSatisfying(TenantEvents.TenantNameUpdated.class, event -> {
                assertThat(event.tenantId()).isEqualTo(tenant.id());
                assertThat(event.newName().value()).isEqualTo("New Tenant Name");
            });
        }

        @Test
        @DisplayName("Should fail when name is same as current")
        void shouldFailWhenNameIsSameAsCurrent() {
            // Given
            var tenant = createActiveTenant();
            var command = new TenantCommands.UpdateName(tenant.id(), tenant.name());

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(NAME_SAME_AS_CURRENT);
        }
    }

    @Nested
    @DisplayName("Update Subdomain")
    class UpdateSubdomainTests {

        @Test
        @DisplayName("Should update subdomain successfully")
        void shouldUpdateSubdomainSuccessfully() {
            // Given
            var tenant = createActiveTenant();
            var newSubdomain = Subdomain.of("newsubdomain").get();
            var command = new TenantCommands.UpdateSubdomain(tenant.id(), newSubdomain);

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isInstanceOfSatisfying(TenantEvents.TenantSubdomainUpdated.class, event -> {
                assertThat(event.tenantId()).isEqualTo(tenant.id());
                assertThat(event.newSubdomain().value()).isEqualTo("newsubdomain");
            });
        }

        @Test
        @DisplayName("Should fail when subdomain is same as current")
        void shouldFailWhenSubdomainIsSameAsCurrent() {
            // Given
            var tenant = createActiveTenant();
            var command = new TenantCommands.UpdateSubdomain(tenant.id(), tenant.subdomain());

            // When
            var result = tenant.process(command);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(SUBDOMAIN_SAME_AS_CURRENT);
        }
    }

    // Helper methods

    private Tenant createActiveTenant() {
        return new Tenant(
                TenantId.generate(),
                TenantName.of("Test Tenant").get(),
                Subdomain.of("testtenant").get(),
                TenantStatus.ACTIVE,
                1L);
    }

    private Tenant createSuspendedTenant() {
        return new Tenant(
                TenantId.generate(),
                TenantName.of("Test Tenant").get(),
                Subdomain.of("testtenant").get(),
                TenantStatus.SUSPENDED,
                1L);
    }

    private Tenant createInactiveTenant() {
        return new Tenant(
                TenantId.generate(),
                TenantName.of("Test Tenant").get(),
                Subdomain.of("testtenant").get(),
                TenantStatus.INACTIVE,
                1L);
    }
}
