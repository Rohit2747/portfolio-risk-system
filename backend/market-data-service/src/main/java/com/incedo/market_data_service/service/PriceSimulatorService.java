package com.incedo.market_data_service.service;

import com.incedo.market_data_service.events.PriceEventPublisher;
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
 *   - After each tick, publishes PriceUpdatedEvent to SNS (AWS mode)
 *     or logs locally (local mode)
 */
@Service
public class PriceSimulatorService {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ---------------------------------------------------------------
    // 20 Equities: { symbol, name, basePrice, volatility }
    // volatility: max % swing per tick (0.02 = 2%)
    // ---------------------------------------------------------------
    // Volatility = max % move per 5-second tick
    // Reduced to realistic levels so daily drift stays within -3% to +3% for most stocks.
    // Only TSLA/NVDA have enough volatility to occasionally trigger DAILY_DROP (>3%) over time.
    // Conservative stocks (HDFCBANK, TCS, HINDUNILVR) barely move — keeps LOW risk clients stable.
    private static final Object[][] EQUITY_CONFIG = {
        { "AAPL",       "Apple Inc.",            187.50,  0.003 },   // stable
        { "MSFT",       "Microsoft Corp.",       415.20,  0.003 },   // stable
        { "NVDA",       "NVIDIA Corp.",          875.40,  0.008 },   // volatile — can trigger drift
        { "AMZN",       "Amazon.com Inc.",       182.30,  0.004 },   // moderate
        { "GOOGL",      "Alphabet Inc.",         175.60,  0.003 },   // stable
        { "META",       "Meta Platforms",        505.80,  0.006 },   // moderate-high
        { "TSLA",       "Tesla Inc.",            172.40,  0.010 },   // most volatile — triggers breaches
        { "RELIANCE",   "Reliance Industries",  2985.50,  0.002 },   // very stable
        { "HDFCBANK",   "HDFC Bank",            1678.90,  0.002 },   // very stable
        { "INFY",       "Infosys Ltd.",           183.25,  0.003 },   // stable
        { "TCS",        "TCS Ltd.",             3912.00,  0.002 },   // very stable
        { "WIPRO",      "Wipro Ltd.",             538.60,  0.003 },   // stable
        { "ICICIBANK",  "ICICI Bank",            1087.30,  0.002 },   // very stable
        { "SBIN",       "State Bank of India",    815.70,  0.003 },   // stable
        { "BAJFINANCE", "Bajaj Finance",         7285.40,  0.005 },   // moderate
        { "ASIANPAINT", "Asian Paints",          2895.60,  0.002 },   // very stable
        { "HINDUNILVR", "Hindustan Unilever",    2534.80,  0.001 },   // most stable
        { "KOTAKBANK",  "Kotak Mahindra Bank",   1834.20,  0.002 },   // very stable
        { "LT",         "Larsen & Toubro",       3478.90,  0.003 },   // stable
        { "SUNPHARMA",  "Sun Pharmaceutical",    1567.30,  0.003 }    // stable
    };

    // In-memory price store: symbol -> current marketData
    private final Map<String, marketData> priceMap = new ConcurrentHashMap<>();

    private final Random random = new Random();
    private final PriceEventPublisher priceEventPublisher;

    public PriceSimulatorService(PriceEventPublisher priceEventPublisher) {
        this.priceEventPublisher = priceEventPublisher;
        initializePrices();
    }

    private void initializePrices() {
        String startTime = LocalDateTime.now().format(FORMATTER);
        for (Object[] config : EQUITY_CONFIG) {
            String symbol    = (String) config[0];
            String name      = (String) config[1];
            double basePrice = (Double) config[2];
            priceMap.put(symbol, new marketData(
                symbol, name,
                basePrice, basePrice, basePrice,
                0.0, 0.0, startTime
            ));
        }
    }

    /**
     * Simulates price tick every 5 seconds.
     * Updates currentPrice with realistic random walk.
     * Updates change% and dailyChange%.
     *
     * After updating prices, publishes PriceUpdatedEvent:
     *   - LOCAL mode: logs occasionally to console
     *   - AWS mode:   publishes to SNS topic "price-updated"
     */
    @Scheduled(fixedDelay = 5000)
    public void simulatePriceTick() {
        String now = LocalDateTime.now().format(FORMATTER);

        for (Object[] config : EQUITY_CONFIG) {
            String symbol = (String) config[0];
            double vol    = (Double) config[3];

            marketData current = priceMap.get(symbol);
            if (current == null) continue;

            double prev    = current.getCurrentPrice();
            double opening = current.getOpeningPrice();

            double change   = (random.nextDouble() * 2 - 1) * vol;
            double newPrice = Math.round(prev * (1 + change) * 100.0) / 100.0;
            if (newPrice < opening * 0.10) newPrice = opening * 0.10;

            double tickChange  = Math.round(((newPrice - prev) / prev * 100) * 100.0) / 100.0;
            double dailyChange = Math.round(((newPrice - opening) / opening * 100) * 100.0) / 100.0;

            marketData updated = new marketData(
                symbol, current.getStockName(),
                newPrice, opening, prev,
                tickChange, dailyChange, now
            );

            priceMap.put(symbol, updated);

            // Publish PriceUpdated event (SNS in AWS mode, console in local mode)
            if (priceEventPublisher != null) {
                priceEventPublisher.publishPriceUpdate(updated);
            }
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
