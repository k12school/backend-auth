package com.k12.tenant.domain.models;

import static com.k12.tenant.domain.models.error.TenantError.NameError.EMPTY;
import static com.k12.tenant.domain.models.error.TenantError.NameError.NAME_TOO_LONG;
import static com.k12.tenant.domain.models.error.TenantError.NameError.NAME_TOO_SHORT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TenantName Value Object Tests")
class TenantNameTest {

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should create valid tenant name")
        void shouldCreateValidTenantName() {
            // When
            var result = TenantName.of("Valid School Name");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get().value()).isEqualTo("Valid School Name");
        }

        @Test
        @DisplayName("Should trim whitespace")
        void shouldTrimWhitespace() {
            // When
            var result = TenantName.of("  Valid School Name  ");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get().value()).isEqualTo("Valid School Name");
        }

        @Test
        @DisplayName("Should fail when name is null")
        void shouldFailWhenNameIsNull() {
            // When
            var result = TenantName.of((String) null);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(EMPTY);
        }

        @Test
        @DisplayName("Should fail when name is empty")
        void shouldFailWhenNameIsEmpty() {
            // When
            var result = TenantName.of("");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(EMPTY);
        }

        @Test
        @DisplayName("Should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            // When
            var result = TenantName.of("   ");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(EMPTY);
        }

        @Test
        @DisplayName("Should fail when name is too short")
        void shouldFailWhenNameIsTooShort() {
            // When
            var result = TenantName.of("A");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(NAME_TOO_SHORT);
        }

        @Test
        @DisplayName("Should fail when name is too long")
        void shouldFailWhenNameIsTooLong() {
            // Given
            String longName = "A".repeat(101);

            // When
            var result = TenantName.of(longName);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(NAME_TOO_LONG);
        }
    }

    @Nested
    @DisplayName("Equality")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when values are same")
        void shouldBeEqualWhenValuesAreSame() {
            // Given
            var name1 = TenantName.of("Test School").get();
            var name2 = TenantName.of("Test School").get();

            // Then
            assertThat(name1).isEqualTo(name2);
            assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when values are different")
        void shouldNotBeEqualWhenValuesAreDifferent() {
            // Given
            var name1 = TenantName.of("School One").get();
            var name2 = TenantName.of("School Two").get();

            // Then
            assertThat(name1).isNotEqualTo(name2);
        }
    }
}
