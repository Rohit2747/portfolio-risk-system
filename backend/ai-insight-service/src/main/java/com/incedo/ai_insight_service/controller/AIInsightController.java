package com.incedo.ai_insight_service.controller;

import com.incedo.ai_insight_service.model.AIInsightRequest;
import com.incedo.ai_insight_service.model.AIInsightResponse;
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
 * Endpoints:
 *   POST /ai-insight              → generate insight from request body
 *   GET  /ai-insight/portfolio/{clientId} → fetch risk data from Risk Service and generate insight
 *   GET  /ai-insight/all-breached → generate insights for all breached portfolios
 *   GET  /ai-insight/health       → health check
 *
 * Event-driven flow:
 *   LOCAL: Called via REST from Risk Service or frontend
 *   AWS:   SQS consumer triggers generateInsight() for each received RiskAlertEvent
 */
@RestController
@CrossOrigin(origins = "*")
public class AIInsightController {

    private final AIInsightEngine aiInsightEngine;
    private final RestTemplate restTemplate;

    @Value("${risk.analysis.service.url:http://localhost:8082}")
    private String riskAnalysisServiceUrl;

    public AIInsightController(AIInsightEngine aiInsightEngine) {
        this.aiInsightEngine = aiInsightEngine;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Health check
     */
    @GetMapping("/hello")
    public String hello() {
        return "AI Insight Service is running on port 8083";
    }

    /**
     * Generate AI insight from a risk event payload.
     *
     * POST /ai-insight
     * Body: AIInsightRequest JSON
     *
     * This is the endpoint that the Risk Service calls (or SQS consumer invokes).
     */
    @PostMapping("/ai-insight")
    public AIInsightResponse generateInsight(@RequestBody AIInsightRequest request) {
        System.out.printf("[AIInsightService] Generating insight for %s (Risk: %s)%n",
            request.getClientName(), request.getRiskLevel());
        return aiInsightEngine.generateInsight(request);
    }

    /**
     * Fetch risk data for a specific client from Risk Service
     * and generate an AI insight for it.
     *
     * GET /ai-insight/portfolio/{clientId}
     */
    @GetMapping("/ai-insight/portfolio/{clientId}")
    public AIInsightResponse getInsightForClient(@PathVariable int clientId) {
        // Fetch risk analysis from Risk Service
        Map<String, Object> riskData = fetchRiskForClient(clientId);
        if (riskData == null) {
            throw new RuntimeException("Could not fetch risk data for clientId: " + clientId);
        }

        AIInsightRequest request = mapRiskDataToRequest(riskData);
        return aiInsightEngine.generateInsight(request);
    }

    /**
     * Fetches all breached portfolios from Risk Service
     * and generates AI insights for each one.
     *
     * GET /ai-insight/all-breached
     *
     * This simulates the SQS batch-processing pattern:
     * in AWS, SQS delivers messages one-by-one; here we poll and process all at once.
     */
    @GetMapping("/ai-insight/all-breached")
    public List<AIInsightResponse> getAllBreachedInsights() {
        List<Map<String, Object>> breachedPortfolios = fetchBreachedPortfolios();

        return breachedPortfolios.stream()
            .map(this::mapRiskDataToRequest)
            .map(aiInsightEngine::generateInsight)
            .toList();
    }

    /**
     * Returns the prompt that would be sent to an LLM.
     * Useful for demo and transparency.
     *
     * GET /ai-insight/prompt/{clientId}
     */
    @GetMapping("/ai-insight/prompt/{clientId}")
    public Map<String, String> getPromptForClient(@PathVariable int clientId) {
        Map<String, Object> riskData = fetchRiskForClient(clientId);
        AIInsightRequest request = mapRiskDataToRequest(riskData);
        String prompt = aiInsightEngine.buildPrompt(request);
        return Map.of(
            "clientId", String.valueOf(clientId),
            "aiProvider", "Would use: Amazon Bedrock (Claude) or OpenAI GPT",
            "prompt", prompt
        );
    }

    // ---------------------------------------------------------------
    // Private: fetch from Risk Analysis Service
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
            System.err.println("[AIInsightService] ERROR fetching risk for client " + clientId + ": " + e.getMessage());
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
            System.err.println("[AIInsightService] ERROR fetching breached portfolios: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Maps raw risk JSON map to AIInsightRequest.
     */
    @SuppressWarnings("unchecked")
    private AIInsightRequest mapRiskDataToRequest(Map<String, Object> data) {
        AIInsightRequest request = new AIInsightRequest();
        request.setClientId(data.containsKey("clientId") ? (int) data.get("clientId") : 0);
        request.setClientName(data.getOrDefault("clientName", "Unknown").toString());
        request.setRiskLevel(data.getOrDefault("riskLevel", "LOW").toString());
        request.setPortfolioValue(
            data.containsKey("totalPortfolioValue")
                ? ((Number) data.get("totalPortfolioValue")).doubleValue() : 0.0
        );
        request.setDailyChangePercent(
            data.containsKey("dailyChangePercent")
                ? ((Number) data.get("dailyChangePercent")).doubleValue() : 0.0
        );
        request.setTimestamp(data.getOrDefault("alertTimestamp", "").toString());
        // Breaches will be null here (simplified mapping); full mapping done when posting directly
        return request;
    }
}
