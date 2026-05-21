package com.incedo.risk_analysis_service.events;

import com.incedo.risk_analysis_service.model.RiskBreachDetail;

import java.util.List;

/**
 * RiskAlertEvent
 *
 * Published when a portfolio breaches one or more risk thresholds.
 *
 * Event flow:
 *   RiskCalculationEngine detects breach
 *       → RiskEventPublisher.publishRiskAlert(this)
 *           → LOCAL: logs to console (current)
 *           → AWS:   publishes to SNS topic "risk-threshold-breached" (next step)
 *
 * This event is consumed by:
 *   - AI Insight Service (generates explanation)
 *   - Notification Service (optional: sends email/SMS alert)
 */
public class RiskAlertEvent {

    private int clientId;
    private String clientName;
    private String riskLevel;              // HIGH | MEDIUM
    private List<RiskBreachDetail> breaches;
    private double portfolioValue;
    private double dailyChangePercent;
    private String timestamp;

    public RiskAlertEvent() {}

    public RiskAlertEvent(int clientId, String clientName, String riskLevel,
                          List<RiskBreachDetail> breaches, double portfolioValue,
                          double dailyChangePercent, String timestamp) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.riskLevel = riskLevel;
        this.breaches = breaches;
        this.portfolioValue = portfolioValue;
        this.dailyChangePercent = dailyChangePercent;
        this.timestamp = timestamp;
    }

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

    @Override
    public String toString() {
        return String.format(
            "RiskAlertEvent{clientId=%d, client='%s', level=%s, breaches=%d, value=%.2f, dailyChange=%.2f%%}",
            clientId, clientName, riskLevel,
            breaches != null ? breaches.size() : 0,
            portfolioValue, dailyChangePercent
        );
    }
}
