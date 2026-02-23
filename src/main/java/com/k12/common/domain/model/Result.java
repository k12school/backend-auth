package com.k12.common.domain.model;

/**
 * Railway Oriented Programming Result type.
 * Represents either a success with a value or a failure with an error.
 *
 * @param <T> The success type
 * @param <E> The error type
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    /**
     * Creates a successful result containing the given value.
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failed result containing the given error.
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    /**
     * Returns true if this is a success, false otherwise.
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Returns true if this is a failure, false otherwise.
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Maps the success value if present, using the given function.
     * If this is a failure, returns the same failure.
     */
    @SuppressWarnings("unchecked")
    default <U> Result<U, E> map(java.util.function.Function<T, U> mapper) {
        if (isSuccess()) {
            T value = ((Success<T, E>) this).value();
            try {
                return Result.success(mapper.apply(value));
            } catch (Exception e) {
                return Result.failure((E) e);
            }
        }
        return (Result<U, E>) this;
    }

    /**
     * FlatMaps the success value if present, using the given function.
     * If this is a failure, returns the same failure.
     */
    @SuppressWarnings("unchecked")
    default <U> Result<U, E> flatMap(java.util.function.Function<T, Result<U, E>> mapper) {
        if (isSuccess()) {
            T value = ((Success<T, E>) this).value();
            try {
                return mapper.apply(value);
            } catch (Exception e) {
                return Result.failure((E) e);
            }
        }
        return (Result<U, E>) this;
    }

    /**
     * Gets the success value if present, throws otherwise.
     */
    default T get() {
        if (isSuccess()) {
            return ((Success<T, E>) this).value();
        }
        throw new IllegalStateException("Cannot get value from failure: " + ((Failure<T, E>) this).error());
    }

    /**
     * Gets the error if present, throws otherwise.
     */
    default E getError() {
        if (isFailure()) {
            return ((Failure<T, E>) this).error();
        }
        throw new IllegalStateException("Cannot get error from success");
    }

    /**
     * Returns the success value if present, or the given default value.
     */
    default T getOrElse(T defaultValue) {
        if (isSuccess()) {
            return ((Success<T, E>) this).value();
        }
        return defaultValue;
    }

    /**
     * Folds the result into a single value using the provided functions.
     * This is the core operation of Railway Oriented Programming.
     *
     * @param onSuccess Function to apply if this is a success
     * @param onFailure Function to apply if this is a failure
     * @param <R> The result type
     * @return The result of applying the appropriate function
     */
    @SuppressWarnings("unchecked")
    default <R> R fold(java.util.function.Function<T, R> onSuccess, java.util.function.Function<E, R> onFailure) {
        if (isSuccess()) {
            T value = ((Success<T, E>) this).value();
            return onSuccess.apply(value);
        } else {
            E error = ((Failure<T, E>) this).error();
            return onFailure.apply(error);
        }
    }

    /**
     * A successful result containing a value.
     */
    record Success<T, E>(T value) implements Result<T, E> {}

    /**
     * A failed result containing an error.
     */
    record Failure<T, E>(E error) implements Result<T, E> {}
}
