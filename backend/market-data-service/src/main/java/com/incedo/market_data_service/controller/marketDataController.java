package com.incedo.market_data_service.controller;

import com.incedo.market_data_service.model.marketData;
import com.incedo.market_data_service.service.PriceSimulatorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MarketDataController
 *
 * Exposes REST APIs for real-time equity prices.
 * Prices are updated every 5 seconds by PriceSimulatorService.
 */
@RestController
@CrossOrigin(origins = "*")
public class marketDataController {

    private final PriceSimulatorService priceSimulatorService;

    public marketDataController(PriceSimulatorService priceSimulatorService) {
        this.priceSimulatorService = priceSimulatorService;
    }

    /**
     * Health check
     */
    @GetMapping("/hello")
    public String hello() {
        return "Market Data Service is running on port 8081";
    }

    /**
     * Returns current prices for all 20 equities.
     * Frontend polls this every 5 seconds for live updates.
     */
    @GetMapping("/market-data")
    public List<marketData> getAllMarketData() {
        return priceSimulatorService.getAllPrices();
    }

    /**
     * Returns current price for a specific stock symbol.
     * Called by Risk Service for portfolio valuation.
     */
    @GetMapping("/market-data/{symbol}")
    public marketData getBySymbol(@PathVariable String symbol) {
        marketData data = priceSimulatorService.getCurrentPrice(symbol.toUpperCase());
        if (data == null) {
            throw new RuntimeException("Symbol not found: " + symbol);
        }
        return data;
    }

    /**
     * Returns a flat symbol->price map for bulk lookups.
     * Used by Risk Service when computing portfolio values for all 100 clients.
     */
    @GetMapping("/market-data/prices")
    public Map<String, Double> getPriceMap() {
        return priceSimulatorService.getPriceMap();
    }
}