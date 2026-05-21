package com.portfolio.events;

/**
 * PortfolioRevaluedEvent
 *
 * Published by: Risk Analysis Service (after computing portfolio value)
 * Consumed by:  Risk Analysis Service itself (triggers breach detection)
 *               AI Insight Service (for context)
 *
 * AWS: Published to SNS topic "portfolio-revalued"
 */
public class PortfolioRevaluedEvent {

    private int clientId;
    private String clientName;
    private double previousValue;
    private double currentValue;
    private double changePercent;
    private String timestamp;

    public PortfolioRevaluedEvent() {}

    public PortfolioRevaluedEvent(int clientId, String clientName,
                                   double previousValue, double currentValue,
                                   double changePercent, String timestamp) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.previousValue = previousValue;
        this.currentValue = currentValue;
        this.changePercent = changePercent;
        this.timestamp = timestamp;
    }

    public int getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public double getPreviousValue() { return previousValue; }
    public double getCurrentValue() { return currentValue; }
    public double getChangePercent() { return changePercent; }
    public String getTimestamp() { return timestamp; }

    public void setClientId(int v) { this.clientId = v; }
    public void setClientName(String v) { this.clientName = v; }
    public void setPreviousValue(double v) { this.previousValue = v; }
    public void setCurrentValue(double v) { this.currentValue = v; }
    public void setChangePercent(double v) { this.changePercent = v; }
    public void setTimestamp(String v) { this.timestamp = v; }
}
