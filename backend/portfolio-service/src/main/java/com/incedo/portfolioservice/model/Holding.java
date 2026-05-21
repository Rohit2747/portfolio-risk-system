package com.incedo.portfolioservice.model;

/**
 * Represents a single stock holding within a portfolio.
 * Each holding tracks:
 *  - which stock is held (symbol)
 *  - how many shares are held (quantity)
 *  - what % this stock SHOULD be in the portfolio (targetAllocationPercent)
 */
public class Holding {

    private String stockSymbol;   // e.g., "AAPL"
    private String stockName;     // e.g., "Apple Inc."
    private int quantity;         // number of shares held
    private double targetAllocationPercent;  // model/target weight, e.g., 15.0 means 15%

    public Holding() {}

    public Holding(String stockSymbol, String stockName, int quantity, double targetAllocationPercent) {
        this.stockSymbol = stockSymbol;
        this.stockName = stockName;
        this.quantity = quantity;
        this.targetAllocationPercent = targetAllocationPercent;
    }

    // Getters
    public String getStockSymbol() { return stockSymbol; }
    public String getStockName() { return stockName; }
    public int getQuantity() { return quantity; }
    public double getTargetAllocationPercent() { return targetAllocationPercent; }

    // Setters
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setTargetAllocationPercent(double targetAllocationPercent) {
        this.targetAllocationPercent = targetAllocationPercent;
    }
}
