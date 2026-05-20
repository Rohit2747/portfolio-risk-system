package com.incedo.risk_analysis_service.model;

public class riskAnalysis {

    private String portfolioName;
    private double totalValue;
    private String riskLevel;
    private String suggestion;

    public riskAnalysis(String portfolioName,
                        double totalValue,
                        String riskLevel,
                        String suggestion) {

        this.portfolioName = portfolioName;
        this.totalValue = totalValue;
        this.riskLevel = riskLevel;
        this.suggestion = suggestion;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getSuggestion() {
        return suggestion;
    }
}