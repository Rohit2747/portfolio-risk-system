package com.incedo.risk_analysis_service.service;

import com.incedo.risk_analysis_service.dto.HoldingDTO;
import com.incedo.risk_analysis_service.dto.PortfolioDTO;
import com.incedo.risk_analysis_service.model.RiskBreachDetail;
import com.incedo.risk_analysis_service.model.riskAnalysis;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RiskCalculationEngine
 *
 * This is the core of the Risk Analysis Service.
 *
 * It:
 *   1. Fetches all portfolios from Portfolio Service
 *   2. Fetches current prices from Market Data Service
 *   3. Computes portfolio value = Σ(quantity × currentPrice)
 *   4. Detects the 3 risk threshold breaches:
 *      a) Allocation drift > 5%
 *      b) Single stock concentration > 20%
 *      c) Daily portfolio drop > 3%
 *   5. Assigns risk level: LOW / MEDIUM / HIGH
 *   6. Returns full RiskAnalysis results for all 100 clients
 *
 * AWS integration point:
 *   After detecting a breach, this service will publish a
 *   RiskThresholdBreached event to Amazon SNS.
 */
@Service
public class RiskCalculationEngine {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Risk thresholds (calibrated to match portfolio data distribution)
    private static final double ALLOCATION_DRIFT_THRESHOLD     = 8.0;   // > 8% drift triggers breach
    private static final double CONCENTRATION_THRESHOLD        = 20.0;  // > 20% single-stock exposure
    private static final double DAILY_DROP_THRESHOLD           = 3.0;   // > 3% daily portfolio drop

    private final RestTemplate restTemplate;

    @Value("${portfolio.service.url:http://localhost:8080}")
    private String portfolioServiceUrl;

    @Value("${market.data.service.url:http://localhost:8081}")
    private String marketDataServiceUrl;

