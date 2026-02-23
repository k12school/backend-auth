package com.k12.user.infrastructure.persistence;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.domain.models.events.UserEvents;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Utility class to set up test data for integration tests.
 * This class inserts a test SUPER_ADMIN user with proper Kryo-serialized events.
 */
@ApplicationScoped
public class SetupTestData {

    @Inject
    AgroalDataSource dataSource;

    /**
     * Sets up the test user data.
     * This should be called before running integration tests.
     */
    public void setupTestUser() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        // Check if user already exists in users table
        Long existingUserCount = ctx.selectCount()
                .from(com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS)
                .where(com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.ID.eq(userId))
                .fetchOne(0, Long.class);

        if (existingUserCount != null && existingUserCount > 0) {
            System.out.println("Test user already exists, skipping setup");
            return;
        }

        // Create UserCreated event
        UserEvents.UserCreated event = new UserEvents.UserCreated(
                new UserId(userId),
                new EmailAddress("admin@k12.com"),
                new PasswordHash("$2b$12$UJ3TGU9.Po1qYDEBuNgeout1LgBuxDLgfxebbyoAPmewn5Evj0Q.6"),
                new HashSet<>(java.util.Set.of(UserRole.SUPER_ADMIN)),
                UserStatus.ACTIVE,
                new UserName("Super Administrator"),
                Instant.now(),
                1L);

        // Serialize the event using the same Kryo configuration as the deserializer
        byte[] eventData = serializeUserEvent(event);

        // Insert into user_events table
        int eventsInserted = ctx.insertInto(
                        com.k12.backend.infrastructure.jooq.public_.tables.UserEvents.USER_EVENTS,
                        com.k12.backend.infrastructure.jooq.public_.tables.UserEvents.USER_EVENTS.USER_ID,
                        com.k12.backend.infrastructure.jooq.public_.tables.UserEvents.USER_EVENTS.EVENT_TYPE,
                        com.k12.backend.infrastructure.jooq.public_.tables.UserEvents.USER_EVENTS.EVENT_DATA,
                        com.k12.backend.infrastructure.jooq.public_.tables.UserEvents.USER_EVENTS.VERSION,
                        com.k12.backend.infrastructure.jooq.public_.tables.UserEvents.USER_EVENTS.OCCURRED_AT)
                .values(userId, "UserCreated", eventData, 1L, java.time.OffsetDateTime.now())
                .execute();

        // Insert into users projection table
        int usersInserted = ctx.insertInto(
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.ID,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.EMAIL,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.PASSWORD_HASH,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.ROLES,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.STATUS,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.NAME,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.VERSION,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.CREATED_AT,
                        com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS.UPDATED_AT)
                .values(
                        userId,
                        "admin@k12.com",
                        "$2b$12$UJ3TGU9.Po1qYDEBuNgeout1LgBuxDLgfxebbyoAPmewn5Evj0Q.6",
                        "SUPER_ADMIN",
                        "ACTIVE",
                        "Super Administrator",
                        1L,
                        java.time.OffsetDateTime.now(),
                        java.time.OffsetDateTime.now())
                .execute();

        System.out.println(
                "Test user data inserted successfully: " + eventsInserted + " events, " + usersInserted + " users");
    }

    /**
     * Serializes a User event using the same Kryo configuration as KryoEventSerializer.
     * This ensures compatibility between serialization and deserialization.
     */
    private byte[] serializeUserEvent(UserEvents event) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Output output = new Output(baos)) {

            Kryo kryo = new Kryo();

            // Register UUID with custom serializer (same as KryoEventSerializer)
            kryo.register(UUID.class, new KryoUuidSerializer());

            // Register common types
            kryo.register(String.class);
            kryo.register(long.class);
            kryo.register(int.class);
            kryo.register(boolean.class);

            // Register Java time types
            kryo.register(Instant.class);

            // Register User value objects
            kryo.register(com.k12.common.domain.model.UserId.class);
            kryo.register(EmailAddress.class);
            kryo.register(PasswordHash.class);
            kryo.register(UserName.class);
            kryo.register(UserRole.class);
            kryo.register(UserStatus.class);
            kryo.register(HashSet.class);
            kryo.register(ArrayList.class);

            // Register all UserEvents subclasses (use same IDs as deserializeUserEvent)
            int classId = 200;
            kryo.register(UserEvents.UserCreated.class, classId++);
            kryo.register(UserEvents.UserSuspended.class, classId++);
            kryo.register(UserEvents.UserActivated.class, classId++);
            kryo.register(UserEvents.UserEmailUpdated.class, classId++);
            kryo.register(UserEvents.UserPasswordUpdated.class, classId++);
            kryo.register(UserEvents.UserRoleAdded.class, classId++);
            kryo.register(UserEvents.UserRoleRemoved.class, classId++);
            kryo.register(UserEvents.UserNameUpdated.class, classId++);

            // Configure settings
            kryo.setReferences(false);
            kryo.setRegistrationRequired(true);

            kryo.writeClassAndObject(output, event);
            output.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    /**
     * Custom UUID serializer for Kryo (same as in KryoEventSerializer).
     * Ensures UUIDs are serialized efficiently without reflection issues.
     */
    private static class KryoUuidSerializer extends com.esotericsoftware.kryo.Serializer<UUID> {
        @Override
        public void write(Kryo kryo, Output output, UUID uuid) {
            output.writeLong(uuid.getMostSignificantBits());
            output.writeLong(uuid.getLeastSignificantBits());
        }

        @Override
        public UUID read(Kryo kryo, com.esotericsoftware.kryo.io.Input input, Class<? extends UUID> type) {
            long mostSigBits = input.readLong();
            long leastSigBits = input.readLong();
            return new UUID(mostSigBits, leastSigBits);
        }
    }
}
