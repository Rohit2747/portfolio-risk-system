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
 *   GET /hello              → Health check
 *   GET /market-data        → All 20 equities with full details (used by Frontend)
 *   GET /market-data/prices → Symbol-to-price map (used by Risk Analysis Service)
 *   GET /market-data/{sym}  → Single stock details (used by Risk Service for opening price)
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
     * Returns current prices for all 20 equities with full details.
     * Frontend polls this every 5 seconds for live updates.
     *
     * Response includes: stockSymbol, stockName, currentPrice,
     * openingPrice, previousPrice, changePercent, dailyChangePercent, lastUpdated
     */
    @GetMapping("/market-data")
    public List<marketData> getMarketData() {
        return priceSimulatorService.getAllPrices();
    }

    /**
     * Returns a simple symbol → currentPrice map.
     * Used by Risk Analysis Service for fast portfolio valuation.
     *
     * Example response:
     * { "AAPL": 187.50, "MSFT": 415.20, "NVDA": 875.40, ... }
     */
    @GetMapping("/market-data/prices")
    public Map<String, Double> getPriceMap() {
        return priceSimulatorService.getPriceMap();
    }

    /**
     * Returns full market data for a single stock symbol.
     * Used by Risk Analysis Service to get opening prices for daily change calculation.
     *
     * Example: GET /market-data/AAPL
     * Response: { "stockSymbol": "AAPL", "currentPrice": 189.50, "openingPrice": 187.50, ... }
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
