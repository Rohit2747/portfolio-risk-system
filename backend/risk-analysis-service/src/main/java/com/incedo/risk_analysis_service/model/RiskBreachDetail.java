package com.incedo.risk_analysis_service.model;

/**
 * RiskBreachDetail describes a single risk threshold breach.
 *
 * There are 3 possible breach types (as defined in the problem statement):
 *   1. ALLOCATION_DRIFT     - A stock's actual % deviates from target % by > 5%
 *   2. CONCENTRATION_RISK   - A single stock's value exceeds 20% of portfolio value
 *   3. DAILY_DROP           - Portfolio lost > 3% from its opening value today
 */
public class RiskBreachDetail {

    private String breachType;       // ALLOCATION_DRIFT | CONCENTRATION_RISK | DAILY_DROP
    private String affectedSymbol;   // which stock triggered the breach (null for DAILY_DROP)
    private double actualValue;      // the computed % value
    private double thresholdValue;   // the limit that was exceeded
    private String description;      // human-readable description

    public RiskBreachDetail() {}

    public RiskBreachDetail(String breachType, String affectedSymbol,
                             double actualValue, double thresholdValue,
                             String description) {
        this.breachType = breachType;
        this.affectedSymbol = affectedSymbol;
        this.actualValue = actualValue;
        this.thresholdValue = thresholdValue;
        this.description = description;
    }

    // Getters & Setters
    public String getBreachType() { return breachType; }
    public void setBreachType(String breachType) { this.breachType = breachType; }

    public String getAffectedSymbol() { return affectedSymbol; }
    public void setAffectedSymbol(String affectedSymbol) { this.affectedSymbol = affectedSymbol; }

    public double getActualValue() { return actualValue; }
    public void setActualValue(double actualValue) { this.actualValue = actualValue; }

    public double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(double thresholdValue) { this.thresholdValue = thresholdValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
