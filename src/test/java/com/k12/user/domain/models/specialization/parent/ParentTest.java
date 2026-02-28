package com.k12.user.domain.models.specialization.parent;

import static org.junit.jupiter.api.Assertions.*;

import com.k12.common.domain.model.UserId;
import org.junit.jupiter.api.Test;

class ParentTest {

    @Test
    void testParentCreation() {
        UserId userId = UserId.generate();
        Parent parent = ParentFactory.create(userId, "+1-555-0123", "123 Main St", "Jane Doe - +1-555-0987");

        assertEquals(userId, parent.parentId().value());
        assertEquals("+1-555-0123", parent.phoneNumber());
        assertEquals("123 Main St", parent.address());
        assertEquals("Jane Doe - +1-555-0987", parent.emergencyContact());
    }

    @Test
    void testParentValidation_NullUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ParentFactory.create(
                        null, // invalid
                        "+1-555-0123",
                        "123 Main St",
                        "Jane Doe"));
    }

    @Test
    void testParentIdOf() {
        UserId userId = UserId.generate();
        ParentId parentId = ParentId.of(userId);

        assertEquals(userId, parentId.value());
    }

    @Test
    void testParentIdValidation_Null() {
        assertThrows(IllegalArgumentException.class, () -> ParentId.of(null));
    }

    @Test
    void testParentWithDefaults() {
        UserId userId = UserId.generate();
        Parent parent = ParentFactory.create(
                userId,
                null, // phone number optional
                null, // address optional
                null // emergency contact optional
                );

        assertEquals(userId, parent.parentId().value());
        assertNull(parent.phoneNumber());
        assertNull(parent.address());
        assertNull(parent.emergencyContact());
        assertNotNull(parent.createdAt()); // should default to now
    }
}
