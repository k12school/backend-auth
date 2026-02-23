package com.k12.tenant.domain.models;

import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.EMPTY;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_HAS_CONSECUTIVE_HYPHENS;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_INVALID_FORMAT;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_TOO_LONG;
import static com.k12.tenant.domain.models.error.TenantError.SubdomainError.SUBDOMAIN_TOO_SHORT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Subdomain Value Object Tests")
class SubdomainTest {

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should create valid subdomain")
        void shouldCreateValidSubdomain() {
            // When
            var result = Subdomain.of("valid-school");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get().value()).isEqualTo("valid-school");
        }

        @Test
        @DisplayName("Should normalize to lowercase")
        void shouldNormalizeToLowercase() {
            // When
            var result = Subdomain.of("Valid-School-123");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get().value()).isEqualTo("valid-school-123");
        }

        @Test
        @DisplayName("Should trim whitespace")
        void shouldTrimWhitespace() {
            // When
            var result = Subdomain.of("  valid-school  ");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get().value()).isEqualTo("valid-school");
        }

        @Test
        @DisplayName("Should fail when subdomain is null")
        void shouldFailWhenSubdomainIsNull() {
            // When
            var result = Subdomain.of((String) null);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(EMPTY);
        }

        @Test
        @DisplayName("Should fail when subdomain is empty")
        void shouldFailWhenSubdomainIsEmpty() {
            // When
            var result = Subdomain.of("");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(EMPTY);
        }

        @Test
        @DisplayName("Should fail when subdomain is too short")
        void shouldFailWhenSubdomainIsTooShort() {
            // When
            var result = Subdomain.of("ab");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(SUBDOMAIN_TOO_SHORT);
        }

        @Test
        @DisplayName("Should fail when subdomain is too long")
        void shouldFailWhenSubdomainIsTooLong() {
            // Given
            String longSubdomain = "a".repeat(64);

            // When
            var result = Subdomain.of(longSubdomain);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(SUBDOMAIN_TOO_LONG);
        }

        @Test
        @DisplayName("Should normalize uppercase to lowercase")
        void shouldNormalizeUppercaseToLowercase() {
            // When
            var result = Subdomain.of("Valid-School-123");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.get().value()).isEqualTo("valid-school-123");
        }

        @Test
        @DisplayName("Should fail when subdomain starts with hyphen")
        void shouldFailWhenSubdomainStartsWithHyphen() {
            // When
            var result = Subdomain.of("-invalid");

            // Then
            assertThat(result.isFailure()).isTrue();
        }

        @Test
        @DisplayName("Should fail when subdomain ends with hyphen")
        void shouldFailWhenSubdomainEndsWithHyphen() {
            // When
            var result = Subdomain.of("invalid-");

            // Then
            assertThat(result.isFailure()).isTrue();
        }

        @Test
        @DisplayName("Should fail when subdomain has consecutive hyphens")
        void shouldFailWhenSubdomainHasConsecutiveHyphens() {
            // When
            var result = Subdomain.of("invalid--school");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(SUBDOMAIN_HAS_CONSECUTIVE_HYPHENS);
        }

        @Test
        @DisplayName("Should fail when subdomain has special characters")
        void shouldFailWhenSubdomainHasSpecialCharacters() {
            // When
            var result = Subdomain.of("invalid_school");

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo(SUBDOMAIN_INVALID_FORMAT);
        }
    }

    @Nested
    @DisplayName("Equality")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when values are same")
        void shouldBeEqualWhenValuesAreSame() {
            // Given
            var subdomain1 = Subdomain.of("test-school").get();
            var subdomain2 = Subdomain.of("test-school").get();

            // Then
            assertThat(subdomain1).isEqualTo(subdomain2);
            assertThat(subdomain1.hashCode()).isEqualTo(subdomain2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when values are different")
        void shouldNotBeEqualWhenValuesAreDifferent() {
            // Given
            var subdomain1 = Subdomain.of("school-one").get();
            var subdomain2 = Subdomain.of("school-two").get();

            // Then
            assertThat(subdomain1).isNotEqualTo(subdomain2);
        }
    }
}
