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
 *
 * Endpoints:
 *   GET /market-data          → all 20 equities (full marketData objects)
 *   GET /market-data/prices   → symbol → currentPrice map (used by Risk Service)
 *   GET /market-data/{symbol} → single stock details (used by Risk Service for openingPrice)
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
    public List<marketData> getMarketData() {
        return priceSimulatorService.getAllPrices();
    }

    /**
     * Returns a map of symbol → currentPrice for all equities.
     * Used by Risk Analysis Service for fast bulk portfolio valuation.
     *
     * Example response:
     * { "AAPL": 189.50, "MSFT": 418.30, "NVDA": 890.20, ... }
     */
    @GetMapping("/market-data/prices")
    public Map<String, Double> getPriceMap() {
        return priceSimulatorService.getPriceMap();
    }

    /**
     * Returns full market data for a single stock symbol.
     * Used by Risk Analysis Service to get openingPrice for daily change calculation.
     *
     * Example: GET /market-data/AAPL
     * Returns: { "stockSymbol": "AAPL", "currentPrice": 189.50, "openingPrice": 187.50, ... }
     */
    @GetMapping("/market-data/{symbol}")
    public marketData getMarketDataBySymbol(@PathVariable String symbol) {
        marketData data = priceSimulatorService.getCurrentPrice(symbol);
        if (data == null) {
            throw new RuntimeException("Stock symbol not found: " + symbol);
        }
        return data;
    }
}