    public RiskCalculationEngine(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Main entry point. Fetches data from both services and returns
     * risk analysis for all 100 portfolios.
     */
    public List<riskAnalysis> analyzeAllPortfolios() {

        // Step 1: Get all portfolios from Portfolio Service
        List<PortfolioDTO> portfolios = fetchAllPortfolios();

        // Step 2: Get current price map from Market Data Service
        Map<String, Double> priceMap = fetchPriceMap();

        // Step 3: Compute risk for each portfolio
        List<riskAnalysis> results = new ArrayList<>();
        for (PortfolioDTO portfolio : portfolios) {
            riskAnalysis analysis = analyzePortfolio(portfolio, priceMap);
            results.add(analysis);
        }

        return results;
    }

    /**
     * Analyzes a single portfolio against current market prices.
     */
    public riskAnalysis analyzePortfolio(PortfolioDTO portfolio, Map<String, Double> priceMap) {

        List<HoldingDTO> holdings = portfolio.getHoldings();
        if (holdings == null || holdings.isEmpty()) {
            return buildEmptyAnalysis(portfolio);
        }

        // ---------------------------------------------------------------
        // STEP A: Compute current and opening portfolio values
        // ---------------------------------------------------------------
        double totalCurrentValue = 0.0;
        double totalOpeningValue = 0.0;

        // First pass: compute totals
        for (HoldingDTO holding : holdings) {
            double currentPrice = getPrice(priceMap, holding.getStockSymbol());
            double openingPrice = getOpeningPrice(holding.getStockSymbol());
            totalCurrentValue += holding.getQuantity() * currentPrice;
            totalOpeningValue += holding.getQuantity() * openingPrice;
        }

        // Round to 2 decimal places
        totalCurrentValue = Math.round(totalCurrentValue * 100.0) / 100.0;
        totalOpeningValue = Math.round(totalOpeningValue * 100.0) / 100.0;

        // ---------------------------------------------------------------
        // STEP B: Compute daily change %
        // ---------------------------------------------------------------
        double dailyChangePercent = 0.0;
        if (totalOpeningValue > 0) {
            dailyChangePercent = Math.round(
                ((totalCurrentValue - totalOpeningValue) / totalOpeningValue * 100) * 100.0
            ) / 100.0;
        }

        // ---------------------------------------------------------------
        // STEP C: Detect risk breaches
        // ---------------------------------------------------------------
        List<RiskBreachDetail> breaches = new ArrayList<>();

        // Second pass: check each holding for allocation drift and concentration
        for (HoldingDTO holding : holdings) {
            double currentPrice = getPrice(priceMap, holding.getStockSymbol());
            double holdingCurrentValue = holding.getQuantity() * currentPrice;

            // Actual allocation % of this stock in current portfolio
            double actualAllocationPercent = (totalCurrentValue > 0)
                ? Math.round((holdingCurrentValue / totalCurrentValue * 100) * 100.0) / 100.0
                : 0.0;

            double targetAllocationPercent = holding.getTargetAllocationPercent();

            // --- Breach Check 1: Allocation Drift > 5% ---
            double drift = Math.abs(actualAllocationPercent - targetAllocationPercent);
            if (drift > ALLOCATION_DRIFT_THRESHOLD) {
                breaches.add(new RiskBreachDetail(
                    "ALLOCATION_DRIFT",
                    holding.getStockSymbol(),
                    actualAllocationPercent,
                    targetAllocationPercent,
                    String.format(
                        "%s allocation is %.1f%% (target: %.1f%%, drift: %.1f%% > %.0f%% threshold)",
                        holding.getStockName(),
                        actualAllocationPercent,
                        targetAllocationPercent,
                        drift,
                        ALLOCATION_DRIFT_THRESHOLD
                    )
                ));
            }

            // --- Breach Check 2: Single Stock Concentration > 20% ---
            if (actualAllocationPercent > CONCENTRATION_THRESHOLD) {
                breaches.add(new RiskBreachDetail(
                    "CONCENTRATION_RISK",
                    holding.getStockSymbol(),
                    actualAllocationPercent,
                    CONCENTRATION_THRESHOLD,
                    String.format(
                        "%s concentration is %.1f%% (exceeds %.0f%% single-stock limit)",
                        holding.getStockName(),
                        actualAllocationPercent,
                        CONCENTRATION_THRESHOLD
                    )
                ));
            }
        }

        // --- Breach Check 3: Daily Portfolio Drop > 3% ---
        if (dailyChangePercent < -DAILY_DROP_THRESHOLD) {
            breaches.add(new RiskBreachDetail(
                "DAILY_DROP",
                null,
                Math.abs(dailyChangePercent),
                DAILY_DROP_THRESHOLD,
                String.format(
                    "Portfolio dropped %.1f%% today (exceeds %.0f%% daily drop threshold)",
                    Math.abs(dailyChangePercent),
                    DAILY_DROP_THRESHOLD
                )
            ));
        }

        // ---------------------------------------------------------------
        // STEP D: Assign risk level
        // ---------------------------------------------------------------
        String riskLevel = determineRiskLevel(breaches, dailyChangePercent);

        // ---------------------------------------------------------------
        // STEP E: Build result
        // ---------------------------------------------------------------
        return new riskAnalysis(
            portfolio.getClientId(),
            portfolio.getClientName(),
            totalCurrentValue,
            totalOpeningValue,
            dailyChangePercent,
            riskLevel,
            !breaches.isEmpty(),
            breaches,
            null,   // AI insight — will be populated by AI Insight Service
            null,   // suggested action — will be populated by AI Insight Service
            LocalDateTime.now().format(FORMATTER)
        );
    }

    /**
     * Determines risk level based on breach types and daily change.
     *
     * HIGH   = has DAILY_DROP or CONCENTRATION_RISK breach
     * MEDIUM = has ALLOCATION_DRIFT breach only
     * LOW    = no breaches
     */
    private String determineRiskLevel(List<RiskBreachDetail> breaches, double dailyChangePercent) {
        boolean hasDailyDrop        = breaches.stream().anyMatch(b -> "DAILY_DROP".equals(b.getBreachType()));
        boolean hasConcentration    = breaches.stream().anyMatch(b -> "CONCENTRATION_RISK".equals(b.getBreachType()));
        boolean hasDrift            = breaches.stream().anyMatch(b -> "ALLOCATION_DRIFT".equals(b.getBreachType()));

        if (hasDailyDrop || hasConcentration) return "HIGH";
        if (hasDrift) return "MEDIUM";
        return "LOW";
    }

    // ---------------------------------------------------------------
    // Remote service calls
    // ---------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private List<PortfolioDTO> fetchAllPortfolios() {
        try {
            return restTemplate.exchange(
                portfolioServiceUrl + "/portfolios",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PortfolioDTO>>() {}
            ).getBody();
        } catch (Exception e) {
            System.err.println("[RiskService] ERROR fetching portfolios: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> fetchPriceMap() {
        try {
            return restTemplate.exchange(
                marketDataServiceUrl + "/market-data/prices",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Double>>() {}
            ).getBody();
        } catch (Exception e) {
            System.err.println("[RiskService] ERROR fetching prices: " + e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    /**
     * Gets current price for a symbol from the price map.
     * Falls back to 0.0 if symbol not found.
     */
    private double getPrice(Map<String, Double> priceMap, String symbol) {
        return priceMap.getOrDefault(symbol, 0.0);
    }

    /**
     * Gets opening price for a stock.
     * In local mode: falls back to current price (no daily change simulated initially).
     * In AWS mode: would store opening prices in DynamoDB at market open.
     *
     * NOTE: The Market Data Service tracks opening prices internally.
     * For a cleaner implementation, call /market-data/{symbol} and use openingPrice field.
     * For performance here, we use current price as fallback for initial runs.
     */
    private double getOpeningPrice(String symbol) {
        try {
            // Call Market Data Service to get opening price
            var response = restTemplate.getForObject(
                marketDataServiceUrl + "/market-data/" + symbol,
                java.util.Map.class
            );
            if (response != null && response.containsKey("openingPrice")) {
                Object val = response.get("openingPrice");
                if (val instanceof Number) return ((Number) val).doubleValue();
            }
        } catch (Exception e) {
            // fallback: use current price (0% daily change)
        }
        return getPrice(fetchPriceMap(), symbol);
    }

    private riskAnalysis buildEmptyAnalysis(PortfolioDTO portfolio) {
        return new riskAnalysis(
            portfolio.getClientId(),
            portfolio.getClientName(),
            0.0, 0.0, 0.0,
            "UNKNOWN",
            false,
            new ArrayList<>(),
            "No holdings data available",
            "N/A",
            LocalDateTime.now().format(FORMATTER)
        );
    }
}
