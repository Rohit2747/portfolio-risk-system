package com.incedo.portfolioservice.model;

public class portfolio {

    private int clientId;
    private String clientName;
    private double portfolioValue;
    private String riskLevel;

    public portfolio(int clientId, String clientName, double portfolioValue, String riskLevel) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.portfolioValue = portfolioValue;
        this.riskLevel = riskLevel;
    }

    public int getClientId() {
        return clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public double getPortfolioValue() {
        return portfolioValue;
    }

    public String getRiskLevel() {
        return riskLevel;
    }
}