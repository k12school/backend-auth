package com.k12.user.infrastructure.persistence;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.transaction.TransactionManager;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * Provides transaction-aware DSLContext instances for multi-repository operations.
 *
 * <p>This class is request-scoped, meaning the same instance is used throughout
 * a single HTTP request/transaction. The DSLContext is cached to ensure all
 * repositories use the same database connection.</p>
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
@RequestScoped
@RequiredArgsConstructor
public class TransactionalContext {

    private final AgroalDataSource dataSource;
    private final TransactionManager transactionManager;

    private DSLContext cachedContext;
    private Connection cachedConnection;

    /**
     * Gets a transactional DSLContext that participates in the current JTA transaction.
     * The context is cached for the duration of the request to ensure all repositories
     * use the same database connection.
     *
     * @return A DSLContext configured for PostgreSQL with transactional connection
     */
    public DSLContext getContext() {
        if (cachedContext == null) {
            try {
                // Get a connection from the datasource
                // In a JTA transaction, Agroal returns the same transactional connection
                cachedConnection = dataSource.getConnection();

                // Create DSLContext with the connection
                // This ensures all operations use the same physical connection
                cachedContext = DSL.using(cachedConnection, SQLDialect.POSTGRES);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create transactional DSLContext", e);
            }
        }
        return cachedContext;
    }
}
