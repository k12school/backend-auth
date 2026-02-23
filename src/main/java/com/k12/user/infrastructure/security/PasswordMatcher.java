package com.k12.user.infrastructure.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

public final class PasswordMatcher {
    private PasswordMatcher() {}

    public static boolean verify(String password, String hash) {
        if (hash == null || password == null) {
            return false;
        }
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
            return result.verified;
        } catch (Exception e) {
            return false;
        }
    }
}
