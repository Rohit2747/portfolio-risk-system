package com.incedo.risk_analysis_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

/**
 * RiskEventPublisher — Step 10 Implementation
 *
 * Publishes RiskThresholdBreached events to Amazon SNS when aws.enabled=true.
 * Falls back to structured console logging when aws.enabled=false.
 *
 * AWS Event Flow:
 *   Risk Service detects breach
 *       → RiskEventPublisher.publishRiskAlert(event)
 *           → SNS.publish(topicArn, messageJson)
 *               → SQS Queue subscribed to SNS
 *                   → AI Insight Service polls SQS
 *                       → AI Insight generated
 *
 * The SNS message includes:
 *   - Message body: full RiskAlertEvent as JSON
 *   - Message attribute "eventType": used for SQS filter policies
 *   - Message attribute "riskLevel": HIGH or MEDIUM (for routing)
 *
 * To set up the SNS topic in AWS:
 *   aws sns create-topic --name risk-threshold-breached
 *   (copy the ARN into application.properties or SNS_RISK_TOPIC_ARN env var)
 */
@Component
public class RiskEventPublisher {

    @Value("${aws.enabled:false}")
    private boolean awsEnabled;

    @Value("${sns.topic.arn.risk-threshold-breached:arn:aws:sns:us-east-1:ACCOUNT_ID:risk-threshold-breached}")
    private String riskTopicArn;

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    public RiskEventPublisher(SnsClient snsClient) {
        this.snsClient = snsClient;
        this.objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Publishes a RiskAlertEvent.
     *
     * LOCAL mode (aws.enabled=false): structured console logging only.
     * AWS mode   (aws.enabled=true):  publishes to SNS topic.
     *
     * @param event the risk alert event to publish
     */
    public void publishRiskAlert(RiskAlertEvent event) {

        if (awsEnabled) {
            publishToSns(event);
        } else {
            logToConsole(event);
        }
    }

    // ---------------------------------------------------------------
    // AWS SNS Publishing
    // ---------------------------------------------------------------

    /**
     * Serializes the event to JSON and publishes to the SNS topic.
     * Message attributes allow SQS filter policies to route events.
     */
    private void publishToSns(RiskAlertEvent event) {
        try {
            // Serialize event to JSON
            String messageBody = objectMapper.writeValueAsString(event);

            // Build SNS publish request with message attributes for filtering
            PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(riskTopicArn)
                .message(messageBody)
                .subject("RiskThresholdBreached")
                // Message attributes — used by SQS subscription filter policies
                .messageAttributes(Map.of(
                    "eventType", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("RiskThresholdBreachedEvent")
                        .build(),
                    "riskLevel", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(event.getRiskLevel())
                        .build(),
                    "clientId", MessageAttributeValue.builder()
                        .dataType("Number")
                        .stringValue(String.valueOf(event.getClientId()))
                        .build()
                ))
                .build();

            PublishResponse response = snsClient.publish(publishRequest);

            System.out.printf(
                "[SNS] ✅ Published RiskThresholdBreached | client=%s | level=%s | messageId=%s%n",
                event.getClientName(), event.getRiskLevel(), response.messageId()
            );

        } catch (JsonProcessingException e) {
            System.err.println("[RiskEventPublisher] JSON serialization failed: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[RiskEventPublisher] SNS publish failed: " + e.getMessage());
            // Fallback to console so breach is not silently lost
            logToConsole(event);
        }
    }

    // ---------------------------------------------------------------
    // Local console logging (when aws.enabled=false)
    // ---------------------------------------------------------------

    private void logToConsole(RiskAlertEvent event) {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║   [LOCAL] RISK THRESHOLD BREACHED — EVENT LOGGED          ║");
        System.out.println("║   (aws.enabled=false → set to true for real SNS publish)  ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf ("║  Client     : %-42s ║%n", event.getClientName());
        System.out.printf ("║  Risk Level : %-42s ║%n", event.getRiskLevel());
        System.out.printf ("║  Value      : ₹%-41.2f ║%n", event.getPortfolioValue());
        System.out.printf ("║  Daily Chg  : %-41.2f%% ║%n", event.getDailyChangePercent());
        System.out.printf ("║  Breaches   : %-42d ║%n",
            event.getBreaches() != null ? event.getBreaches().size() : 0);
        System.out.printf ("║  Timestamp  : %-42s ║%n", event.getTimestamp());

        if (event.getBreaches() != null && !event.getBreaches().isEmpty()) {
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            for (var breach : event.getBreaches()) {
                System.out.printf("║  → [%-20s] %-32s ║%n",
                    breach.getBreachType(),
                    truncate(breach.getDescription(), 32));
            }
        }
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  AWS SNS Topic: risk-threshold-breached                   ║");
        System.out.println("║  AI Insight Service will consume via SQS queue            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
