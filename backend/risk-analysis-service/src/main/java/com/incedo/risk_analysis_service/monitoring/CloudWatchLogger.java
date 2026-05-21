package com.incedo.risk_analysis_service.monitoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CloudWatchLogger — Step 9 Implementation
 *
 * Writes structured log entries to Amazon CloudWatch Logs when
 * aws.enabled=true. Falls back to formatted console output when
 * aws.enabled=false (local development mode).
 *
 * Log Group  : /portfolio-risk/risk-analysis-service
 * Log Stream : risk-events
 *
 * In the AWS Console you will see log entries like:
 *   [2026-05-21T14:30:00Z] INFO | RiskAnalysisService | API_ACCESS endpoint=/risk-analysis status=SUCCESS
 *   [2026-05-21T14:30:01Z] WARN | RiskDetectionEngine | RISK_BREACH client='Client-078' level=HIGH type=CONCENTRATION_RISK
 */
@Component
public class CloudWatchLogger {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @Value("${aws.enabled:false}")
    private boolean awsEnabled;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${cloudwatch.log.group:/portfolio-risk/risk-analysis-service}")
    private String logGroupName;

    @Value("${cloudwatch.log.stream:risk-events}")
    private String logStreamName;

    private final CloudWatchLogsClient cwLogsClient;

    // Tracks the sequence token needed for CloudWatch log stream
    // (each PutLogEvents call must use the token returned by the previous call)
    private String sequenceToken = null;

    public CloudWatchLogger(CloudWatchLogsClient cwLogsClient) {
        this.cwLogsClient = cwLogsClient;
    }

    // ---------------------------------------------------------------
    // Public logging methods
    // ---------------------------------------------------------------

    /** Logs an API access event. */
    public void logServiceAccess(String serviceName, String endpoint) {
        log("INFO", serviceName,
            String.format("API_ACCESS endpoint=%s status=SUCCESS", endpoint));
    }

    /** Logs a numeric metric value (custom CloudWatch metric simulation). */
    public void logMetric(String serviceName, String metricName, double value) {
        log("METRIC", serviceName,
            String.format("metric=%s value=%.0f unit=Count", metricName, value));
    }

    /** Logs a detected risk breach. */
    public void logRiskBreach(String clientName, String riskLevel, String breachType) {
        log("WARN", "RiskDetectionEngine",
            String.format("RISK_BREACH client='%s' level=%s type=%s",
                clientName, riskLevel, breachType));
    }

    /** Logs an error. */
    public void logError(String serviceName, String errorMessage) {
        log("ERROR", serviceName, "error=" + errorMessage);
    }

    /** Logs an SNS event publish confirmation. */
    public void logEventPublished(String eventType, String clientName, String messageId) {
        log("INFO", "RiskEventPublisher",
            String.format("EVENT_PUBLISHED type=%s client='%s' messageId=%s",
                eventType, clientName, messageId));
    }

    // ---------------------------------------------------------------
    // Core log dispatcher
    // ---------------------------------------------------------------

    private void log(String level, String component, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER) + "Z";
        String formattedMessage = String.format("[%s] %-6s | %-25s | %s",
            timestamp, level, component, message);

        // Always print to console (visible in local dev AND in ECS/EC2 logs)
        System.out.println(formattedMessage);

        // If AWS is enabled, also send to CloudWatch Logs
        if (awsEnabled) {
            pushToCloudWatch(formattedMessage);
        }
    }

    // ---------------------------------------------------------------
    // CloudWatch Logs: PutLogEvents
    // ---------------------------------------------------------------

    /**
     * Pushes a single log entry to CloudWatch Logs.
     *
     * Steps:
     *   1. Ensure log group exists (create if needed)
     *   2. Ensure log stream exists (create if needed)
     *   3. PutLogEvents with current sequence token
     *   4. Save new sequence token for next call
     */
    private void pushToCloudWatch(String message) {
        try {
            ensureLogGroupAndStreamExist();

            InputLogEvent logEvent = InputLogEvent.builder()
                .message(message)
                .timestamp(Instant.now().toEpochMilli())
                .build();

            PutLogEventsRequest.Builder requestBuilder = PutLogEventsRequest.builder()
                .logGroupName(logGroupName)
                .logStreamName(logStreamName)
                .logEvents(List.of(logEvent));

            // Sequence token required after first call
            if (sequenceToken != null) {
                requestBuilder.sequenceToken(sequenceToken);
            }

            PutLogEventsResponse response = cwLogsClient.putLogEvents(requestBuilder.build());
            sequenceToken = response.nextSequenceToken();

        } catch (DataAlreadyAcceptedException e) {
            // Message already in CloudWatch — update sequence token and move on
            sequenceToken = e.expectedSequenceToken();
        } catch (InvalidSequenceTokenException e) {
            // Token expired or out of sync — reset and retry next time
            sequenceToken = e.expectedSequenceToken();
        } catch (Exception e) {
            // CloudWatch not reachable — silently fall through (console log already written)
            System.err.println("[CloudWatchLogger] AWS CloudWatch unavailable: " + e.getMessage());
        }
    }

    /**
     * Creates the CloudWatch log group and stream if they don't already exist.
     * Safe to call on every log entry (checks first, creates only if missing).
     */
    private void ensureLogGroupAndStreamExist() {
        // Create log group
        try {
            cwLogsClient.createLogGroup(r -> r.logGroupName(logGroupName));
        } catch (ResourceAlreadyExistsException ignored) {
            // Already exists — normal on subsequent runs
        }

        // Create log stream
        try {
            cwLogsClient.createLogStream(r -> r
                .logGroupName(logGroupName)
                .logStreamName(logStreamName));
        } catch (ResourceAlreadyExistsException ignored) {
            // Already exists
        }
    }
}
