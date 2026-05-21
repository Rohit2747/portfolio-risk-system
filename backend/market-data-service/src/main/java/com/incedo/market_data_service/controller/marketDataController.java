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
     * Returns current market data for all 20 equities.
     * Frontend polls this every 5 seconds for live updates.
     * Data comes from PriceSimulatorService which updates prices every 5s.
     */
    @GetMapping("/market-data")
    public List<marketData> getMarketData() {
        return priceSimulatorService.getAllPrices();
    }

    /**
     * Returns a simple price map: symbol -> currentPrice.
     * Used by Risk Analysis Service for portfolio valuation.
     */
    @GetMapping("/market-data/prices")
    public Map<String, Double> getPriceMap() {
        return priceSimulatorService.getPriceMap();
    }

    /**
     * Returns full market data for a single stock symbol.
     * Used by Risk Analysis Service to get opening prices.
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
