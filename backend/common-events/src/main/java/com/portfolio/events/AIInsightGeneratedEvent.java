package com.portfolio.events;

/**
 * AIInsightGeneratedEvent
 *
 * Published by: AI Insight Service (after generating insight)
 * Consumed by:  Dashboard (via polling or WebSocket)
 *               Notification Service (optional: sends to client)
 *
 * AWS: Published to SNS topic "ai-insight-generated"
 */
public class AIInsightGeneratedEvent {

    private int clientId;
    private String clientName;
    private String riskLevel;
    private String explanation;
    private String suggestedAction;
    private String severity;
    private String aiProvider;
    private String timestamp;

    public AIInsightGeneratedEvent() {}

    public AIInsightGeneratedEvent(int clientId, String clientName, String riskLevel,
                                    String explanation, String suggestedAction,
                                    String severity, String aiProvider, String timestamp) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.riskLevel = riskLevel;
        this.explanation = explanation;
        this.suggestedAction = suggestedAction;
        this.severity = severity;
        this.aiProvider = aiProvider;
        this.timestamp = timestamp;
    }

    public int getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getRiskLevel() { return riskLevel; }
    public String getExplanation() { return explanation; }
    public String getSuggestedAction() { return suggestedAction; }
    public String getSeverity() { return severity; }
    public String getAiProvider() { return aiProvider; }
    public String getTimestamp() { return timestamp; }

    public void setClientId(int v) { this.clientId = v; }
    public void setClientName(String v) { this.clientName = v; }
    public void setRiskLevel(String v) { this.riskLevel = v; }
    public void setExplanation(String v) { this.explanation = v; }
    public void setSuggestedAction(String v) { this.suggestedAction = v; }
    public void setSeverity(String v) { this.severity = v; }
    public void setAiProvider(String v) { this.aiProvider = v; }
    public void setTimestamp(String v) { this.timestamp = v; }
}
