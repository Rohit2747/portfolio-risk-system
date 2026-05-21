package com.incedo.risk_analysis_service.ai;

import com.incedo.risk_analysis_service.model.RiskBreachDetail;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AIRecommendationService
 *
 * Generates structured AI insights for portfolio risk breaches.
 *
 * CURRENT IMPLEMENTATION:
 *   Rule-based contextual explanations using breach data.
 *   Outputs are structured, specific, and context-aware.
 *   (This is Option B from the problem statement: Rule + AI Hybrid)
 *
 * AWS UPGRADE PATH (Amazon Bedrock):
 *   Replace generateInsight() body with a Bedrock API call:
 *
 *   BedrockRuntimeClient client = BedrockRuntimeClient.create();
 *   String prompt = buildPrompt(clientName, riskLevel, breaches, portfolioValue, dailyChange);
 *   InvokeModelRequest request = InvokeModelRequest.builder()
 *       .modelId("anthropic.claude-3-sonnet-20240229-v1:0")
 *       .body(SdkBytes.fromUtf8String(prompt))
 *       .build();
 *   String response = client.invokeModel(request).body().asUtf8String();
 *   return parseBedrockResponse(response);
 */
@Service
public class AIRecommendationService {

    /**
     * Generates a structured AI insight for a portfolio with risk breaches.
     *
     * @param clientName      client identifier
     * @param riskLevel       HIGH | MEDIUM | LOW
     * @param breaches        list of detected breach details
     * @param portfolioValue  current total value
     * @param dailyChange     daily % change (negative = loss)
     * @return AIInsight object with explanation, action, severity, disclaimer
     */
    public AIInsight generateInsight(String clientName, String riskLevel,
                                     List<RiskBreachDetail> breaches,
                                     double portfolioValue, double dailyChange) {

        String explanation  = buildExplanation(clientName, riskLevel, breaches, dailyChange);
        String action       = buildSuggestedAction(riskLevel, breaches);
        String severity     = mapToSeverity(riskLevel);
        String disclaimer   = "DISCLAIMER: This analysis is AI-generated for informational purposes only. "
                            + "It does not constitute financial advice. Please consult a qualified financial advisor "
                            + "before making investment decisions.";

        return new AIInsight(explanation, action, severity, disclaimer);
    }

    /**
     * Simplified version returning just the explanation string.
     * Used by existing riskAnalysis response field.
     */
    public String generateRecommendation(String riskLevel) {
        return generateInsight("Portfolio", riskLevel, List.of(), 0, 0).getExplanation();
    }

    // ---------------------------------------------------------------
    // Private: Build contextual explanation
    // ---------------------------------------------------------------
    private String buildExplanation(String clientName, String riskLevel,
                                    List<RiskBreachDetail> breaches, double dailyChange) {

        if (breaches == null || breaches.isEmpty()) {
            return String.format(
                "Portfolio %s maintains healthy allocation balance. " +
                "All positions are within target thresholds with no risk breaches detected. " +
                "Current market exposure is within acceptable risk parameters.",
                clientName
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Portfolio %s has been flagged with %s risk. ",
            clientName, riskLevel));

        // Describe each breach
        boolean hasDrift        = false;
        boolean hasConcentration = false;
        boolean hasDailyDrop    = false;

        for (RiskBreachDetail breach : breaches) {
            switch (breach.getBreachType()) {
                case "ALLOCATION_DRIFT" -> {
                    if (!hasDrift) {
                        sb.append(String.format(
                            "%s is overweight in %s (actual: %.1f%%, target: %.1f%%), " +
                            "indicating portfolio drift from the model allocation. ",
                            clientName, breach.getAffectedSymbol(),
                            breach.getActualValue(), breach.getThresholdValue()
                        ));
                        hasDrift = true;
                    }
                }
                case "CONCENTRATION_RISK" -> {
                    if (!hasConcentration) {
                        sb.append(String.format(
                            "Single-stock concentration in %s has reached %.1f%%, " +
                            "exceeding the 20%% maximum threshold for diversified portfolios. ",
                            breach.getAffectedSymbol(), breach.getActualValue()
                        ));
                        hasConcentration = true;
                    }
                }
                case "DAILY_DROP" -> {
                    if (!hasDailyDrop) {
                        sb.append(String.format(
                            "The portfolio has declined %.1f%% from today's opening value, " +
                            "breaching the 3%% daily drawdown threshold. ",
                            Math.abs(dailyChange)
                        ));
                        hasDailyDrop = true;
                    }
                }
            }
        }

        return sb.toString().trim();
    }

