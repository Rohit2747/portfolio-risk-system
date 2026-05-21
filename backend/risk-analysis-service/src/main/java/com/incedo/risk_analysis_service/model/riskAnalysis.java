package com.incedo.risk_analysis_service.model;

import java.util.List;

/**
 * RiskAnalysis is the full risk evaluation result for one client portfolio.
 *
 * This is returned by GET /risk-analysis and displayed on the dashboard.
 * It will also be sent to the AI Insight Service for explanation generation.
 */
public class riskAnalysis {

    private int clientId;
    private String clientName;
    private double totalPortfolioValue;       // computed: Σ(quantity × currentPrice)
    private double openingPortfolioValue;     // computed: Σ(quantity × openingPrice)
    private double dailyChangePercent;        // (total - opening) / opening * 100
    private String riskLevel;                 // LOW | MEDIUM | HIGH
    private boolean hasBreaches;              // true if any threshold was breached
    private List<RiskBreachDetail> breaches;  // list of all breaches detected
    private String aiInsight;                 // explanation from AI Insight Service
    private String suggestedAction;           // rebalancing recommendation
    private String alertTimestamp;            // when this analysis was generated

    public riskAnalysis() {}

    public riskAnalysis(int clientId, String clientName,
                        double totalPortfolioValue, double openingPortfolioValue,
                        double dailyChangePercent, String riskLevel,
                        boolean hasBreaches, List<RiskBreachDetail> breaches,
                        String aiInsight, String suggestedAction,
                        String alertTimestamp) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.totalPortfolioValue = totalPortfolioValue;
        this.openingPortfolioValue = openingPortfolioValue;
        this.dailyChangePercent = dailyChangePercent;
        this.riskLevel = riskLevel;
        this.hasBreaches = hasBreaches;
        this.breaches = breaches;
        this.aiInsight = aiInsight;
        this.suggestedAction = suggestedAction;
        this.alertTimestamp = alertTimestamp;
    }

    // Getters
    public int getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public double getTotalPortfolioValue() { return totalPortfolioValue; }
    public double getOpeningPortfolioValue() { return openingPortfolioValue; }
    public double getDailyChangePercent() { return dailyChangePercent; }
    public String getRiskLevel() { return riskLevel; }
    public boolean isHasBreaches() { return hasBreaches; }
    public List<RiskBreachDetail> getBreaches() { return breaches; }
    public String getAiInsight() { return aiInsight; }
    public String getSuggestedAction() { return suggestedAction; }
    public String getAlertTimestamp() { return alertTimestamp; }

    // Setters
    public void setClientId(int clientId) { this.clientId = clientId; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setTotalPortfolioValue(double v) { this.totalPortfolioValue = v; }
    public void setOpeningPortfolioValue(double v) { this.openingPortfolioValue = v; }
    public void setDailyChangePercent(double v) { this.dailyChangePercent = v; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setHasBreaches(boolean hasBreaches) { this.hasBreaches = hasBreaches; }
    public void setBreaches(List<RiskBreachDetail> breaches) { this.breaches = breaches; }
    public void setAiInsight(String aiInsight) { this.aiInsight = aiInsight; }
    public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
    public void setAlertTimestamp(String alertTimestamp) { this.alertTimestamp = alertTimestamp; }

    // Convenience alias used by frontend
    public double getPortfolioValue() { return totalPortfolioValue; }
    public String getPortfolioName() { return clientName; }
    public String getSuggestion() { return suggestedAction; }
}
