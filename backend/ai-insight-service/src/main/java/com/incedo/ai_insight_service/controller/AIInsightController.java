package com.incedo.ai_insight_service.controller;

import com.incedo.ai_insight_service.events.SqsRiskEventConsumer;
import com.incedo.ai_insight_service.model.AIInsightRequest;
import com.incedo.ai_insight_service.model.AIInsightResponse;
import com.incedo.ai_insight_service.model.RiskBreachDetail;
import com.incedo.ai_insight_service.service.AIInsightEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * AIInsightController
 *
 * Exposes REST APIs for AI-generated portfolio risk explanations.
 *
 * Two consumption modes:
 *
 *   LOCAL (aws.enabled=false):
 *     - REST endpoints called directly by frontend or Risk Service
 *     - /ai-insight/portfolio/{id} → fetches from Risk Service → generates insight
 *     - /ai-insight/all-breached   → batch insight generation
 *
 *   AWS (aws.enabled=true):
 *     - SqsRiskEventConsumer polls queue and auto-processes events
 *     - /ai-insight/cached → returns pre-generated insights from SQS cache
 *     - REST endpoints still available as fallback
 */
@RestController
@CrossOrigin(origins = "*")
public class AIInsightController {

    private final AIInsightEngine aiInsightEngine;
    private final SqsRiskEventConsumer sqsConsumer;
    private final RestTemplate restTemplate;

    @Value("${risk.analysis.service.url:http://localhost:8082}")
    private String riskAnalysisServiceUrl;

    @Value("${aws.enabled:false}")
    private boolean awsEnabled;

    public AIInsightController(AIInsightEngine aiInsightEngine,
                                SqsRiskEventConsumer sqsConsumer) {
        this.aiInsightEngine = aiInsightEngine;
        this.sqsConsumer     = sqsConsumer;
        this.restTemplate    = new RestTemplate();
    }

