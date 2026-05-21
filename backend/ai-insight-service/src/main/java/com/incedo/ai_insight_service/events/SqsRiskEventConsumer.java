package com.incedo.ai_insight_service.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.incedo.ai_insight_service.model.AIInsightRequest;
import com.incedo.ai_insight_service.model.AIInsightResponse;
import com.incedo.ai_insight_service.model.RiskBreachDetail;
import com.incedo.ai_insight_service.service.AIInsightEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SqsRiskEventConsumer — Step 11 Implementation
 *
 * Polls Amazon SQS for RiskThresholdBreached events published by Risk Analysis Service.
 * For each received message:
 *   1. Parses the JSON payload into AIInsightRequest
 *   2. Calls AIInsightEngine.generateInsight()
 *   3. Stores the result in an in-memory cache (replaces DynamoDB for now)
 *   4. Deletes the message from SQS (acknowledgement)
 *
 * This implements the actual pub-subscribe pattern:
 *   SNS (publisher) → SQS (queue) → this consumer (subscriber)
 *
 * Local mode (aws.enabled=false):
 *   The SQS poller is disabled. REST endpoints are used instead.
 *   The @Scheduled method checks aws.enabled before making any AWS call.
 *
 * AWS Setup required:
 *   1. Create SQS queue:   aws sqs create-queue --queue-name ai-insight-queue
 *   2. Subscribe to SNS:   aws sns subscribe --topic-arn <risk-arn>
 *                              --protocol sqs --notification-endpoint <queue-arn>
 *   3. Set SQS_RISK_QUEUE_URL environment variable
 */
@Component
public class SqsRiskEventConsumer {

    @Value("${aws.enabled:false}")
    private boolean awsEnabled;

    @Value("${sqs.queue.url:https://sqs.us-east-1.amazonaws.com/ACCOUNT_ID/ai-insight-queue}")
    private String sqsQueueUrl;

    private final SqsClient sqsClient;
    private final AIInsightEngine aiInsightEngine;
    private final ObjectMapper objectMapper;

    // In-memory store of recently generated insights (keyed by clientId)
    // In production: replace with DynamoDB or ElastiCache
    private final java.util.concurrent.ConcurrentHashMap<Integer, AIInsightResponse> insightCache
        = new java.util.concurrent.ConcurrentHashMap<>();

    // Audit log of all processed events
    private final ConcurrentLinkedQueue<String> processedEvents = new ConcurrentLinkedQueue<>();

    public SqsRiskEventConsumer(SqsClient sqsClient, AIInsightEngine aiInsightEngine) {
        this.sqsClient      = sqsClient;
        this.aiInsightEngine = aiInsightEngine;
        this.objectMapper   = new ObjectMapper();
    }

    // ---------------------------------------------------------------
    // SQS Poll Loop — runs every 5 seconds
    // ---------------------------------------------------------------

