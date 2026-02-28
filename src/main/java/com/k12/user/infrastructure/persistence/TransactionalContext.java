package com.k12.user.infrastructure.persistence;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.TransactionManager;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Provides transaction-aware DSLContext instances for multi-repository operations.
 *
 * <p>This class solves the issue where multiple repositories creating their own
 * DSLContext instances don't participate in the same JTA transaction.</p>
 *
 * <p>Usage pattern in service methods marked @Transactional:</p>
 * <pre>{@code
 * @Transactional
 * public void someMethod() {
 *     DSLContext ctx = transactionalContext.getContext();
 *     userRepository.save(ctx, user);
 *     teacherRepository.save(ctx, teacher);
 *     // Both operations use the same transactional context
 * }
 * }</pre>
 */
@ApplicationScoped
@RequiredArgsConstructor
public class TransactionalContext {

    private final AgroalDataSource dataSource;
    private final TransactionManager transactionManager;

    /**
     * Gets a transactional DSLContext that participates in the current JTA transaction.
     * This method must be called within a @Transactional method.
     *
     * @return A DSLContext configured for PostgreSQL with transactional connection
     */
    public DSLContext getContext() {
        // Simply use the datasource directly
        // Agroal + JTA will ensure we get the same transactional connection
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
