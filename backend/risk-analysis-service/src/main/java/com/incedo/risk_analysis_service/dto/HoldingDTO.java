package com.incedo.risk_analysis_service.dto;

/**
 * DTO matching the Holding JSON structure from Portfolio Service.
 */
public class HoldingDTO {
    private String stockSymbol;
    private String stockName;
    private int quantity;
    private double targetAllocationPercent;

    public HoldingDTO() {}

    public String getStockSymbol() { return stockSymbol; }
    public String getStockName() { return stockName; }
    public int getQuantity() { return quantity; }
    public double getTargetAllocationPercent() { return targetAllocationPercent; }

    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setTargetAllocationPercent(double t) { this.targetAllocationPercent = t; }
}
