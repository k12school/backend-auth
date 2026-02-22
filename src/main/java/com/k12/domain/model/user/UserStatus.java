package com.k12.domain.model.user;

/**
 * User account status.
 */
public enum UserStatus {
    /**
     * User can login and access the system.
     */
    ACTIVE,

    /**
     * User cannot login.
     */
    SUSPENDED
}
