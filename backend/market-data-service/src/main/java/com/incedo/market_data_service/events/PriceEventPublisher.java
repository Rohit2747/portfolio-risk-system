package com.incedo.market_data_service.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incedo.market_data_service.model.marketData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

/**
 * PriceEventPublisher
 *
 * Publishes PriceUpdated events to Amazon SNS after each price tick.
 *
 * AWS Flow:
 *   Market Data Service (every 5s tick)
 *       → PriceEventPublisher.publishPriceUpdate(marketData)
 *           → SNS topic: "price-updated"
 *               → SQS Queue subscribed to that topic
 *                   → Risk Analysis Service polls SQS (future enhancement)
 *
 * For this project, Risk Analysis Service currently polls via REST.
 * This publisher demonstrates the AWS event flow is implemented.
 */
@Component
public class PriceEventPublisher {

    @Value("${aws.enabled:false}")
    private boolean awsEnabled;

    @Value("${sns.topic.arn.price-updated:arn:aws:sns:us-east-1:ACCOUNT_ID:price-updated}")
    private String priceTopicArn;

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Throttle: only publish to SNS every N ticks to avoid flooding
    // (SNS has cost per publish; we don't need every 5s tick in AWS)
    private int tickCount = 0;
    private static final int PUBLISH_EVERY_N_TICKS = 6; // ~30 seconds

    public PriceEventPublisher(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    /**
     * Called by PriceSimulatorService after each price tick.
     * Publishes a PriceUpdatedEvent for a single stock to SNS.
     */
    public void publishPriceUpdate(marketData data) {
        tickCount++;

        // Only publish every Nth tick to throttle SNS calls
        if (tickCount % PUBLISH_EVERY_N_TICKS != 0) return;

        if (awsEnabled) {
            publishToSns(data);
        } else {
            // In local mode, just log occasionally (not every tick — too noisy)
            if (tickCount % 60 == 0) { // log once per ~5 min
                System.out.printf(
                    "[LOCAL] PriceUpdated: %s = ₹%.2f (%.2f%% daily)%n",
                    data.getStockSymbol(), data.getCurrentPrice(), data.getDailyChangePercent()
                );
            }
        }
    }

    private void publishToSns(marketData data) {
        try {
            // Build the PriceUpdated event payload
            Map<String, Object> event = Map.of(
                "eventType",          "PriceUpdatedEvent",
                "stockSymbol",        data.getStockSymbol(),
                "stockName",          data.getStockName(),
                "currentPrice",       data.getCurrentPrice(),
                "previousPrice",      data.getPreviousPrice(),
                "changePercent",      data.getChangePercent(),
                "dailyChangePercent", data.getDailyChangePercent(),
                "timestamp",          data.getLastUpdated()
            );

            String messageBody = objectMapper.writeValueAsString(event);

            PublishRequest request = PublishRequest.builder()
                .topicArn(priceTopicArn)
                .message(messageBody)
                .subject("PriceUpdated")
                .messageAttributes(Map.of(
                    "eventType", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("PriceUpdatedEvent")
                        .build(),
                    "stockSymbol", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(data.getStockSymbol())
                        .build()
                ))
                .build();

            PublishResponse response = snsClient.publish(request);
            System.out.printf("[SNS] ✅ PriceUpdated: %s = ₹%.2f | msgId=%s%n",
                data.getStockSymbol(), data.getCurrentPrice(), response.messageId());

        } catch (Exception e) {
            System.err.println("[PriceEventPublisher] SNS publish failed: " + e.getMessage());
        }
    }
}
