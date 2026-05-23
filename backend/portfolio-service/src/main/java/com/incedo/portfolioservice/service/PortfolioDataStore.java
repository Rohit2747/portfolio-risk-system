package com.incedo.portfolioservice.service;

import com.incedo.portfolioservice.model.Holding;
import com.incedo.portfolioservice.model.portfolio;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PortfolioDataStore
 *
 * Generates 100 simulated client portfolios, each with a different
 * mix of holdings from 20 equities.
 *
 * CRITICAL: Quantities are computed FROM target allocations and base prices
 * so that actual allocation % matches target allocation % at opening.
 * This ensures drift only occurs as prices move over time.
 *
 * Client groups & expected risk behavior:
 *   1–60   : Conservative/Balanced → LOW risk (no breaches at open)
 *   61–85  : Growth → MEDIUM risk (may develop drift as volatile stocks move)
 *   86–100 : Aggressive → HIGH risk (intentional concentration >20%)
 *
 * Target allocations across holdings always sum to 100%.
 */
@Service
public class PortfolioDataStore {

    // ---------------------------------------------------------------
    // 20 Equities: symbol, name, base price (used for quantity calculation)
    // ---------------------------------------------------------------
    public static final String[][] EQUITIES = {
        // { symbol, name }
        { "AAPL",   "Apple Inc."          },
        { "MSFT",   "Microsoft Corp."     },
        { "NVDA",   "NVIDIA Corp."        },
        { "AMZN",   "Amazon.com Inc."     },
        { "GOOGL",  "Alphabet Inc."       },
        { "META",   "Meta Platforms"      },
        { "TSLA",   "Tesla Inc."          },
        { "RELIANCE","Reliance Industries"},
        { "HDFCBANK","HDFC Bank"          },
        { "INFY",   "Infosys Ltd."        },
        { "TCS",    "TCS Ltd."            },
        { "WIPRO",  "Wipro Ltd."          },
        { "ICICIBANK","ICICI Bank"        },
        { "SBIN",   "State Bank of India" },
        { "BAJFINANCE","Bajaj Finance"    },
        { "ASIANPAINT","Asian Paints"     },
        { "HINDUNILVR","Hindustan Unilever"},
        { "KOTAKBANK","Kotak Mahindra Bank"},
        { "LT",     "Larsen & Toubro"     },
        { "SUNPHARMA","Sun Pharmaceutical"}
    };

    // Base prices must match PriceSimulatorService opening prices exactly
    private static final double PRICE_AAPL       = 187.50;
    private static final double PRICE_MSFT       = 415.20;
    private static final double PRICE_NVDA       = 875.40;
    private static final double PRICE_AMZN       = 182.30;
    private static final double PRICE_GOOGL      = 175.60;
    private static final double PRICE_META       = 505.80;
    private static final double PRICE_TSLA       = 172.40;
    private static final double PRICE_RELIANCE   = 2985.50;
    private static final double PRICE_HDFCBANK   = 1678.90;
    private static final double PRICE_INFY       = 183.25;
    private static final double PRICE_TCS        = 3912.00;
    private static final double PRICE_WIPRO      = 538.60;
    private static final double PRICE_ICICIBANK  = 1087.30;
    private static final double PRICE_SBIN       = 815.70;
    private static final double PRICE_BAJFINANCE = 7285.40;
    private static final double PRICE_ASIANPAINT = 2895.60;
    private static final double PRICE_HINDUNILVR = 2534.80;
    private static final double PRICE_KOTAKBANK  = 1834.20;
    private static final double PRICE_LT         = 3478.90;
    private static final double PRICE_SUNPHARMA  = 1567.30;

    /**
     * Returns all 100 client portfolios.
     * Value is set to 0.0 here — the Risk Service will compute it
     * dynamically using live prices from Market Data Service.
     */
    public List<portfolio> getAllPortfolios() {
        List<portfolio> portfolios = new ArrayList<>();

        for (int i = 1; i <= 100; i++) {
            List<Holding> holdings = generateHoldingsForClient(i);
            portfolios.add(new portfolio(
                i,
                "Client-" + String.format("%03d", i),
                0.0,           // will be computed by Risk Service
                "UNKNOWN",     // will be set by Risk Service
                holdings
            ));
        }

        return portfolios;
    }

