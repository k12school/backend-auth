package com.k12.infrastructure.logging;

import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
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
 * application logs directly to SigNoz using the OTLP protocol over HTTP.
 */
public class OpenTelemetryLogHandler extends Handler {

    private final Logger otelLogger;
    private final SdkLoggerProvider loggerProvider;

    public OpenTelemetryLogHandler() {
        // Read from environment variable first, fall back to system property, then default
        // This matches Quarkus configuration precedence
        String otelEndpoint = System.getenv()
                .getOrDefault(
                        "QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT",
                        System.getProperty(
                                "quarkus.otel.exporter.otlp.endpoint", "http://k12-signoz-otel-collector:4318"));
        String serviceName = System.getProperty("quarkus.application.name", "k12-backend");

        // Print resolved configuration for debugging
        System.out.println("[OpenTelemetryLogHandler] Initializing with endpoint: " + otelEndpoint);
        System.out.println("[OpenTelemetryLogHandler] Service name: " + serviceName);

        // Configure resource
        Resource resource = Resource.getDefault().toBuilder()
                .put("service.name", serviceName)
                .put("service.version", "1.0-SNAPSHOT")
                .put("deployment.environment", "development")
                .build();

        // Create OTLP log exporter using HTTP (same protocol as traces)
        OtlpHttpLogRecordExporter exporter =
                OtlpHttpLogRecordExporter.builder().setEndpoint(otelEndpoint).build();

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
        // Debug: log every call to see if handler is being invoked
        if (record.getSequenceNumber() % 50 == 0) {
            System.out.println("[OpenTelemetryLogHandler] publish() called - isLoggable: " + isLoggable(record)
                    + ", otelLogger: " + (otelLogger != null));
        }

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

            // Debug: print every 50th log to show handler is working
            if (record.getSequenceNumber() % 50 == 0) {
                System.out.println("[OpenTelemetryLogHandler] Exporting log #" + record.getSequenceNumber() + ": "
                        + record.getLevel() + " - " + message);
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
            e.printStackTrace();
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
