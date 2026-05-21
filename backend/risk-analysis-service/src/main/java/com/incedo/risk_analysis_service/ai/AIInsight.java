package com.incedo.risk_analysis_service.ai;

/**
 * AIInsight — structured output from the AI Recommendation Service.
 *
 * This is the JSON schema for AI-generated portfolio commentary.
 *
 * Matches the problem statement requirement:
 *   - Risk explanation ✓
 *   - Suggested corrective action ✓
 *   - Risk severity classification ✓
 *   - Advisory disclaimer ✓
 *
 * AWS: When using Amazon Bedrock, the LLM response is parsed into this structure.
 */
public class AIInsight {

    private String explanation;      // plain-language portfolio risk explanation
    private String suggestedAction;  // specific rebalancing recommendation
    private String severity;         // CRITICAL | WARNING | NORMAL
    private String disclaimer;       // regulatory advisory disclaimer

    public AIInsight() {}

    public AIInsight(String explanation, String suggestedAction,
                     String severity, String disclaimer) {
        this.explanation = explanation;
        this.suggestedAction = suggestedAction;
        this.severity = severity;
        this.disclaimer = disclaimer;
    }

    // Getters
    public String getExplanation() { return explanation; }
    public String getSuggestedAction() { return suggestedAction; }
    public String getSeverity() { return severity; }
    public String getDisclaimer() { return disclaimer; }

    // Setters
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
}