    /**
     * Returns a single portfolio by clientId.
     */
    public portfolio getPortfolioById(int clientId) {
        return getAllPortfolios().stream()
            .filter(p -> p.getClientId() == clientId)
            .findFirst()
            .orElse(null);
    }

    /**
     * Generates holdings for a specific client based on their client number.
     *
     * Risk distribution design:
     *   1–60   : Conservative/Balanced → LOW risk at opening (well diversified)
     *   61–85  : Growth → starts LOW/MEDIUM, drifts to MEDIUM as prices move
     *   86–100 : Aggressive → HIGH risk (intentional concentration breaches)
     */
    private List<Holding> generateHoldingsForClient(int clientId) {
        if (clientId <= 30) {
            return buildConservativeHoldings(clientId);
        } else if (clientId <= 60) {
            return buildBalancedHoldings(clientId);
        } else if (clientId <= 85) {
            return buildGrowthHoldings(clientId);
        } else {
            return buildAggressiveHoldings(clientId);
        }
    }

    /**
     * Computes quantity from target allocation, portfolio budget, and stock price.
     * quantity = (targetPercent/100 * budget) / price
     * Ensures at least 1 share.
     */
    private int computeQuantity(double targetPercent, double budget, double price) {
        int qty = (int) Math.round((targetPercent / 100.0 * budget) / price);
        return Math.max(1, qty);
    }

    // ---------------------------------------------------------------
    // CONSERVATIVE PORTFOLIO: 8 stable holdings, well diversified
    // Max target = 18% — well under 20% concentration threshold
    // All drift starts at ~0% at opening prices
    // Expected risk: LOW
    // ---------------------------------------------------------------
    private List<Holding> buildConservativeHoldings(int clientId) {
        // Budget varies by client for variety: ₹5L to ₹8L
        double budget = 500000 + (clientId % 10) * 30000;

        return Arrays.asList(
            new Holding("HDFCBANK",   "HDFC Bank",            computeQuantity(18.0, budget, PRICE_HDFCBANK),   18.0),
            new Holding("HINDUNILVR", "Hindustan Unilever",   computeQuantity(16.0, budget, PRICE_HINDUNILVR), 16.0),
            new Holding("KOTAKBANK",  "Kotak Mahindra Bank",  computeQuantity(14.0, budget, PRICE_KOTAKBANK),  14.0),
            new Holding("SBIN",       "State Bank of India",  computeQuantity(13.0, budget, PRICE_SBIN),       13.0),
            new Holding("TCS",        "TCS Ltd.",             computeQuantity(12.0, budget, PRICE_TCS),        12.0),
            new Holding("INFY",       "Infosys Ltd.",         computeQuantity(11.0, budget, PRICE_INFY),       11.0),
            new Holding("SUNPHARMA",  "Sun Pharmaceutical",   computeQuantity(9.0,  budget, PRICE_SUNPHARMA),   9.0),
            new Holding("ASIANPAINT", "Asian Paints",         computeQuantity(7.0,  budget, PRICE_ASIANPAINT),   7.0)
        );
    }

    // ---------------------------------------------------------------
    // BALANCED PORTFOLIO: 10 holdings, mix of sectors
    // Max target = 14% — well under 20% concentration threshold
    // Expected risk: LOW
    // ---------------------------------------------------------------
    private List<Holding> buildBalancedHoldings(int clientId) {
        double budget = 600000 + (clientId % 10) * 25000;

        return Arrays.asList(
            new Holding("AAPL",      "Apple Inc.",           computeQuantity(14.0, budget, PRICE_AAPL),      14.0),
            new Holding("MSFT",      "Microsoft Corp.",      computeQuantity(13.0, budget, PRICE_MSFT),      13.0),
            new Holding("RELIANCE",  "Reliance Industries",  computeQuantity(12.0, budget, PRICE_RELIANCE),  12.0),
            new Holding("HDFCBANK",  "HDFC Bank",            computeQuantity(11.0, budget, PRICE_HDFCBANK),  11.0),
            new Holding("TCS",       "TCS Ltd.",             computeQuantity(10.0, budget, PRICE_TCS),       10.0),
            new Holding("INFY",      "Infosys Ltd.",         computeQuantity(10.0, budget, PRICE_INFY),      10.0),
            new Holding("ICICIBANK", "ICICI Bank",           computeQuantity(9.0,  budget, PRICE_ICICIBANK),  9.0),
            new Holding("LT",        "Larsen & Toubro",      computeQuantity(8.0,  budget, PRICE_LT),         8.0),
            new Holding("WIPRO",     "Wipro Ltd.",           computeQuantity(7.0,  budget, PRICE_WIPRO),      7.0),
            new Holding("SUNPHARMA", "Sun Pharmaceutical",   computeQuantity(6.0,  budget, PRICE_SUNPHARMA),  6.0)
        );
    }

