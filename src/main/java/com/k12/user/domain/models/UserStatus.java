package com.k12.user.domain.models;

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
