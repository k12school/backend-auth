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
}