    // ---------------------------------------------------------------
    // Health check
    // ---------------------------------------------------------------

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        return Map.of(
            "service",    "AI Insight Service",
            "port",       8083,
            "status",     "running",
            "aiProvider", System.getProperty("ai.provider", "MOCK"),
            "awsEnabled", awsEnabled
        );
    }

    // ---------------------------------------------------------------
    // Core: Generate from request body
    // ---------------------------------------------------------------

    /**
     * POST /ai-insight
     *
     * Generates AI insight from a RiskAlertEvent payload.
     * This is what Risk Service calls (or SQS consumer invokes internally).
     *
     * Example body:
     * {
     *   "clientId": 78,
     *   "clientName": "Client-078",
     *   "riskLevel": "HIGH",
     *   "portfolioValue": 425000.00,
     *   "dailyChangePercent": -3.8,
     *   "breaches": [...]
     * }
     */
    @PostMapping("/ai-insight")
    public AIInsightResponse generateInsight(@RequestBody AIInsightRequest request) {
        System.out.printf("[AIInsightService] POST request: client=%s level=%s%n",
            request.getClientName(), request.getRiskLevel());
        return aiInsightEngine.generateInsight(request);
    }

    // ---------------------------------------------------------------
    // REST: Fetch risk data and generate insight
    // ---------------------------------------------------------------

    /**
     * GET /ai-insight/portfolio/{clientId}
     *
     * In AWS mode: returns cached insight from SQS processing.
     * In local mode: fetches risk data from Risk Service and generates fresh.
     */
    @GetMapping("/ai-insight/portfolio/{clientId}")
    public AIInsightResponse getInsightForClient(@PathVariable int clientId) {

        // In AWS mode: return SQS-processed insight if available
        if (awsEnabled) {
            AIInsightResponse cached = sqsConsumer.getCachedInsight(clientId);
            if (cached != null) {
                System.out.printf("[AIInsightService] Cache hit for clientId=%d%n", clientId);
                return cached;
            }
        }

        // Fallback: generate fresh insight via REST call to Risk Service
        Map<String, Object> riskData = fetchRiskForClient(clientId);
        if (riskData == null) {
            throw new RuntimeException("Could not fetch risk data for clientId: " + clientId);
        }
        AIInsightRequest request = mapRiskDataToRequest(riskData);
        return aiInsightEngine.generateInsight(request);
    }

    /**
     * GET /ai-insight/all-breached
     *
     * Generates insights for all portfolios with active risk breaches.
     * In AWS mode: also returns insights pre-generated via SQS.
     * In local mode: fetches from Risk Service and generates batch.
     */
    @GetMapping("/ai-insight/all-breached")
    public List<AIInsightResponse> getAllBreachedInsights() {
        List<Map<String, Object>> breachedPortfolios = fetchBreachedPortfolios();

        return breachedPortfolios.stream()
            .map(this::mapRiskDataToRequest)
            .map(aiInsightEngine::generateInsight)
            .toList();
    }

    // ---------------------------------------------------------------
    // AWS: SQS Cache endpoints
    // ---------------------------------------------------------------

    /**
     * GET /ai-insight/cached
     *
     * Returns all AI insights currently in the SQS processing cache.
     * These were generated when SqsRiskEventConsumer processed SQS messages.
     *
     * In AWS mode: this is the primary data source.
     * In local mode: cache will be empty (populated via POST /ai-insight instead).
     */
    @GetMapping("/ai-insight/cached")
    public Map<String, Object> getCachedInsights() {
        List<AIInsightResponse> insights = sqsConsumer.getAllCachedInsights();
        return Map.of(
            "awsEnabled",    awsEnabled,
            "cacheSize",     insights.size(),
            "insights",      insights,
            "note", awsEnabled
                ? "Auto-populated by SQS consumer"
                : "Empty in local mode — use POST /ai-insight or GET /ai-insight/all-breached"
        );
    }

    /**
     * GET /ai-insight/events/log
     *
     * Returns the SQS processing audit log.
     * Shows which events were consumed and when.
     */
    @GetMapping("/ai-insight/events/log")
    public Map<String, Object> getEventLog() {
        List<String> log = sqsConsumer.getProcessedEventLog();
        return Map.of(
            "awsEnabled",  awsEnabled,
            "eventsCount", log.size(),
            "events",      log,
            "sqsQueueUrl", awsEnabled ? "configured" : "disabled (aws.enabled=false)"
        );
    }

    // ---------------------------------------------------------------
    // Transparency: show prompt
    // ---------------------------------------------------------------

    /**
     * GET /ai-insight/prompt/{clientId}
     *
     * Shows the exact prompt that would be sent to Amazon Bedrock / OpenAI.
     * Useful for demo and evaluator review.
     */
    @GetMapping("/ai-insight/prompt/{clientId}")
    public Map<String, Object> getPromptForClient(@PathVariable int clientId) {
        Map<String, Object> riskData = fetchRiskForClient(clientId);
        AIInsightRequest request = riskData != null
            ? mapRiskDataToRequest(riskData)
            : buildFallbackRequest(clientId);

        String prompt = aiInsightEngine.buildPrompt(request);

        return Map.of(
            "clientId",        clientId,
            "aiProviderReady", "Amazon Bedrock (Claude 3 Sonnet) OR OpenAI GPT-3.5-turbo",
            "modelId",         "anthropic.claude-3-sonnet-20240229-v1:0",
            "prompt",          prompt,
            "disclaimer",      "This prompt is designed to produce structured JSON output with advisory disclaimers"
        );
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchRiskForClient(int clientId) {
        try {
            return restTemplate.exchange(
                riskAnalysisServiceUrl + "/risk-analysis/" + clientId,
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();
        } catch (Exception e) {
            System.err.println("[AIInsightService] Cannot reach Risk Service for client "
                + clientId + ": " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchBreachedPortfolios() {
        try {
            return restTemplate.exchange(
                riskAnalysisServiceUrl + "/risk-analysis/breached",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
        } catch (Exception e) {
            System.err.println("[AIInsightService] Cannot reach Risk Service: " + e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private AIInsightRequest mapRiskDataToRequest(Map<String, Object> data) {
        AIInsightRequest request = new AIInsightRequest();
        request.setClientId(data.containsKey("clientId")
            ? ((Number) data.get("clientId")).intValue() : 0);
        request.setClientName(data.getOrDefault("clientName", "Unknown").toString());
        request.setRiskLevel(data.getOrDefault("riskLevel", "LOW").toString());
        request.setPortfolioValue(data.containsKey("totalPortfolioValue")
            ? ((Number) data.get("totalPortfolioValue")).doubleValue() : 0.0);
        request.setDailyChangePercent(data.containsKey("dailyChangePercent")
            ? ((Number) data.get("dailyChangePercent")).doubleValue() : 0.0);
        request.setTimestamp(data.getOrDefault("alertTimestamp", "").toString());

        // Parse breaches list so AI engine can give specific explanations
        List<RiskBreachDetail> breaches = new java.util.ArrayList<>();
        if (data.containsKey("breaches") && data.get("breaches") instanceof List<?> rawBreaches) {
            for (Object rawBreach : rawBreaches) {
                if (rawBreach instanceof Map<?, ?> rawMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> bMap = (Map<String, Object>) rawMap;
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

    private AIInsightRequest buildFallbackRequest(int clientId) {
        AIInsightRequest r = new AIInsightRequest();
        r.setClientId(clientId);
        r.setClientName("Client-" + String.format("%03d", clientId));
        r.setRiskLevel("UNKNOWN");
        r.setBreaches(List.of());
        return r;
    }
}
