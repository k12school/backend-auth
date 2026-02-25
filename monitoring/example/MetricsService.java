package com.k12.monitoring.example;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmMemory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Example service showing how to add custom metrics to your Quarkus application.
 *
 * Metrics will be automatically available at /metrics endpoint and scraped by Prometheus.
 */
@ApplicationScoped
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    @Inject
    MeterRegistry registry;

    // Track counters by tenant
    private final ConcurrentHashMap<String, Counter> tenantCounters = new ConcurrentHashMap<>();

    // Track operation timers
    private final ConcurrentHashMap<String, Timer> operationTimers = new ConcurrentHashMap<>();

    // Active connections gauge
    private final AtomicLong activeConnections = new AtomicLong(0);

    // ============================================
    // COUNTER METRICS
    // ============================================

    /**
     * Increment a counter for user logins
     *
     * @param tenantId The tenant ID
     * @param userId The user ID
     */
    public void recordUserLogin(String tenantId, String userId) {
        Counter.builder("user.logins")
                .description("Total number of user logins")
                .tags("tenant", tenantId, "status", "success")
                .register(registry)
                .increment();

        log.info("Recorded login metric for tenant={}, user={}", tenantId, userId);
    }

    /**
     * Increment a counter for failed login attempts
     */
    public void recordFailedLogin(String tenantId, String reason) {
        Counter.builder("user.logins")
                .description("Total number of user logins")
                .tags("tenant", tenantId, "status", "failed", "reason", reason)
                .register(registry)
                .increment();
    }

    /**
     * Increment a counter for API requests
     */
    public void recordApiRequest(String endpoint, String method, int statusCode) {
        Counter.builder("api.requests")
                .description("Total number of API requests")
                .tags("endpoint", endpoint, "method", method, "status", String.valueOf(statusCode))
                .register(registry)
                .increment();
    }

    // ============================================
    // TIMER METRICS
    // ============================================

    /**
     * Record the time taken to execute a database query
     *
     * @param queryType Type of query (SELECT, INSERT, UPDATE, DELETE)
     * @param durationMs Duration in milliseconds
     */
    public void recordDatabaseQueryTime(String queryType, long durationMs) {
        Timer.builder("database.query.duration")
                .description("Database query execution time")
                .tags("query_type", queryType)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Record external API call duration
     */
    public void recordExternalApiCall(String service, long durationMs, boolean success) {
        Timer.builder("external.api.duration")
                .description("External API call duration")
                .tags("service", service, "success", String.valueOf(success))
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Time a code block
     */
    public <T> T timeOperation(String operationName, java.util.function.Supplier<T> supplier) {
        Timer timer = operationTimers.computeIfAbsent(operationName,
            name -> Timer.builder("operation.duration")
                    .description("Custom operation duration")
                    .tags("operation", name)
                    .register(registry));

        return timer.record(supplier);
    }

    // ============================================
    // GAUGE METRICS
    // ============================================

    /**
     * Register a gauge for active connections
     * Call this during application startup
     */
    public void registerActiveConnectionsGauge() {
        Gauge.builder("connections.active", activeConnections, AtomicLong::longValue)
                .description("Number of active database connections")
                .tags("type", "database")
                .register(registry);

        log.info("Registered active connections gauge");
    }

    /**
     * Increment active connections counter
     */
    public void incrementActiveConnections() {
        activeConnections.incrementAndGet();
    }

    /**
     * Decrement active connections counter
     */
    public void decrementActiveConnections() {
        activeConnections.decrementAndGet();
    }

    /**
     * Register a gauge for cache size
     */
    public void registerCacheSizeGauge(String cacheName, ConcurrentHashMap<?, ?> cache) {
        Gauge.builder("cache.size", cache, ConcurrentHashMap::size)
                .description("Cache size")
                .tags("cache", cacheName)
                .register(registry);
    }

    // ============================================
    // SUMMARY METRICS
    // ============================================

    /**
     * Record response payload size
     */
    public void recordResponseSize(String endpoint, int bytes) {
        registry.counter("http.response.size.bytes",
                "endpoint", endpoint,
                "unit", "bytes").increment(bytes);
    }

    // ============================================
    // BUSINESS METRICS EXAMPLES
    // ============================================

    /**
     * Record student enrollment
     */
    public void recordStudentEnrollment(String tenantId, String grade) {
        Counter.builder("business.student.enrollment")
                .description("Student enrollment events")
                .tags("tenant", tenantId, "grade", grade)
                .register(registry)
                .increment();
    }

    /**
     * Record assignment submission
     */
    public void recordAssignmentSubmission(String tenantId, String assignmentType) {
        Counter.builder("business.assignment.submission")
                .description("Assignment submission events")
                .tags("tenant", tenantId, "type", assignmentType)
                .register(registry)
                .increment();
    }
}