    // ---------------------------------------------------------------
    // Private: Build suggested action
    // ---------------------------------------------------------------
    private String buildSuggestedAction(String riskLevel, List<RiskBreachDetail> breaches) {

        if (breaches == null || breaches.isEmpty()) {
            return "Continue current investment strategy. Schedule a periodic review in 30 days.";
        }

        StringBuilder actions = new StringBuilder();
        boolean hasDrift        = breaches.stream().anyMatch(b -> "ALLOCATION_DRIFT".equals(b.getBreachType()));
        boolean hasConcentration = breaches.stream().anyMatch(b -> "CONCENTRATION_RISK".equals(b.getBreachType()));
        boolean hasDailyDrop    = breaches.stream().anyMatch(b -> "DAILY_DROP".equals(b.getBreachType()));

        if (hasConcentration) {
            // Find the concentrated stock
            breaches.stream()
                .filter(b -> "CONCENTRATION_RISK".equals(b.getBreachType()))
                .findFirst()
                .ifPresent(b -> actions.append(String.format(
                    "Reduce %s position by selling approximately %.0f%% of current holding " +
                    "to bring concentration below 20%%. Redistribute proceeds across " +
                    "underweight sectors for better diversification. ",
                    b.getAffectedSymbol(),
                    b.getActualValue() - 20.0
                )));
        }

        if (hasDrift) {
            actions.append("Rebalance portfolio to restore target allocation weights. " +
                "Trim overweight positions and add to underweight ones in a tax-efficient manner. ");
        }

        if (hasDailyDrop) {
            actions.append("Review current market exposure. Consider adding defensive assets " +
                "(bonds, REITs, or cash equivalents) to cushion further downside. " +
                "Avoid panic-selling; evaluate fundamental changes before acting. ");
        }

        return actions.toString().trim();
    }

    private String mapToSeverity(String riskLevel) {
        return switch (riskLevel) {
            case "HIGH"   -> "CRITICAL";
            case "MEDIUM" -> "WARNING";
            default       -> "NORMAL";
        };
    }

    // ---------------------------------------------------------------
    // Bedrock prompt builder (ready for AWS upgrade)
    // ---------------------------------------------------------------

    /**
     * Builds the prompt that would be sent to Amazon Bedrock / Claude.
     * This method is ready — just needs to be wired to the API call.
     */
    public String buildBedrockPrompt(String clientName, String riskLevel,
                                     List<RiskBreachDetail> breaches,
                                     double portfolioValue, double dailyChange) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a portfolio risk analyst for a digital wealth management platform.\n\n");
        prompt.append("Analyze the following portfolio risk data and provide a structured response.\n\n");
        prompt.append(String.format("Client: %s\n", clientName));
        prompt.append(String.format("Risk Level: %s\n", riskLevel));
        prompt.append(String.format("Portfolio Value: $%.2f\n", portfolioValue));
        prompt.append(String.format("Daily Change: %.2f%%\n\n", dailyChange));
        prompt.append("Risk Breaches Detected:\n");

        if (breaches != null) {
            for (RiskBreachDetail breach : breaches) {
                prompt.append(String.format("- %s: %s\n", breach.getBreachType(), breach.getDescription()));
            }
        }

        prompt.append("\nProvide a JSON response with these exact fields:\n");
        prompt.append("{\n");
        prompt.append("  \"explanation\": \"Plain-language explanation of the risk situation\",\n");
        prompt.append("  \"suggestedAction\": \"Specific rebalancing recommendation\",\n");
        prompt.append("  \"severity\": \"CRITICAL|WARNING|NORMAL\",\n");
        prompt.append("  \"disclaimer\": \"Advisory disclaimer\"\n");
        prompt.append("}\n\n");
        prompt.append("IMPORTANT: Do not guarantee returns. Do not give specific price targets. ");
        prompt.append("Always include the advisory disclaimer.");

        return prompt.toString();
    }
}
