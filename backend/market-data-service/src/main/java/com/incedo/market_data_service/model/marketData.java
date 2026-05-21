package com.incedo.market_data_service.model;

/**
 * MarketData represents the current market snapshot for one equity.
 * Price updates are simulated by the PriceSimulatorService.
 */
public class marketData {

    private String stockSymbol;         // e.g., "AAPL"
    private String stockName;           // e.g., "Apple Inc."
    private double currentPrice;        // current simulated price
    private double openingPrice;        // price at start of day (fixed at service startup)
    private double previousPrice;       // price just before the latest update
    private double changePercent;       // (currentPrice - previousPrice) / previousPrice * 100
    private double dailyChangePercent;  // (currentPrice - openingPrice) / openingPrice * 100
    private String lastUpdated;         // ISO timestamp

    public marketData() {}

    public marketData(String stockSymbol, String stockName, double currentPrice,
                      double openingPrice, double previousPrice,
                      double changePercent, double dailyChangePercent,
                      String lastUpdated) {
        this.stockSymbol = stockSymbol;
        this.stockName = stockName;
        this.currentPrice = currentPrice;
        this.openingPrice = openingPrice;
        this.previousPrice = previousPrice;
        this.changePercent = changePercent;
        this.dailyChangePercent = dailyChangePercent;
        this.lastUpdated = lastUpdated;
    }

    // Getters
    public String getStockSymbol() { return stockSymbol; }
    public String getStockName() { return stockName; }
    public double getCurrentPrice() { return currentPrice; }
    public double getOpeningPrice() { return openingPrice; }
    public double getPreviousPrice() { return previousPrice; }
    public double getChangePercent() { return changePercent; }
    public double getDailyChangePercent() { return dailyChangePercent; }
    public String getLastUpdated() { return lastUpdated; }

    // Setters
    public void setStockSymbol(String stockSymbol) { this.stockSymbol = stockSymbol; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setOpeningPrice(double openingPrice) { this.openingPrice = openingPrice; }
    public void setPreviousPrice(double previousPrice) { this.previousPrice = previousPrice; }
    public void setChangePercent(double changePercent) { this.changePercent = changePercent; }
    public void setDailyChangePercent(double dailyChangePercent) { this.dailyChangePercent = dailyChangePercent; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    // convenience alias used by frontend (change field)
    public double getChange() { return changePercent; }
    // convenience alias used by frontend (price field)
    public double getPrice() { return currentPrice; }
    // convenience alias used by frontend (stockName)
    public String getSymbol() { return stockSymbol; }
}
