package com.k12.user.application.dto;

import com.k12.user.domain.models.UserRole;
import java.util.Set;

public record LoginResponse(String token, UserInfo user) {
    public record UserInfo(String id, String email, String name, Set<UserRole> roles) {}

    public static LoginResponse from(String token, com.k12.user.domain.models.User user) {
        return new LoginResponse(
                token,
                new LoginResponse.UserInfo(
                        user.userId().value().toString(),
                        user.emailAddress().value(),
                        user.name().value(),
                        user.userRole()));
    }
}
