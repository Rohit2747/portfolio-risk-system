package com.incedo.market_data_service.model;

public class marketData {

    private String stockName;
    private double price;
    private double change;

    public marketData(String stockName, double price, double change) {
        this.stockName = stockName;
        this.price = price;
        this.change = change;
    }

    public String getStockName() {
        return stockName;
    }

    public double getPrice() {
        return price;
    }

    public double getChange() {
        return change;
        
    }
}