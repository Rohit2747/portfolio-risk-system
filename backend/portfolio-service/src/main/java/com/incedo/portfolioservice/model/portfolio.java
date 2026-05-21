package com.incedo.portfolioservice.model;

import java.util.List;

/**
 * Represents a client's investment portfolio.
 * Contains the client's identity, their stock holdings,
 * and the computed portfolio value (set by Risk Service).
 */
public class portfolio {

    private int clientId;
    private String clientName;
    private double portfolioValue;        // computed dynamically from holdings × prices
    private String riskLevel;             // LOW / MEDIUM / HIGH (set by risk service)
    private List<Holding> holdings;       // actual stock positions

    public portfolio() {}

    public portfolio(int clientId, String clientName, double portfolioValue,
                     String riskLevel, List<Holding> holdings) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.portfolioValue = portfolioValue;
        this.riskLevel = riskLevel;
        this.holdings = holdings;
    }

    // Getters
    public int getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public double getPortfolioValue() { return portfolioValue; }
    public String getRiskLevel() { return riskLevel; }
    public List<Holding> getHoldings() { return holdings; }

    // Setters
    public void setClientId(int clientId) { this.clientId = clientId; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setPortfolioValue(double portfolioValue) { this.portfolioValue = portfolioValue; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setHoldings(List<Holding> holdings) { this.holdings = holdings; }
}