    /**
     * Polls SQS queue for incoming RiskThresholdBreached messages.
     *
     * Only runs when aws.enabled=true.
     * In local mode, this method exits immediately without making AWS calls.
     */
    @Scheduled(fixedDelayString = "${sqs.poll.interval.ms:5000}")
    public void pollQueue() {
        if (!awsEnabled) {
            // Local mode: skip SQS polling entirely
            return;
        }

        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(sqsQueueUrl)
                .maxNumberOfMessages(10)      // process up to 10 messages per poll
                .waitTimeSeconds(5)            // long polling (reduces empty responses)
                .messageAttributeNames("All")
                .build();

            List<Message> messages = sqsClient.receiveMessage(request).messages();

            if (!messages.isEmpty()) {
                System.out.printf("[SQS] Received %d message(s) from ai-insight-queue%n",
                    messages.size());
            }

            for (Message message : messages) {
                processMessage(message);
            }

        } catch (Exception e) {
            System.err.println("[SqsConsumer] Poll error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Message Processing
    // ---------------------------------------------------------------

    /**
     * Processes a single SQS message:
     *   1. Extract body (SNS wraps message in an outer envelope)
     *   2. Parse RiskAlertEvent JSON
     *   3. Generate AI insight
     *   4. Cache result
     *   5. Acknowledge (delete) from SQS
     */
    @SuppressWarnings("unchecked")
    private void processMessage(Message message) {
        try {
            String body = message.body();

            // SNS delivers messages wrapped in a JSON envelope:
            // { "Type": "Notification", "Message": "<actual JSON string>", ... }
            // We need to extract the inner "Message" field.
            Map<String, Object> snsEnvelope = objectMapper.readValue(body, Map.class);
            String actualPayload;

            if (snsEnvelope.containsKey("Message")) {
                // SNS envelope — extract inner message
                actualPayload = snsEnvelope.get("Message").toString();
            } else {
                // Direct SQS message (not via SNS) — use body directly
                actualPayload = body;
            }

            // Parse the RiskAlertEvent payload
            Map<String, Object> eventData = objectMapper.readValue(actualPayload, Map.class);
            AIInsightRequest request = mapEventToRequest(eventData);

            System.out.printf("[SQS] Processing: client=%s level=%s breaches=%d%n",
                request.getClientName(),
                request.getRiskLevel(),
                request.getBreaches() != null ? request.getBreaches().size() : 0
            );

            // Generate AI insight
            AIInsightResponse insight = aiInsightEngine.generateInsight(request);

            // Cache the result
            insightCache.put(request.getClientId(), insight);
            processedEvents.offer(String.format(
                "[%s] clientId=%d level=%s severity=%s",
                insight.getGeneratedAt(), insight.getClientId(),
                insight.getRiskLevel(), insight.getSeverity()
            ));

            System.out.printf("[SQS] ✅ Insight generated: client=%s severity=%s%n",
                insight.getClientName(), insight.getSeverity());

            // Acknowledge message — delete from queue
            deleteMessage(message.receiptHandle());

        } catch (Exception e) {
            System.err.println("[SqsConsumer] Message processing failed: " + e.getMessage());
            // Do NOT delete — message will return to queue for retry after visibility timeout
        }
    }

    private void deleteMessage(String receiptHandle) {
        try {
            sqsClient.deleteMessage(r -> r
                .queueUrl(sqsQueueUrl)
                .receiptHandle(receiptHandle));
        } catch (Exception e) {
            System.err.println("[SqsConsumer] Delete failed: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Cache Access (used by REST controller)
    // ---------------------------------------------------------------

    /** Returns cached AI insight for a client, or null if not yet generated. */
    public AIInsightResponse getCachedInsight(int clientId) {
        return insightCache.get(clientId);
    }

    /** Returns all cached insights. */
    public List<AIInsightResponse> getAllCachedInsights() {
        return new ArrayList<>(insightCache.values());
    }

    /** Returns recent SQS processing audit log. */
    public List<String> getProcessedEventLog() {
        return new ArrayList<>(processedEvents);
    }

    // ---------------------------------------------------------------
    // Event Mapping
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private AIInsightRequest mapEventToRequest(Map<String, Object> data) {
        AIInsightRequest request = new AIInsightRequest();

        request.setClientId(data.containsKey("clientId")
            ? ((Number) data.get("clientId")).intValue() : 0);
        request.setClientName(data.getOrDefault("clientName", "Unknown").toString());
        request.setRiskLevel(data.getOrDefault("riskLevel", "LOW").toString());
        request.setPortfolioValue(data.containsKey("portfolioValue")
            ? ((Number) data.get("portfolioValue")).doubleValue() : 0.0);
        request.setDailyChangePercent(data.containsKey("dailyChangePercent")
            ? ((Number) data.get("dailyChangePercent")).doubleValue() : 0.0);
        request.setTimestamp(data.getOrDefault("timestamp", "").toString());

        // Parse breaches list
        List<RiskBreachDetail> breaches = new ArrayList<>();
        if (data.containsKey("breaches") && data.get("breaches") instanceof List<?> rawBreaches) {
            for (Object rawBreach : rawBreaches) {
                if (rawBreach instanceof Map<?, ?> bMap) {
                    RiskBreachDetail breach = new RiskBreachDetail();
                    breach.setBreachType(bMap.getOrDefault("breachType", "").toString());
                    breach.setAffectedSymbol(
                        bMap.containsKey("affectedSymbol") && bMap.get("affectedSymbol") != null
                            ? bMap.get("affectedSymbol").toString() : null);
                    breach.setActualValue(bMap.containsKey("actualValue")
                        ? ((Number) bMap.get("actualValue")).doubleValue() : 0.0);
                    breach.setThresholdValue(bMap.containsKey("thresholdValue")
                        ? ((Number) bMap.get("thresholdValue")).doubleValue() : 0.0);
                    breach.setDescription(bMap.getOrDefault("description", "").toString());
                    breaches.add(breach);
                }
            }
        }
        request.setBreaches(breaches);

        return request;
    }
}
