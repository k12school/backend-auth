package com.k12.user.application;

import com.k12.common.domain.model.Result;
import com.k12.user.application.dto.LoginRequest;
import com.k12.user.application.dto.LoginResponse;
import com.k12.user.domain.error.AuthenticationError;
import com.k12.user.domain.models.User;
import com.k12.user.domain.service.AuthenticationService;
import com.k12.user.infrastructure.security.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class AuthenticationApplicationService {
    private final AuthenticationService authenticationService;

    public Result<LoginResponse, AuthenticationError> login(LoginRequest request) {
        return authenticationService
                .authenticate(request.email(), request.password())
                .map(user -> {
                    String tenantId = getTenantIdForUser(user);
                    String token = TokenService.generateToken(user, tenantId);
                    return LoginResponse.from(token, user);
                });
    }

    private String getTenantIdForUser(User user) {
        // TODO: Load from user's tenant association when implemented
        return "default-tenant";
    }
}