    // ---------------------------------------------------------------
    // GROWTH PORTFOLIO: 10 holdings, tech-heavy with volatile stocks
    // Max target = 16% — under 20% but volatile stocks (NVDA 3.5%, TSLA 4%)
    // can drift over 5% threshold, triggering MEDIUM risk
    // Expected risk: starts LOW → drifts to MEDIUM over time
    // ---------------------------------------------------------------
    private List<Holding> buildGrowthHoldings(int clientId) {
        double budget = 700000 + (clientId % 10) * 35000;

        return Arrays.asList(
            new Holding("NVDA",      "NVIDIA Corp.",         computeQuantity(16.0, budget, PRICE_NVDA),      16.0),
            new Holding("AAPL",      "Apple Inc.",           computeQuantity(14.0, budget, PRICE_AAPL),      14.0),
            new Holding("MSFT",      "Microsoft Corp.",      computeQuantity(13.0, budget, PRICE_MSFT),      13.0),
            new Holding("META",      "Meta Platforms",       computeQuantity(12.0, budget, PRICE_META),      12.0),
            new Holding("AMZN",      "Amazon.com Inc.",      computeQuantity(11.0, budget, PRICE_AMZN),      11.0),
            new Holding("GOOGL",     "Alphabet Inc.",        computeQuantity(10.0, budget, PRICE_GOOGL),     10.0),
            new Holding("TSLA",      "Tesla Inc.",           computeQuantity(9.0,  budget, PRICE_TSLA),       9.0),
            new Holding("RELIANCE",  "Reliance Industries",  computeQuantity(7.0,  budget, PRICE_RELIANCE),   7.0),
            new Holding("TCS",       "TCS Ltd.",             computeQuantity(4.0,  budget, PRICE_TCS),        4.0),
            new Holding("BAJFINANCE","Bajaj Finance",        computeQuantity(4.0,  budget, PRICE_BAJFINANCE), 4.0)
        );
    }

    // ---------------------------------------------------------------
    // AGGRESSIVE PORTFOLIO: 6 holdings, concentrated in high-vol stocks
    // INTENTIONAL BREACHES:
    //   - NVDA target 35% → exceeds 20% concentration threshold
    //   - TSLA target 28% → exceeds 20% concentration threshold
    // These will ALWAYS trigger CONCENTRATION_RISK → HIGH
    // Expected risk: HIGH (by design)
    // ---------------------------------------------------------------
    private List<Holding> buildAggressiveHoldings(int clientId) {
        double budget = 800000 + (clientId % 10) * 40000;

        return Arrays.asList(
            new Holding("NVDA",   "NVIDIA Corp.",    computeQuantity(35.0, budget, PRICE_NVDA),  35.0),   // >20% — intentional breach
            new Holding("TSLA",   "Tesla Inc.",      computeQuantity(28.0, budget, PRICE_TSLA),  28.0),   // >20% — intentional breach
            new Holding("META",   "Meta Platforms",  computeQuantity(17.0, budget, PRICE_META),  17.0),
            new Holding("AMZN",   "Amazon.com Inc.", computeQuantity(10.0, budget, PRICE_AMZN),  10.0),
            new Holding("GOOGL",  "Alphabet Inc.",   computeQuantity(6.0,  budget, PRICE_GOOGL),  6.0),
            new Holding("AAPL",   "Apple Inc.",      computeQuantity(4.0,  budget, PRICE_AAPL),   4.0)
        );
    }
}
