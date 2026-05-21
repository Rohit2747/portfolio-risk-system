package com.incedo.risk_analysis_service.monitoring;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CloudWatchLogger
 *
 * Structured logging component that simulates AWS CloudWatch log format.
 *
 * CURRENT IMPLEMENTATION: Console logging in CloudWatch-compatible structured format
 *
 * AWS UPGRADE PATH:
 * Replace logEntry() body with:
 *   CloudWatchLogsClient cwClient = CloudWatchLogsClient.create();
 *   cwClient.putLogEvents(r -> r
 *     .logGroupName("/portfolio-risk/risk-analysis-service")
 *     .logStreamName("risk-events")
 *     .logEvents(InputLogEvent.builder()
 *       .message(message)
 *       .timestamp(Instant.now().toEpochMilli())
 *       .build()));
 */
@Component
public class CloudWatchLogger {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private static final String LOG_GROUP = "/portfolio-risk/risk-analysis-service";

    /**
     * Logs a service access event (equivalent to CloudWatch access log).
     */
    public void logServiceAccess(String serviceName, String endpoint) {
        logEntry("INFO", serviceName, String.format(
            "API_ACCESS endpoint=%s status=SUCCESS", endpoint
        ));
    }

    /**
     * Logs a metric value (equivalent to CloudWatch custom metric).
     */
    public void logMetric(String serviceName, String metricName, double value) {
        logEntry("METRIC", serviceName, String.format(
            "metric=%s value=%.2f unit=Count", metricName, value
        ));
    }

    /**
     * Logs a risk breach event.
     */
    public void logRiskBreach(String clientName, String riskLevel, String breachType) {
        logEntry("WARN", "RiskDetectionEngine", String.format(
            "RISK_BREACH client='%s' level=%s type=%s",
            clientName, riskLevel, breachType
        ));
    }

    /**
     * Logs an error.
     */
    public void logError(String serviceName, String errorMessage) {
        logEntry("ERROR", serviceName, "error=" + errorMessage);
    }

    /**
     * Core log formatter — outputs in AWS CloudWatch structured log format.
     *
     * Format:
     *   [TIMESTAMP] LEVEL LOG_GROUP | SERVICE | message
     *
     * AWS UPGRADE: Replace this with CloudWatchLogsClient.putLogEvents()
     */
    private void logEntry(String level, String serviceName, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER) + "Z";
        System.out.printf("[%s] %-6s %s | %-25s | %s%n",
            timestamp, level, LOG_GROUP, serviceName, message
        );

        // TODO (AWS Step): send to CloudWatch Logs:
        // cwClient.putLogEvents(r -> r.logGroupName(LOG_GROUP).logStreamName(serviceName)
        //   .logEvents(InputLogEvent.builder().message(message)
        //     .timestamp(Instant.now().toEpochMilli()).build()));
    }
}
