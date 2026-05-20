package com.portfolio.events;
public class MarketDataUpdatedEvent {

    private String stockName;
    private double latestPrice;
    private double percentageChange;
    private String timestamp;

    public MarketDataUpdatedEvent() {
    }

    public MarketDataUpdatedEvent(
            String stockName,
            double latestPrice,
            double percentageChange,
            String timestamp) {

        this.stockName = stockName;
        this.latestPrice = latestPrice;
        this.percentageChange = percentageChange;
        this.timestamp = timestamp;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public double getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(double latestPrice) {
        this.latestPrice = latestPrice;
    }

    public double getPercentageChange() {
        return percentageChange;
    }

    public void setPercentageChange(double percentageChange) {
        this.percentageChange = percentageChange;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}