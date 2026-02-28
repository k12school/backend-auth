package com.k12.user.infrastructure.persistence;

import static com.k12.backend.infrastructure.jooq.public_.tables.UserEvents.USER_EVENTS;
import static com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS;

import com.k12.backend.infrastructure.jooq.public_.tables.records.UserEventsRecord;
import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.UserId;
import com.k12.tenant.infrastructure.persistence.KryoEventSerializer;
import com.k12.tenant.infrastructure.persistence.KryoEventSerializer.SerializationException;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.UserReconstructor;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.models.events.UserEvents;
import com.k12.user.domain.models.events.UserEvents.UserCreated;
import com.k12.user.domain.ports.out.UserRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Implementation of UserRepository using jOOQ with event sourcing.
 * Loads User aggregates from user_events table using Kryo deserialization.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final AgroalDataSource dataSource;

    @Override
    @Transactional
    public User save(User user) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        try {
            // Convert immutable Set to HashSet for Kryo serialization
            Set<UserRole> roles = new HashSet<>(user.userRole());

            // Create UserCreated event from the User aggregate
            UserCreated event = new UserEvents.UserCreated(
                    user.userId(),
                    user.emailAddress(),
                    user.passwordHash(),
                    roles,
                    user.status(),
                    user.name(),
                    Instant.now(),
                    1L);

            // Serialize the event
            byte[] eventData = KryoEventSerializer.serializeUserEvent(event);

            // Save event to event store
            ctx.insertInto(
                            USER_EVENTS,
                            USER_EVENTS.USER_ID,
                            USER_EVENTS.EVENT_TYPE,
                            USER_EVENTS.EVENT_DATA,
                            USER_EVENTS.VERSION,
                            USER_EVENTS.OCCURRED_AT,
                            USER_EVENTS.CREATED_AT)
                    .values(
                            event.userId().value(),
                            "UserCreated",
                            eventData,
                            event.version(),
                            OffsetDateTime.ofInstant(event.createdAt(), ZoneOffset.UTC),
                            OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
                    .execute();

            // Update projection table for queries
            String rolesString = user.userRole().stream()
                    .map(Enum::name)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            ctx.insertInto(
                            USERS,
                            USERS.ID,
                            USERS.EMAIL,
                            USERS.PASSWORD_HASH,
                            USERS.ROLES,
                            USERS.STATUS,
                            USERS.NAME,
                            USERS.CREATED_AT,
                            USERS.UPDATED_AT)
                    .values(
                            user.userId().value(),
                            user.emailAddress().value(),
                            user.passwordHash().value(),
                            rolesString,
                            user.status().name(),
                            user.name().value(),
                            now,
                            now)
                    .onConflict(USERS.ID)
                    .doUpdate()
                    .set(USERS.EMAIL, user.emailAddress().value())
                    .set(USERS.ROLES, rolesString)
                    .set(USERS.STATUS, user.status().name())
                    .set(USERS.NAME, user.name().value())
                    .set(USERS.UPDATED_AT, now)
                    .execute();

            return user;
        } catch (SerializationException e) {
            throw new RuntimeException("Failed to serialize user event", e);
        }
    }

    @Override
    public Optional<User> findById(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        try {
            // First try to load from events
            List<UserEvents> events = loadEvents(ctx, userId.value());
            if (events.isEmpty()) {
                // Fallback: load from projection table for users created via migrations
                return findByProjection(ctx, userId.value());
            }
            Result<User, UserError> result = UserReconstructor.reconstructWithValidation(events);
            return result.isSuccess() ? Optional.of(result.get()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Loads user directly from projection table.
     * Used as fallback for users created via database migrations.
     */
    private Optional<User> findByProjection(DSLContext ctx, UUID userId) {
        var record = ctx.selectFrom(USERS).where(USERS.ID.eq(userId)).fetchOne();

        if (record == null) {
            return Optional.empty();
        }

        try {
            User user = new User(
                    new UserId(record.getId()),
                    EmailAddress.of(record.getEmail()),
                    new PasswordHash(record.getPasswordHash()),
                    parseRoles(record.getRoles()),
                    UserStatus.valueOf(record.getStatus()),
                    UserName.of(record.getName()));
            return Optional.of(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Parses roles string into Set<UserRole>.
     */
    private Set<UserRole> parseRoles(String rolesString) {
        Set<UserRole> roles = new HashSet<>();
        if (rolesString != null && !rolesString.isEmpty()) {
            String[] parts = rolesString.split(",");
            for (String part : parts) {
                try {
                    roles.add(UserRole.valueOf(part.trim()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid roles
                }
            }
        }
        return roles;
    }

    @Override
    public Optional<User> findByEmailAddress(String emailAddress) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        try {
            UUID userId = ctx.select(USERS.ID)
                    .from(USERS)
                    .where(USERS.EMAIL.eq(emailAddress))
                    .fetchOne(USERS.ID);
            if (userId == null) {
                return Optional.empty();
            }
            return findById(new UserId(userId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByEmail(String emailAddress) {
        return findByEmailAddress(emailAddress).isPresent();
    }

    @Override
    public void deleteById(UserId userId) {
        throw new UnsupportedOperationException("User deletion not yet implemented");
    }

    @Override
    public long count() {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchCount(USERS);
    }

    /**
     * Load all events for a user from the event store.
     *
     * @param ctx The DSLContext
     * @param userId The user ID
     * @return List of events in chronological order
     */
    private List<UserEvents> loadEvents(DSLContext ctx, UUID userId) {
        try {
            List<UserEventsRecord> records = ctx.selectFrom(USER_EVENTS)
                    .where(USER_EVENTS.USER_ID.eq(userId))
                    .orderBy(USER_EVENTS.VERSION.asc())
                    .fetch();
            List<UserEvents> events = new ArrayList<>();
            for (UserEventsRecord record : records) {
                byte[] eventData = record.getEventData();
                events.add(KryoEventSerializer.deserializeUserEvent(eventData));
            }
            return events;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
