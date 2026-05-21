package com.incedo.ai_insight_service.model;

import java.util.List;

/**
 * AIInsightRequest — payload sent to AI Insight Service.
 *
 * In local mode: sent via REST POST from Risk Service or frontend.
 * In AWS mode:   received via SQS message from Risk Service SNS topic.
 */
public class AIInsightRequest {

    private int clientId;
    private String clientName;
    private String riskLevel;
    private List<RiskBreachDetail> breaches;
    private double portfolioValue;
    private double dailyChangePercent;
    private String timestamp;

    public AIInsightRequest() {}

    // Getters
    public int getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public String getRiskLevel() { return riskLevel; }
    public List<RiskBreachDetail> getBreaches() { return breaches; }
    public double getPortfolioValue() { return portfolioValue; }
    public double getDailyChangePercent() { return dailyChangePercent; }
    public String getTimestamp() { return timestamp; }

    // Setters
    public void setClientId(int clientId) { this.clientId = clientId; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setBreaches(List<RiskBreachDetail> breaches) { this.breaches = breaches; }
    public void setPortfolioValue(double portfolioValue) { this.portfolioValue = portfolioValue; }
    public void setDailyChangePercent(double dailyChangePercent) { this.dailyChangePercent = dailyChangePercent; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
