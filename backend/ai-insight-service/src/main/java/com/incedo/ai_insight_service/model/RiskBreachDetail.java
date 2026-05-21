package com.incedo.ai_insight_service.model;

/**
 * Mirror of the RiskBreachDetail from Risk Analysis Service.
 * Used to deserialize the RiskAlertEvent payload.
 */
public class RiskBreachDetail {
    private String breachType;
    private String affectedSymbol;
    private double actualValue;
    private double thresholdValue;
    private String description;

    public RiskBreachDetail() {}

    public String getBreachType() { return breachType; }
    public String getAffectedSymbol() { return affectedSymbol; }
    public double getActualValue() { return actualValue; }
    public double getThresholdValue() { return thresholdValue; }
    public String getDescription() { return description; }

    public void setBreachType(String breachType) { this.breachType = breachType; }
    public void setAffectedSymbol(String affectedSymbol) { this.affectedSymbol = affectedSymbol; }
    public void setActualValue(double actualValue) { this.actualValue = actualValue; }
    public void setThresholdValue(double thresholdValue) { this.thresholdValue = thresholdValue; }
    public void setDescription(String description) { this.description = description; }
}
