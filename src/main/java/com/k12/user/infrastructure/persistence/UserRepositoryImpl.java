package com.k12.user.infrastructure.persistence;

import static com.k12.backend.infrastructure.jooq.public_.tables.UserEvents.USER_EVENTS;
import static com.k12.backend.infrastructure.jooq.public_.tables.Users.USERS;

import com.k12.backend.infrastructure.jooq.public_.tables.records.UserEventsRecord;
import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.UserId;
import com.k12.tenant.infrastructure.persistence.KryoEventSerializer;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserReconstructor;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.models.events.UserEvents;
import com.k12.user.domain.ports.out.UserRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    public User save(User user) {
        throw new UnsupportedOperationException("User saving not yet implemented");
    }

    @Override
    public Optional<User> findById(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        try {
            List<UserEvents> events = loadEvents(ctx, userId.value());
            if (events.isEmpty()) {
                return Optional.empty();
            }
            Result<User, UserError> result = UserReconstructor.reconstructWithValidation(events);
            return result.isSuccess() ? Optional.of(result.get()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
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
