package com.k12.user.domain.error;

public sealed interface UserError {
    record ValidationError(String message) implements UserError {}

    record ConflictError(String message) implements UserError {}

    record NotFoundError(String message) implements UserError {}

    record PersistenceError(String message) implements UserError {}
}
