package com.k12.user.infrastructure.rest.dto;

import com.k12.user.domain.models.UserRole;
import java.util.Set;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Login response with JWT token and user info")
public record LoginResponseDTO(
        @Schema(description = "JWT authentication token") String token,
        @Schema(description = "User information") UserInfoDTO user) {
    @Schema(description = "User information")
    public record UserInfoDTO(String id, String email, String name, Set<UserRole> roles) {}

    public static LoginResponseDTO from(String token, com.k12.user.application.dto.LoginResponse.UserInfo userInfo) {
        return new LoginResponseDTO(
                token, new UserInfoDTO(userInfo.id(), userInfo.email(), userInfo.name(), userInfo.roles()));
    }
}
