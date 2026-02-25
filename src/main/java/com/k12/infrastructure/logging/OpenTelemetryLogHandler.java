package com.k12.infrastructure.logging;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Custom Log Handler that exports logs to SigNoz via OpenTelemetry OTLP.
 *
 * <p>This handler creates a standalone OpenTelemetry LoggerProvider that sends
 * application logs directly to SigNoz using the OTLP protocol.
 */
public class OpenTelemetryLogHandler extends Handler {

    private final Logger otelLogger;
    private final SdkLoggerProvider loggerProvider;

    public OpenTelemetryLogHandler() {
        // Get configuration from system properties
        String otelEndpoint = System.getProperty("quarkus.otel.exporter.otlp.endpoint", "http://localhost:4317");
        String serviceName = System.getProperty("quarkus.application.name", "k12-backend");

        // Configure resource
        Resource resource = Resource.getDefault().toBuilder()
                .put("service.name", serviceName)
                .put("service.version", "1.0-SNAPSHOT")
                .put("deployment.environment", "development")
                .build();

        // Create OTLP log exporter
        OtlpGrpcLogRecordExporter exporter =
                OtlpGrpcLogRecordExporter.builder().setEndpoint(otelEndpoint).build();

        // Create logger provider with batch processor
        this.loggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(exporter)
                        .setScheduleDelay(1000, TimeUnit.MILLISECONDS)
                        .setMaxQueueSize(2048)
                        .setMaxExportBatchSize(512)
                        .setExporterTimeout(30000, TimeUnit.MILLISECONDS)
                        .build())
                .build();

        this.otelLogger = loggerProvider.get("java-util-logging");
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record) || otelLogger == null) {
            return;
        }

        try {
            // Map JUL level to OTEL severity
            Severity severity = mapSeverity(record.getLevel());

            // Create log body
            String message = record.getMessage();
            if (record.getThrown() != null) {
                message = message + "\n" + record.getThrown().toString();
            }

            // Emit log record
            otelLogger
                    .logRecordBuilder()
                    .setSeverity(severity)
                    .setSeverityText(record.getLevel().getName())
                    .setBody(message)
                    .emit();
        } catch (Exception e) {
            // Don't let logging errors break the application
            System.err.println("Failed to export log to OpenTelemetry: " + e.getMessage());
        }
    }

    @Override
    public void flush() {
        if (loggerProvider != null) {
            loggerProvider.forceFlush();
        }
    }

    @Override
    public void close() throws SecurityException {
        if (loggerProvider != null) {
            loggerProvider.close();
        }
    }

    private Severity mapSeverity(Level level) {
        int levelValue = level.intValue();

        if (levelValue >= Level.SEVERE.intValue()) {
            return Severity.FATAL;
        } else if (levelValue >= 900) {
            return Severity.ERROR;
        } else if (levelValue >= Level.WARNING.intValue()) {
            return Severity.WARN;
        } else if (levelValue >= Level.INFO.intValue()) {
            return Severity.INFO;
        } else if (levelValue >= Level.FINE.intValue()) {
            return Severity.DEBUG;
        } else {
            return Severity.TRACE;
        }
    }
}
