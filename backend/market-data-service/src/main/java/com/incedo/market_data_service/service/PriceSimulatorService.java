package com.incedo.market_data_service.service;

import com.incedo.market_data_service.model.marketData;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PriceSimulatorService
 *
 * Maintains stateful, realistic price simulation for 20 equities.
 *
 * Key behaviors:
 *   - Prices start at realistic base values and drift realistically
 *   - Prices update every 5 seconds (configurable)
 *   - Opening price is fixed at service startup (simulates day open)
 *   - Tracks both tick-change% and daily-change%
 *   - Some volatile stocks (NVDA, TSLA, META) have higher variance
 *
 * This simulates the "Market Data Service publishes PriceUpdated events" requirement.
 * The @Scheduled method acts as the event trigger.
 */
@Service
public class PriceSimulatorService {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---------------------------------------------------------------
    // 20 Equities: { symbol, name, basePrice, volatility }
    // volatility: max % swing per tick (0.02 = 2%)
    // ---------------------------------------------------------------
    private static final Object[][] EQUITY_CONFIG = {
        { "AAPL",       "Apple Inc.",            187.50,  0.015 },
        { "MSFT",       "Microsoft Corp.",       415.20,  0.012 },
        { "NVDA",       "NVIDIA Corp.",          875.40,  0.035 },  // high vol
        { "AMZN",       "Amazon.com Inc.",       182.30,  0.018 },
        { "GOOGL",      "Alphabet Inc.",         175.60,  0.015 },
        { "META",       "Meta Platforms",        505.80,  0.025 },  // high vol
        { "TSLA",       "Tesla Inc.",            172.40,  0.040 },  // highest vol
        { "RELIANCE",   "Reliance Industries",  2985.50,  0.012 },
        { "HDFCBANK",   "HDFC Bank",            1678.90,  0.010 },
        { "INFY",       "Infosys Ltd.",           183.25,  0.015 },
        { "TCS",        "TCS Ltd.",             3912.00,  0.010 },
        { "WIPRO",      "Wipro Ltd.",             538.60,  0.015 },
        { "ICICIBANK",  "ICICI Bank",            1087.30,  0.012 },
        { "SBIN",       "State Bank of India",    815.70,  0.018 },
        { "BAJFINANCE", "Bajaj Finance",         7285.40,  0.022 },
        { "ASIANPAINT", "Asian Paints",          2895.60,  0.012 },
        { "HINDUNILVR", "Hindustan Unilever",    2534.80,  0.008 },
        { "KOTAKBANK",  "Kotak Mahindra Bank",   1834.20,  0.012 },
        { "LT",         "Larsen & Toubro",       3478.90,  0.015 },
        { "SUNPHARMA",  "Sun Pharmaceutical",    1567.30,  0.018 }
    };

    // In-memory price store: symbol -> current marketData
    private final Map<String, marketData> priceMap = new ConcurrentHashMap<>();

    private final Random random = new Random();

    /**
     * Initializes all 20 stocks with their base prices at service startup.
     * Opening prices are recorded here for daily-change calculation.
     */
    public PriceSimulatorService() {
        String startTime = LocalDateTime.now().format(FORMATTER);

        for (Object[] config : EQUITY_CONFIG) {
            String symbol     = (String) config[0];
            String name       = (String) config[1];
            double basePrice  = (Double) config[2];

            priceMap.put(symbol, new marketData(
                symbol,
                name,
                basePrice,      // currentPrice
                basePrice,      // openingPrice (fixed)
                basePrice,      // previousPrice
                0.0,            // changePercent (tick)
                0.0,            // dailyChangePercent
                startTime
            ));
        }
    }

    /**
     * Simulates price tick every 5 seconds.
     * Updates currentPrice with realistic random walk.
     * Updates change% and dailyChange%.
     *
     * This is the event source — equivalent to publishing a PriceUpdatedEvent.
     * In the AWS version, this method will publish to SNS.
     */
    @Scheduled(fixedDelay = 5000)
    public void simulatePriceTick() {
        String now = LocalDateTime.now().format(FORMATTER);

        for (Object[] config : EQUITY_CONFIG) {
            String symbol    = (String) config[0];
            double vol       = (Double) config[3];

            marketData current = priceMap.get(symbol);
            if (current == null) continue;

            double prev     = current.getCurrentPrice();
            double opening  = current.getOpeningPrice();

            // Random walk: +/- vol% swing
            double change   = (random.nextDouble() * 2 - 1) * vol;
            double newPrice = Math.round(prev * (1 + change) * 100.0) / 100.0;

            // Prevent price going below 10% of base price (floor)
            if (newPrice < opening * 0.10) newPrice = opening * 0.10;

            double tickChange  = Math.round(((newPrice - prev) / prev * 100) * 100.0) / 100.0;
            double dailyChange = Math.round(((newPrice - opening) / opening * 100) * 100.0) / 100.0;

            priceMap.put(symbol, new marketData(
                symbol,
                current.getStockName(),
                newPrice,
                opening,        // opening price stays fixed all day
                prev,           // previous price = what it was before this tick
                tickChange,
                dailyChange,
                now
            ));
        }
    }

    /**
     * Returns current price for a specific stock symbol.
     * Called by Risk Service for portfolio valuation.
     */
    public marketData getCurrentPrice(String symbol) {
        return priceMap.get(symbol);
    }

    /**
     * Returns all current prices (used by /market-data endpoint).
     */
    public List<marketData> getAllPrices() {
        return new ArrayList<>(priceMap.values());
    }

    /**
     * Returns a price map for fast bulk lookup.
     * Key = symbol, Value = current price double.
     * Used by Risk Service.
     */
    public Map<String, Double> getPriceMap() {
        Map<String, Double> result = new HashMap<>();
        priceMap.forEach((symbol, data) -> result.put(symbol, data.getCurrentPrice()));
        return result;
    }
}
