package com.k12.tenant.domain.models;

import static org.assertj.core.api.Assertions.assertThat;

import com.k12.tenant.domain.models.commands.TenantCommands;
import com.k12.tenant.domain.models.events.TenantEvents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TenantFactory Tests")
class TenantFactoryTest {

    @Nested
    @DisplayName("handle CreateTenant command")
    class HandleCreateTenantTests {

        @Test
        @DisplayName("Should create tenant successfully with valid data")
        void shouldCreateTenantSuccessfullyWithValidData() {
            // Given
            var command = new TenantCommands.CreateTenant("My School", "myschool");

            // When
            var result = TenantFactory.handle(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isInstanceOfSatisfying(TenantEvents.TenantCreated.class, event -> {
                assertThat(event.tenantId()).isNotNull();
                assertThat(event.name().value()).isEqualTo("My School");
                assertThat(event.subdomain().value()).isEqualTo("myschool");
                assertThat(event.status()).isEqualTo(TenantStatus.ACTIVE);
                assertThat(event.version()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("Should fail with invalid name")
        void shouldFailWithInvalidName() {
            // Given
            var command = new TenantCommands.CreateTenant("", "validsubdomain");

            // When
            var result = TenantFactory.handle(command);

            // Then
            assertThat(result.isFailure()).isTrue();
        }

        @Test
        @DisplayName("Should fail with invalid subdomain")
        void shouldFailWithInvalidSubdomain() {
            // Given
            var command = new TenantCommands.CreateTenant("Valid Name", "invalid subdomain");

            // When
            var result = TenantFactory.handle(command);

            // Then
            assertThat(result.isFailure()).isTrue();
        }
    }

    @Nested
    @DisplayName("create with string values")
    class CreateWithStringValuesTests {

        @Test
        @DisplayName("Should create tenant with valid strings")
        void shouldCreateTenantWithValidStrings() {
            // When
            var result = TenantFactory.create("My School", "myschool");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isInstanceOf(TenantEvents.TenantCreated.class);
        }
    }

    @Nested
    @DisplayName("create with value objects")
    class CreateWithValueObjectsTests {

        @Test
        @DisplayName("Should create tenant with validated value objects")
        void shouldCreateTenantWithValidatedValueObjects() {
            // Given
            var name = TenantName.of("My School").get();
            var subdomain = Subdomain.of("myschool").get();

            // When
            var result = TenantFactory.create(name, subdomain);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get()).isInstanceOfSatisfying(TenantEvents.TenantCreated.class, event -> {
                assertThat(event.name()).isEqualTo(name);
                assertThat(event.subdomain()).isEqualTo(subdomain);
            });
        }
    }
}
