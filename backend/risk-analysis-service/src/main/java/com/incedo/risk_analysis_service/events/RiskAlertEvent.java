package com.incedo.risk_analysis_service.events;
public class RiskAlertEvent {

    private String portfolioName;
    private String riskLevel;
    private String alertMessage;
    private String timestamp;

    public RiskAlertEvent() {
    }

    public RiskAlertEvent(
            String portfolioName,
            String riskLevel,
            String alertMessage,
            String timestamp) {

        this.portfolioName = portfolioName;
        this.riskLevel = riskLevel;
        this.alertMessage = alertMessage;
        this.timestamp = timestamp;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public void setPortfolioName(String portfolioName) {
        this.portfolioName = portfolioName;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
