package com.k12.infrastructure.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.ws.rs.core.Context;

/**
 * CDI producer for AuthContext.
 * Enables @Inject AuthContext throughout the application.
 */
public class AuthContextProducer {

    @Context
    private jakarta.ws.rs.container.ContainerRequestContext requestContext;

    @Produces
    @RequestScoped
    public AuthContext produceAuthContext() {
        if (requestContext == null) {
            return null;
        }
        return (AuthContext) requestContext.getProperty("auth.context");
    }
}
