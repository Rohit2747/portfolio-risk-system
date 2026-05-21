package com.incedo.risk_analysis_service.events;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * RiskEventPublisher
 *
 * Publishes risk alert events to the event bus.
 *
 * CURRENT IMPLEMENTATION: Structured console logging
 * (simulates event publishing for local development)
 *
 * AWS UPGRADE PATH:
 * Replace the publishRiskAlert() body with:
 *
 *   String topicArn = "arn:aws:sns:us-east-1:ACCOUNT:risk-threshold-breached";
 *   String messageJson = objectMapper.writeValueAsString(event);
 *   snsClient.publish(r -> r.topicArn(topicArn).message(messageJson));
 *
 * The rest of the code stays exactly the same.
 */
@Component
public class RiskEventPublisher {

    /**
     * Publishes a RiskAlertEvent.
     *
     * Currently logs to console in structured format.
     * AWS upgrade: replace body with SNS publish call.
     */
    public void publishRiskAlert(RiskAlertEvent event) {

        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║          RISK THRESHOLD BREACHED — EVENT PUBLISHED         ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.printf ("║  Client    : %-40s ║%n", event.getClientName());
        System.out.printf ("║  Risk Level: %-40s ║%n", event.getRiskLevel());
        System.out.printf ("║  Value     : ₹%-39.2f ║%n", event.getPortfolioValue());
        System.out.printf ("║  Daily Chg : %-39.2f%% ║%n", event.getDailyChangePercent());
        System.out.printf ("║  Breaches  : %-40d ║%n",
            event.getBreaches() != null ? event.getBreaches().size() : 0);
        System.out.printf ("║  Timestamp : %-40s ║%n", event.getTimestamp());

        if (event.getBreaches() != null) {
            System.out.println("╠══════════════════════════════════════════════════════╣");
            System.out.println("║  Breach Details:                                          ║");
            for (var breach : event.getBreaches()) {
                System.out.printf("║  → [%-20s] %s %n", breach.getBreachType(), breach.getDescription());
            }
        }

        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  [AWS UPGRADE] → Publish to SNS: risk-threshold-breached  ║");
        System.out.println("║  [AWS UPGRADE] → AI Insight Service will consume via SQS  ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");

        // TODO (AWS Step): Replace above with SNS publish:
        // snsClient.publish(r -> r.topicArn(topicArn).message(toJson(event)));
    }
}
