package com.incedo.ai_insight_service.model;

/**
 * AIInsightResponse — structured JSON output from AI Insight Service.
 *
 * This is the required output format from the problem statement:
 *   - Risk explanation ✓
 *   - Suggested corrective action ✓
 *   - Risk severity classification ✓
 *   - Advisory disclaimer ✓
 *
 * Example output (matches problem statement example):
 * {
 *   "explanation": "The portfolio is overweight in NVDA by 8% due to recent price appreciation.",
 *   "suggestedAction": "Consider trimming NVDA exposure by ~5% to reduce concentration risk.",
 *   "severity": "WARNING",
 *   "disclaimer": "This analysis is AI-generated for informational purposes only...",
 *   "clientId": 78,
 *   "clientName": "Client-078",
 *   "generatedAt": "2026-05-21 14:30:00",
 *   "aiProvider": "MOCK"
 * }
 */
public class AIInsightResponse {

    private int clientId;
    private String clientName;
    private String riskLevel;
    private String explanation;       // plain-language risk explanation
    private String suggestedAction;   // specific rebalancing recommendation
    private String severity;          // CRITICAL | WARNING | NORMAL
    private String disclaimer;        // regulatory advisory disclaimer
    private String generatedAt;       // timestamp
    private String aiProvider;        // MOCK | OPENAI | BEDROCK

    public AIInsightResponse() {}

    public AIInsightResponse(int clientId, String clientName, String riskLevel,
                              String explanation, String suggestedAction,
                              String severity, String disclaimer,
                              String generatedAt, String aiProvider) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.riskLevel = riskLevel;
        this.explanation = explanation;
        this.suggestedAction = suggestedAction;
        this.severity = severity;
        this.disclaimer = disclaimer;
        this.generatedAt = generatedAt;
        this.aiProvider = aiProvider;
    }

    // Getters
    public int getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getRiskLevel() { return riskLevel; }
    public String getExplanation() { return explanation; }
    public String getSuggestedAction() { return suggestedAction; }
    public String getSeverity() { return severity; }
    public String getDisclaimer() { return disclaimer; }
    public String getGeneratedAt() { return generatedAt; }
    public String getAiProvider() { return aiProvider; }

    // Setters
    public void setClientId(int clientId) { this.clientId = clientId; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }
}
