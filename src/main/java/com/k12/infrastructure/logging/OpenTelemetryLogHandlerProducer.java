package com.k12.infrastructure.logging;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Registers the OpenTelemetryLogHandler with JBoss Log Manager at startup.
 *
 * <p>This bean ensures that the custom log handler is registered when
 * the application starts, so all application logs are exported to SigNoz.
 */
@ApplicationScoped
public class OpenTelemetryLogHandlerProducer {

    private OpenTelemetryLogHandler otelHandler;

    void onStart(@Observes StartupEvent event) {
        try {
            // Create the OTLP log handler
            otelHandler = new OpenTelemetryLogHandler();
            otelHandler.setLevel(Level.INFO);
            otelHandler.setEncoding("UTF-8");

            // Get the root logger
            java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
            rootLogger.addHandler(otelHandler);

            System.out.println("✓ OpenTelemetryLogHandler registered - logs will be exported to SigNoz");
        } catch (Exception e) {
            System.err.println("Failed to register OpenTelemetryLogHandler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    void onStop(@Observes ShutdownEvent event) {
        if (otelHandler != null) {
            try {
                otelHandler.close();
                System.out.println("✓ OpenTelemetryLogHandler closed");
            } catch (Exception e) {
                System.err.println("Failed to close OpenTelemetryLogHandler: " + e.getMessage());
            }
        }
    }
}
