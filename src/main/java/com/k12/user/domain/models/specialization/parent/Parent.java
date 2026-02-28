package com.k12.user.domain.models.specialization.parent;

import java.time.Instant;

/**
 * Aggregate root representing a Parent in the system.
 * <p>
 * A Parent represents a user who is a parent/guardian of students in the system.
 * This aggregate contains parent-specific information such as contact details
 * and emergency contact information.
 * </p>
 */
public record Parent(
        ParentId parentId, String phoneNumber, String address, String emergencyContact, Instant createdAt) {

    /**
     * Creates a new Parent instance with validation.
     * <p>
     * The parentId is required and must not be null.
     * All other fields (phoneNumber, address, emergencyContact) are optional.
     * The createdAt timestamp defaults to the current instant if not provided.
     * </p>
     *
     * @param parentId the unique identifier for this parent; must not be null
     * @param phoneNumber the phone number (optional, may be null)
     * @param address the home address (optional, may be null)
     * @param emergencyContact the emergency contact information (optional, may be null)
     * @param createdAt the timestamp when this parent was created; defaults to now if null
     * @throws IllegalArgumentException if parentId is null
     */
    public Parent {
        if (parentId == null) {
            throw new IllegalArgumentException("ParentId cannot be null");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
