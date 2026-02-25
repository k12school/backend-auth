package com.k12.monitoring.example;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class MetricsServiceTest {

    @Inject
    MetricsService metricsService;

    @Inject
    MeterRegistry registry;

    @Test
    void shouldRecordUserLogin() {
        // Given
        String tenantId = "tenant-123";
        String userId = "user-456";

        // When
        metricsService.recordUserLogin(tenantId, userId);

        // Then
        var counter = registry.find("user.logins")
                .tags("tenant", tenantId, "status", "success")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThan(0);
    }

    @Test
    void shouldRecordDatabaseQueryTime() {
        // Given
        String queryType = "SELECT";
        long durationMs = 100;

        // When
        metricsService.recordDatabaseQueryTime(queryType, durationMs);

        // Then
        var timer = registry.find("database.query.duration")
                .tags("query_type", queryType)
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isGreaterThan(0);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(durationMs);
    }

    @Test
    void shouldTimeOperation() {
        // When
        String result = metricsService.timeOperation("test-operation", () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "completed";
        });

        // Then
        assertThat(result).isEqualTo("completed");

        var timer = registry.find("operation.duration")
                .tags("operation", "test-operation")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(50);
    }

    @Test
    void shouldTrackActiveConnections() {
        // Given
        metricsService.registerActiveConnectionsGauge();

        // When
        metricsService.incrementActiveConnections();
        metricsService.incrementActiveConnections();
        metricsService.decrementActiveConnections();

        // Then
        var gauge = registry.find("connections.active").gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(1.0);
    }
}
