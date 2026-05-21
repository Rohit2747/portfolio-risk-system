package com.incedo.ai_insight_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incedo.ai_insight_service.model.AIInsightRequest;
import com.incedo.ai_insight_service.model.AIInsightResponse;
import com.incedo.ai_insight_service.model.RiskBreachDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * AIInsightEngine — Step 12 Implementation
 *
 * Generates structured AI insights using one of three providers:
 *
 *   MOCK    (default) — Rule-based contextual responses. No API key needed.
 *   OPENAI            — GPT-3.5-turbo. Requires OPENAI_API_KEY env variable.
 *   BEDROCK           — Amazon Bedrock Claude 3. Requires AWS credentials + model access.
 *
 * To switch providers:
 *   Local:  set ai.provider=OPENAI in application.properties + export OPENAI_API_KEY=sk-...
 *   AWS EC2: set ai.provider=BEDROCK, attach IAM role with bedrock:InvokeModel permission
 *
 * All providers return the same AIInsightResponse structure:
 *   { explanation, suggestedAction, severity, disclaimer, clientId, aiProvider }
 */
@Service
public class AIInsightEngine {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String DISCLAIMER =
        "DISCLAIMER: This analysis is AI-generated for informational purposes only. " +
        "It does not constitute financial advice. Consult a qualified financial advisor " +
        "before making investment decisions. Past performance does not guarantee future results.";

