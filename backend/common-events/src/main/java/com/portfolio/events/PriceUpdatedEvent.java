package com.portfolio.events;

/**
 * PriceUpdatedEvent
 *
 * Published by: Market Data Service
 * Consumed by:  Risk Analysis Service
 *
 * AWS: Published to SNS topic "price-updated"
 *      Risk Service subscribes via SQS queue
 */
public class PriceUpdatedEvent {

    private String stockSymbol;
    private String stockName;
    private double currentPrice;
    private double previousPrice;
    private double changePercent;
    private double dailyChangePercent;
    private String timestamp;

    public PriceUpdatedEvent() {}

    public PriceUpdatedEvent(String stockSymbol, String stockName,
                              double currentPrice, double previousPrice,
                              double changePercent, double dailyChangePercent,
                              String timestamp) {
        this.stockSymbol = stockSymbol;
        this.stockName = stockName;
        this.currentPrice = currentPrice;
        this.previousPrice = previousPrice;
        this.changePercent = changePercent;
        this.dailyChangePercent = dailyChangePercent;
        this.timestamp = timestamp;
    }

    public String getStockSymbol() { return stockSymbol; }
    public String getStockName() { return stockName; }
    public double getCurrentPrice() { return currentPrice; }
    public double getPreviousPrice() { return previousPrice; }
    public double getChangePercent() { return changePercent; }
    public double getDailyChangePercent() { return dailyChangePercent; }
    public String getTimestamp() { return timestamp; }

    public void setStockSymbol(String s) { this.stockSymbol = s; }
    public void setStockName(String s) { this.stockName = s; }
    public void setCurrentPrice(double v) { this.currentPrice = v; }
    public void setPreviousPrice(double v) { this.previousPrice = v; }
    public void setChangePercent(double v) { this.changePercent = v; }
    public void setDailyChangePercent(double v) { this.dailyChangePercent = v; }
    public void setTimestamp(String s) { this.timestamp = s; }
}
