package com.incedo.risk_analysis_service.dto;

import java.util.List;

/**
 * DTO matching the Portfolio JSON structure from Portfolio Service.
 */
public class PortfolioDTO {
    private int clientId;
    private String clientName;
    private double portfolioValue;
    private String riskLevel;
    private List<HoldingDTO> holdings;

    public PortfolioDTO() {}

    public int getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public double getPortfolioValue() { return portfolioValue; }
    public String getRiskLevel() { return riskLevel; }
    public List<HoldingDTO> getHoldings() { return holdings; }

    public void setClientId(int clientId) { this.clientId = clientId; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setPortfolioValue(double portfolioValue) { this.portfolioValue = portfolioValue; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setHoldings(List<HoldingDTO> holdings) { this.holdings = holdings; }
}