    @Value("${ai.provider:MOCK}")
    private String aiProvider;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-3.5-turbo}")
    private String openAiModel;

    @Value("${openai.base.url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${openai.max.tokens:512}")
    private int openAiMaxTokens;

    @Value("${bedrock.model.id:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String bedrockModelId;

    private final BedrockRuntimeClient bedrockClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AIInsightEngine(BedrockRuntimeClient bedrockClient) {
        this.bedrockClient = bedrockClient;
        this.restTemplate  = new RestTemplate();
        this.objectMapper  = new ObjectMapper();
    }

    // ---------------------------------------------------------------
    // Main dispatcher
    // ---------------------------------------------------------------

    public AIInsightResponse generateInsight(AIInsightRequest request) {
        System.out.printf("[AIInsightEngine] Provider=%s | Client=%s | Risk=%s%n",
            aiProvider, request.getClientName(), request.getRiskLevel());

        return switch (aiProvider.toUpperCase()) {
            case "OPENAI"  -> generateWithOpenAI(request);
            case "BEDROCK" -> generateWithBedrock(request);
            default        -> generateWithMock(request);
        };
    }

    // ---------------------------------------------------------------
    // PROVIDER 1: MOCK — Rule-based contextual AI simulation
    // ---------------------------------------------------------------

    private AIInsightResponse generateWithMock(AIInsightRequest request) {
        return new AIInsightResponse(
            request.getClientId(),
            request.getClientName(),
            request.getRiskLevel(),
            buildExplanation(request),
            buildSuggestedAction(request),
            mapSeverity(request.getRiskLevel()),
            DISCLAIMER,
            LocalDateTime.now().format(FORMATTER),
            "MOCK"
        );
    }

    // ---------------------------------------------------------------
    // PROVIDER 2: OPENAI — GPT-3.5-turbo via REST
    // ---------------------------------------------------------------

    /**
     * Calls OpenAI Chat Completions API with the risk prompt.
     * Falls back to MOCK if API key is missing or call fails.
     *
     * To enable:
     *   1. export OPENAI_API_KEY=sk-your-key-here
     *   2. Set ai.provider=OPENAI in application.properties
     */
    private AIInsightResponse generateWithOpenAI(AIInsightRequest request) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            System.out.println("[AIInsightEngine] OPENAI_API_KEY not set — falling back to MOCK");
            return generateWithMock(request);
        }

        try {
            String prompt = buildPrompt(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            // OpenAI Chat Completions request body
            Map<String, Object> body = Map.of(
                "model", openAiModel,
                "messages", List.of(
                    Map.of("role", "system",
                           "content", "You are a portfolio risk analyst for a digital wealth management firm. " +
                                      "Always respond with valid JSON only. No markdown, no explanation outside JSON."),
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens",     openAiMaxTokens,
                "temperature",    0.3,  // low temperature for consistent, factual responses
                "response_format", Map.of("type", "json_object")
            );

            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                openAiBaseUrl + "/chat/completions",
                HttpMethod.POST,
                httpRequest,
                String.class
            );

            return parseOpenAIResponse(response.getBody(), request);

        } catch (Exception e) {
            System.err.println("[AIInsightEngine] OpenAI call failed: " + e.getMessage());
            System.out.println("[AIInsightEngine] Falling back to MOCK provider");
            return generateWithMock(request);
        }
    }

    /**
     * Parses the OpenAI API response JSON into AIInsightResponse.
     *
     * OpenAI response structure:
     * {
     *   "choices": [{
     *     "message": {
     *       "content": "{ \"explanation\": \"...\", \"suggestedAction\": \"...\", ... }"
     *     }
     *   }]
     * }
     */
    private AIInsightResponse parseOpenAIResponse(String responseJson,
                                                   AIInsightRequest request) throws Exception {
        JsonNode root    = objectMapper.readTree(responseJson);
        String content   = root.path("choices").get(0)
                               .path("message").path("content").asText();

        JsonNode insight = objectMapper.readTree(content);

        return new AIInsightResponse(
            request.getClientId(),
            request.getClientName(),
            request.getRiskLevel(),
            insight.path("explanation").asText(buildExplanation(request)),
            insight.path("suggestedAction").asText(buildSuggestedAction(request)),
            insight.path("severity").asText(mapSeverity(request.getRiskLevel())),
            insight.path("disclaimer").asText(DISCLAIMER),
            LocalDateTime.now().format(FORMATTER),
            "OPENAI"
        );
    }

    // ---------------------------------------------------------------
    // PROVIDER 3: BEDROCK — Amazon Claude 3 Sonnet
    // ---------------------------------------------------------------

    /**
     * Calls Amazon Bedrock Anthropic Claude 3 Sonnet model.
     * Falls back to MOCK if credentials are unavailable.
     *
     * To enable:
     *   1. Attach IAM role with bedrock:InvokeModel permission to EC2/ECS
     *      OR configure ~/.aws/credentials on local machine
     *   2. Enable model access in AWS Bedrock console:
     *      → Bedrock → Model access → Enable "Anthropic Claude 3 Sonnet"
     *   3. Set ai.provider=BEDROCK in application.properties
     *      OR export AI_PROVIDER=BEDROCK
     */
    private AIInsightResponse generateWithBedrock(AIInsightRequest request) {
        try {
            String prompt = buildPrompt(request);

            // Claude Messages API format for Bedrock
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens",        1024,
                "temperature",       0.3,
                "messages", List.of(
                    Map.of(
                        "role",    "user",
                        "content", prompt
                    )
                )
            ));

            InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                .modelId(bedrockModelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();

            InvokeModelResponse invokeResponse = bedrockClient.invokeModel(invokeRequest);
            String responseJson = invokeResponse.body().asUtf8String();

            return parseBedrockResponse(responseJson, request);

        } catch (Exception e) {
            System.err.println("[AIInsightEngine] Bedrock call failed: " + e.getMessage());
            System.out.println("[AIInsightEngine] Falling back to MOCK provider");
            return generateWithMock(request);
        }
    }

    /**
     * Parses Bedrock Claude response JSON into AIInsightResponse.
     *
     * Bedrock Claude response structure:
     * {
     *   "content": [{ "type": "text", "text": "{ \"explanation\": \"...\", ... }" }],
     *   "usage": { ... }
     * }
     */
    private AIInsightResponse parseBedrockResponse(String responseJson,
                                                    AIInsightRequest request) throws Exception {
        JsonNode root    = objectMapper.readTree(responseJson);
        String content   = root.path("content").get(0).path("text").asText();

        // Claude may wrap JSON in markdown code blocks — strip them
        content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

        JsonNode insight = objectMapper.readTree(content);

        return new AIInsightResponse(
            request.getClientId(),
            request.getClientName(),
            request.getRiskLevel(),
            insight.path("explanation").asText(buildExplanation(request)),
            insight.path("suggestedAction").asText(buildSuggestedAction(request)),
            insight.path("severity").asText(mapSeverity(request.getRiskLevel())),
            insight.path("disclaimer").asText(DISCLAIMER),
            LocalDateTime.now().format(FORMATTER),
            "BEDROCK"
        );
    }

    // ---------------------------------------------------------------
    // Prompt builder — shared by OpenAI and Bedrock
    // ---------------------------------------------------------------

    /**
     * Builds the structured prompt sent to LLMs.
     *
     * Designed to:
     *   - Avoid financial guarantees (per problem statement requirement)
     *   - Include advisory disclaimer
     *   - Produce structured JSON output
     *   - Be specific to the portfolio's actual breach data
     */
    public String buildPrompt(AIInsightRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a portfolio risk analyst for a digital wealth management firm.\n\n");
        sb.append("Analyze the following portfolio risk data and return ONLY a valid JSON object. ");
        sb.append("No additional text, no markdown, just the JSON.\n\n");

        sb.append("=== PORTFOLIO DATA ===\n");
        sb.append(String.format("Client: %s (ID: %d)%n", request.getClientName(), request.getClientId()));
        sb.append(String.format("Risk Level: %s%n", request.getRiskLevel()));
        sb.append(String.format("Portfolio Value: ₹%.2f%n", request.getPortfolioValue()));
        sb.append(String.format("Daily Change: %.2f%%%n", request.getDailyChangePercent()));

        if (request.getBreaches() != null && !request.getBreaches().isEmpty()) {
            sb.append("\n=== RISK THRESHOLD BREACHES ===\n");
            for (RiskBreachDetail breach : request.getBreaches()) {
                sb.append(String.format("- [%s] %s%n",
                    breach.getBreachType(),
                    breach.getDescription() != null ? breach.getDescription() : "Details unavailable"));
            }
        } else {
            sb.append("\n=== NO BREACHES DETECTED ===\n");
            sb.append("Portfolio is within all risk thresholds.\n");
        }

        sb.append("\n=== REQUIRED JSON OUTPUT ===\n");
        sb.append("Respond with ONLY this JSON structure:\n");
        sb.append("{\n");
        sb.append("  \"explanation\": \"2-3 sentence plain-language explanation of the risk situation\",\n");
        sb.append("  \"suggestedAction\": \"Specific, actionable rebalancing recommendation with stock names if applicable\",\n");
        sb.append("  \"severity\": \"CRITICAL or WARNING or NORMAL\",\n");
        sb.append("  \"disclaimer\": \"Standard advisory disclaimer that this is not financial advice\"\n");
        sb.append("}\n\n");

        sb.append("=== RULES ===\n");
        sb.append("1. Do NOT guarantee any returns or outcomes.\n");
        sb.append("2. Do NOT give specific price targets.\n");
        sb.append("3. Always include an advisory disclaimer in the disclaimer field.\n");
        sb.append("4. Keep explanation factual and based ONLY on the data provided.\n");
        sb.append("5. suggestedAction must reference specific stocks if concentration or drift breaches exist.\n");

        return sb.toString();
    }

    // ---------------------------------------------------------------
    // MOCK: Rule-based explanation and action builders
    // ---------------------------------------------------------------

    private String buildExplanation(AIInsightRequest request) {
        List<RiskBreachDetail> breaches = request.getBreaches();

        if (breaches == null || breaches.isEmpty()) {
            return String.format(
                "Portfolio %s is within all risk parameters. " +
                "All holdings maintain target allocation balance with no concentration issues. " +
                "Daily performance of %.2f%% is within acceptable bounds.",
                request.getClientName(),
                request.getDailyChangePercent()
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s has been flagged with %s risk level. ",
            request.getClientName(), request.getRiskLevel()));

        boolean addedConcentration = false, addedDrift = false, addedDrop = false;

        for (RiskBreachDetail breach : breaches) {
            switch (breach.getBreachType()) {
                case "CONCENTRATION_RISK" -> {
                    if (!addedConcentration) {
                        sb.append(String.format(
                            "Single-stock concentration in %s has reached %.1f%%, " +
                            "breaching the 20%% maximum. This creates significant exposure " +
                            "to %s-specific market events. ",
                            breach.getAffectedSymbol(),
                            breach.getActualValue(),
                            breach.getAffectedSymbol()
                        ));
                        addedConcentration = true;
                    }
                }
                case "ALLOCATION_DRIFT" -> {
                    if (!addedDrift) {
                        sb.append(String.format(
                            "Portfolio allocation in %s has drifted %.1f%% from the target " +
                            "(actual: %.1f%%, target: %.1f%%), typically due to asymmetric " +
                            "price appreciation. ",
                            breach.getAffectedSymbol(),
                            Math.abs(breach.getActualValue() - breach.getThresholdValue()),
                            breach.getActualValue(),
                            breach.getThresholdValue()
                        ));
                        addedDrift = true;
                    }
                }
                case "DAILY_DROP" -> {
                    if (!addedDrop) {
                        sb.append(String.format(
                            "Portfolio declined %.1f%% from today's open, " +
                            "exceeding the 3%% daily drawdown threshold. " +
                            "Broad market volatility is contributing to this decline. ",
                            Math.abs(request.getDailyChangePercent())
                        ));
                        addedDrop = true;
                    }
                }
            }
        }

        return sb.toString().trim();
    }

    private String buildSuggestedAction(AIInsightRequest request) {
        List<RiskBreachDetail> breaches = request.getBreaches();

        if (breaches == null || breaches.isEmpty()) {
            return "Maintain current allocation. Conduct next scheduled portfolio review in 30 days.";
        }

        StringBuilder sb = new StringBuilder();

        breaches.stream().filter(b -> "CONCENTRATION_RISK".equals(b.getBreachType()))
            .findFirst().ifPresent(b -> sb.append(String.format(
                "Reduce %s position by approximately %.0f%% to bring concentration below 20%%. " +
                "Redistribute proceeds to underweight diversified holdings. ",
                b.getAffectedSymbol(), Math.max(1.0, b.getActualValue() - 20.0)
            )));

        if (breaches.stream().anyMatch(b -> "ALLOCATION_DRIFT".equals(b.getBreachType()))) {
            sb.append("Rebalance overweight positions to restore target allocation model. " +
                "Trim outperforming holdings and reinvest in lagging sectors. ");
        }

        if (breaches.stream().anyMatch(b -> "DAILY_DROP".equals(b.getBreachType()))) {
            sb.append("Review market exposure and consider adding defensive assets " +
                "(bonds, REITs, or cash). Avoid panic-selling; assess fundamentals before acting. ");
        }

        return sb.toString().trim();
    }

    private String mapSeverity(String riskLevel) {
        if (riskLevel == null) return "NORMAL";
        return switch (riskLevel.toUpperCase()) {
            case "HIGH"   -> "CRITICAL";
            case "MEDIUM" -> "WARNING";
            default       -> "NORMAL";
        };
    }
}
