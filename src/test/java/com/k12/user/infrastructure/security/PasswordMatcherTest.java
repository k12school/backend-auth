package com.k12.user.infrastructure.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordMatcherTest {

    @Test
    void shouldVerifyCorrectPassword() {
        // This is a valid BCrypt hash for "password123"
        String hash = "$2b$12$xi/KRfXYlJ2E1DqDqTjYl.el2Vla6Lq4SUmb1CdGxEeZUCscpa8Ty";
        String password = "password123";
        assertTrue(PasswordMatcher.verify(password, hash));
    }

    @Test
    void shouldRejectIncorrectPassword() {
        // This is a valid BCrypt hash for "password123", not "wrongpassword"
        String hash = "$2b$12$xi/KRfXYlJ2E1DqDqTjYl.el2Vla6Lq4SUmb1CdGxEeZUCscpa8Ty";
        String password = "wrongpassword";
        assertFalse(PasswordMatcher.verify(password, hash));
    }

    @Test
    void shouldHandleNullHash() {
        String password = "password123";
        assertFalse(PasswordMatcher.verify(password, null));
    }
}
