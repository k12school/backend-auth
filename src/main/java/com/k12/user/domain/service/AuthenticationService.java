package com.k12.user.domain.service;

import com.k12.common.domain.model.Result;
import com.k12.user.domain.error.AuthenticationError;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.domain.ports.out.UserRepository;
import com.k12.user.infrastructure.security.PasswordMatcher;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;

    public Result<User, AuthenticationError> authenticate(String email, String password) {
        return userRepository
                .findByEmailAddress(email)
                .map(user -> validateUser(user, password))
                .orElse(Result.failure(new AuthenticationError.UserNotFound()));
    }

    private Result<User, AuthenticationError> validateUser(User user, String password) {
        if (user.status() == UserStatus.SUSPENDED) {
            return Result.failure(new AuthenticationError.UserSuspended());
        }
        if (user.status() != UserStatus.ACTIVE) {
            return Result.failure(new AuthenticationError.UserInactive());
        }
        if (!PasswordMatcher.verify(password, user.passwordHash().value())) {
            return Result.failure(new AuthenticationError.InvalidCredentials());
        }
        return Result.success(user);
    }
}
