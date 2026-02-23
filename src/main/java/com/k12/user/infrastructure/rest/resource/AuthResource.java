package com.k12.user.infrastructure.rest.resource;

import com.k12.user.application.AuthenticationApplicationService;
import com.k12.user.application.dto.LoginRequest;
import com.k12.user.infrastructure.rest.dto.ErrorResponseDTO;
import com.k12.user.infrastructure.rest.dto.LoginRequestDTO;
import com.k12.user.infrastructure.rest.dto.LoginResponseDTO;
import com.k12.user.infrastructure.rest.mapper.AuthErrorResponseMapper;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthResource {
    private final AuthenticationApplicationService authenticationService;

    @POST
    @Path("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user with email and password, returns JWT token")
    @APIResponses({
        @APIResponse(
                responseCode = "200",
                description = "Authentication successful",
                content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @APIResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @APIResponse(
                responseCode = "400",
                description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public Response login(
            @Valid @RequestBody(description = "Login credentials", required = true) LoginRequestDTO request) {
        LoginRequest loginRequest = new LoginRequest(request.email(), request.password());
        var result = authenticationService.login(loginRequest);
        return result.fold(
                success -> Response.ok(LoginResponseDTO.from(success.token(), success.user()))
                        .build(),
                AuthErrorResponseMapper::toResponse);
    }
}
