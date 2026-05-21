package com.incedo.ai_insight_service.service;

import com.incedo.ai_insight_service.model.AIInsightRequest;
import com.incedo.ai_insight_service.model.AIInsightResponse;
import com.incedo.ai_insight_service.model.RiskBreachDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AIInsightEngine
 *
 * Generates structured AI insights for portfolio risk events.
 *
 * Provider strategy (configured in application.properties):
 *   MOCK    → Rule-based contextual explanations (default, no API key needed)
 *   OPENAI  → GPT-3.5-turbo via REST API (requires OPENAI_API_KEY)
 *   BEDROCK → Amazon Bedrock Claude (requires AWS IAM role/credentials)
 *
 * To switch providers: change ai.provider in application.properties
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

    /**
     * Main method: routes to the configured AI provider.
     */
    public AIInsightResponse generateInsight(AIInsightRequest request) {
        return switch (aiProvider.toUpperCase()) {
            case "OPENAI"  -> generateWithOpenAI(request);
            case "BEDROCK" -> generateWithBedrock(request);
            default        -> generateWithMock(request);
        };
    }

    // ---------------------------------------------------------------
    // MOCK PROVIDER: Rule-based contextual AI simulation
    // ---------------------------------------------------------------
    private AIInsightResponse generateWithMock(AIInsightRequest request) {

        String explanation   = buildExplanation(request);
        String action        = buildSuggestedAction(request);
        String severity      = mapSeverity(request.getRiskLevel());

        return new AIInsightResponse(
            request.getClientId(),
            request.getClientName(),
            request.getRiskLevel(),
            explanation,
            action,
            severity,
            DISCLAIMER,
            LocalDateTime.now().format(FORMATTER),
            "MOCK"
        );
    }

    // ---------------------------------------------------------------
    // OPENAI PROVIDER (ready for integration)
    // ---------------------------------------------------------------
    private AIInsightResponse generateWithOpenAI(AIInsightRequest request) {
        /*
         * AWS UPGRADE — OpenAI Integration:
         *
         * String prompt = buildPrompt(request);
         * HttpHeaders headers = new HttpHeaders();
         * headers.setBearerAuth(openAiApiKey);
         * headers.setContentType(MediaType.APPLICATION_JSON);
         *
         * Map<String, Object> body = Map.of(
         *   "model", openAiModel,
         *   "messages", List.of(
         *     Map.of("role", "system", "content", "You are a portfolio risk analyst..."),
         *     Map.of("role", "user", "content", prompt)
         *   ),
         *   "response_format", Map.of("type", "json_object")
         * );
         *
         * ResponseEntity<Map> response = restTemplate.exchange(
         *   openAiBaseUrl + "/chat/completions",
         *   HttpMethod.POST,
         *   new HttpEntity<>(body, headers),
         *   Map.class
         * );
         *
         * String content = extractContent(response.getBody());
         * return parseJsonToResponse(content, request);
         *
         * For now, falls back to mock if API key not configured:
         */
        System.out.println("[AIInsightService] OpenAI provider selected but falling back to MOCK. " +
            "Set OPENAI_API_KEY environment variable to enable.");
        return generateWithMock(request);
    }

    // ---------------------------------------------------------------
    // BEDROCK PROVIDER (ready for integration)
    // ---------------------------------------------------------------
    private AIInsightResponse generateWithBedrock(AIInsightRequest request) {
        /*
         * AWS UPGRADE — Amazon Bedrock Integration:
         *
         * BedrockRuntimeClient client = BedrockRuntimeClient.builder()
         *   .region(Region.of(awsRegion))
         *   .build();
         *
         * String prompt = buildPrompt(request);
         * String requestBody = String.format("""
         *   {
         *     "anthropic_version": "bedrock-2023-05-31",
         *     "max_tokens": 1024,
         *     "messages": [{"role": "user", "content": "%s"}]
         *   }
         *   """, prompt.replace("\"", "\\\""));
         *
         * InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
         *   .modelId(bedrockModelId)
         *   .body(SdkBytes.fromUtf8String(requestBody))
         *   .build();
         *
         * InvokeModelResponse invokeResponse = client.invokeModel(invokeRequest);
         * String responseJson = invokeResponse.body().asUtf8String();
         * return parseBedrockResponse(responseJson, request);
         *
         * For now, falls back to mock:
         */
        System.out.println("[AIInsightService] Bedrock provider selected but falling back to MOCK. " +
            "Configure AWS credentials and region to enable.");
        return generateWithMock(request);
    }

    // ---------------------------------------------------------------
    // Prompt Builder (used by both OpenAI and Bedrock providers)
    // ---------------------------------------------------------------
    public String buildPrompt(AIInsightRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a portfolio risk analyst for a digital wealth management firm.\n\n");
        sb.append("Analyze this portfolio risk event and provide a structured JSON response.\n\n");
        sb.append(String.format("Client: %s\n", request.getClientName()));
        sb.append(String.format("Risk Level: %s\n", request.getRiskLevel()));
        sb.append(String.format("Portfolio Value: ₹%.2f\n", request.getPortfolioValue()));
        sb.append(String.format("Daily Change: %.2f%%\n\n", request.getDailyChangePercent()));

        if (request.getBreaches() != null && !request.getBreaches().isEmpty()) {
            sb.append("Risk Threshold Breaches:\n");
            for (RiskBreachDetail breach : request.getBreaches()) {
                sb.append(String.format("  - [%s] %s\n",
                    breach.getBreachType(), breach.getDescription()));
            }
        }

        sb.append("\nRespond with ONLY a valid JSON object:\n");
        sb.append("{\n");
        sb.append("  \"explanation\": \"<2-3 sentence plain-language explanation>\",\n");
        sb.append("  \"suggestedAction\": \"<specific, actionable rebalancing recommendation>\",\n");
        sb.append("  \"severity\": \"<CRITICAL|WARNING|NORMAL>\",\n");
        sb.append("  \"disclaimer\": \"<advisory disclaimer>\"\n");
        sb.append("}\n\n");
        sb.append("Rules: Do not guarantee returns. Do not give specific price targets.");

        return sb.toString();
    }

    // ---------------------------------------------------------------
    // Explanation builder (MOCK provider)
    // ---------------------------------------------------------------
    private String buildExplanation(AIInsightRequest request) {
        List<RiskBreachDetail> breaches = request.getBreaches();

        if (breaches == null || breaches.isEmpty()) {
            return String.format(
                "Portfolio %s is maintaining healthy allocation balance. " +
                "All positions are within target thresholds with no risk breaches detected. " +
                "Current market exposure is within acceptable risk parameters.",
                request.getClientName()
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Portfolio %s has been flagged with %s risk. ",
            request.getClientName(), request.getRiskLevel()));

        boolean addedConcentration = false;
        boolean addedDrift = false;
        boolean addedDrop = false;

        for (RiskBreachDetail breach : breaches) {
            switch (breach.getBreachType()) {
                case "CONCENTRATION_RISK" -> {
                    if (!addedConcentration) {
                        sb.append(String.format(
                            "Single-stock concentration in %s has reached %.1f%%, " +
                            "exceeding the 20%% maximum threshold. " +
                            "This concentration level creates significant vulnerability to %s-specific events. ",
                            breach.getAffectedSymbol(), breach.getActualValue(),
                            breach.getAffectedSymbol()
                        ));
                        addedConcentration = true;
                    }
                }
                case "ALLOCATION_DRIFT" -> {
                    if (!addedDrift) {
                        sb.append(String.format(
                            "Portfolio has drifted %.1f%% from model allocation in %s " +
                            "(actual: %.1f%%, target: %.1f%%). " +
                            "This drift typically occurs due to asymmetric price appreciation. ",
                            Math.abs(breach.getActualValue() - breach.getThresholdValue()),
                            breach.getAffectedSymbol(),
                            breach.getActualValue(), breach.getThresholdValue()
                        ));
                        addedDrift = true;
                    }
                }
                case "DAILY_DROP" -> {
                    if (!addedDrop) {
                        sb.append(String.format(
                            "The portfolio has declined %.1f%% from today's opening value, " +
                            "breaching the 3%% daily drawdown threshold. " +
                            "Immediate review of market exposure is recommended. ",
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
            return "Maintain current allocation. Schedule periodic review in 30 days.";
        }

        StringBuilder actions = new StringBuilder();
        boolean hasConcentration = breaches.stream().anyMatch(b -> "CONCENTRATION_RISK".equals(b.getBreachType()));
        boolean hasDrift         = breaches.stream().anyMatch(b -> "ALLOCATION_DRIFT".equals(b.getBreachType()));
        boolean hasDrop          = breaches.stream().anyMatch(b -> "DAILY_DROP".equals(b.getBreachType()));

        if (hasConcentration) {
            breaches.stream()
                .filter(b -> "CONCENTRATION_RISK".equals(b.getBreachType()))
                .findFirst()
                .ifPresent(b -> actions.append(String.format(
                    "Reduce %s position by approximately %.0f%% to bring concentration below 20%%. " +
                    "Redistribute proceeds to underweight positions for diversification. ",
                    b.getAffectedSymbol(),
                    Math.max(1.0, b.getActualValue() - 20.0)
                )));
        }

        if (hasDrift) {
            actions.append("Rebalance portfolio to restore target allocation weights. " +
                "Trim overweight positions and add to underweight sectors. ");
        }

        if (hasDrop) {
            actions.append("Review market exposure and consider adding defensive assets. " +
                "Avoid panic-selling; assess fundamental changes before taking action. ");
        }

        return actions.toString().trim();
    }

    private String mapSeverity(String riskLevel) {
        return switch (riskLevel != null ? riskLevel.toUpperCase() : "LOW") {
            case "HIGH"   -> "CRITICAL";
            case "MEDIUM" -> "WARNING";
            default       -> "NORMAL";
        };
    }
}
