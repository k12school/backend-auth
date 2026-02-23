package com.k12.user.infrastructure.rest.mapper;

import com.k12.user.domain.error.AuthenticationError;
import com.k12.user.infrastructure.rest.dto.ErrorResponseDTO;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class AuthErrorResponseMapper {
    public static Response toResponse(AuthenticationError error) {
        Status status = determineStatus(error);
        return Response.status(status).entity(ErrorResponseDTO.from(error)).build();
    }

    private static Status determineStatus(AuthenticationError error) {
        if (error instanceof AuthenticationError.UserNotFound
                || error instanceof AuthenticationError.InvalidCredentials) {
            return Status.UNAUTHORIZED;
        }
        if (error instanceof AuthenticationError.UserSuspended || error instanceof AuthenticationError.UserInactive) {
            return Status.FORBIDDEN;
        }
        return Status.BAD_REQUEST;
    }
}
