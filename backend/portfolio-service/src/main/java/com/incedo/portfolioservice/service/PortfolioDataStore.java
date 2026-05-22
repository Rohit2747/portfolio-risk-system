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
 * Each portfolio uses one of 4 model allocations:
 *  - CONSERVATIVE  (Clients 1-25)  : LOW risk — well-diversified, no breaches
 *  - BALANCED      (Clients 26-50) : LOW risk — evenly spread, no breaches
 *  - GROWTH        (Clients 51-75) : MEDIUM risk — some allocation drift
 *  - AGGRESSIVE    (Clients 76-100): HIGH risk — intentional concentration breaches
 *
 * Target allocations across holdings always sum to 100%.
 *
 * IMPORTANT: Quantities are carefully calibrated against BASE PRICES
 * from PriceSimulatorService to ensure the actual value percentages
 * match the target allocations closely.
 *
 * Base prices used for calibration:
 *   AAPL=187.50, MSFT=415.20, NVDA=875.40, AMZN=182.30, GOOGL=175.60
 *   META=505.80, TSLA=172.40, RELIANCE=2985.50, HDFCBANK=1678.90
 *   INFY=183.25, TCS=3912.00, WIPRO=538.60, ICICIBANK=1087.30
 *   SBIN=815.70, BAJFINANCE=7285.40, ASIANPAINT=2895.60
 *   HINDUNILVR=2534.80, KOTAKBANK=1834.20, LT=3478.90, SUNPHARMA=1567.30
 */
@Service
public class PortfolioDataStore {

    // ---------------------------------------------------------------
    // 20 Equities: symbol, name
    // ---------------------------------------------------------------
    public static final String[][] EQUITIES = {
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

    public List<portfolio> getAllPortfolios() {
        List<portfolio> portfolios = new ArrayList<>();

        for (int i = 1; i <= 100; i++) {
            List<Holding> holdings = generateHoldingsForClient(i);
            portfolios.add(new portfolio(
                i,
                "Client-" + String.format("%03d", i),
                0.0,
                "UNKNOWN",
                holdings
            ));
        }

        return portfolios;
    }

    public portfolio getPortfolioById(int clientId) {
        return getAllPortfolios().stream()
            .filter(p -> p.getClientId() == clientId)
            .findFirst()
            .orElse(null);
    }

    /**
     * Client groups (NOT sequential risk — mixed across the dashboard):
     *   1–25   : Conservative → LOW risk
     *   26–50  : Balanced → LOW risk
     *   51–75  : Growth → MEDIUM risk (drift from volatile stocks)
     *   76–100 : Aggressive → HIGH risk (concentration breach)
     */
    private List<Holding> generateHoldingsForClient(int clientId) {
        if (clientId <= 25) {
            return buildConservativeHoldings(clientId);
        } else if (clientId <= 50) {
            return buildBalancedHoldings(clientId);
        } else if (clientId <= 75) {
            return buildGrowthHoldings(clientId);
        } else {
            return buildAggressiveHoldings(clientId);
        }
    }

    // ---------------------------------------------------------------
    // CONSERVATIVE PORTFOLIO → Expected: LOW risk
    //
    // All stocks are low-volatility Indian large-caps with similar prices.
    // Quantities calibrated so each stock's value is close to its target.
    // No stock should exceed 20%, drift should be < 5%.
    //
    // Target portfolio value: ~₹1,00,000
    // All stocks between ₹800 and ₹2000 range — easy to balance
    // ---------------------------------------------------------------
    private List<Holding> buildConservativeHoldings(int clientId) {
        int v = clientId % 5;  // variation 0-4
        // Using stocks with SIMILAR prices to avoid concentration
        // SBIN=815, ICICIBANK=1087, HDFCBANK=1678, SUNPHARMA=1567, WIPRO=538, INFY=183
        return Arrays.asList(
            new Holding("SBIN",      "State Bank of India",  12 + v, 15.0),    // ~₹9,788  = ~15%
            new Holding("ICICIBANK", "ICICI Bank",           9 + v,  15.0),    // ~₹9,785  = ~15%
            new Holding("HDFCBANK",  "HDFC Bank",            6 + v,  15.0),    // ~₹10,073 = ~15%
            new Holding("SUNPHARMA", "Sun Pharmaceutical",   6 + v,  14.0),    // ~₹9,403  = ~14%
            new Holding("WIPRO",     "Wipro Ltd.",           14 + v, 11.0),    // ~₹7,540  = ~11%
            new Holding("KOTAKBANK", "Kotak Mahindra Bank",  4 + v,  11.0),    // ~₹7,336  = ~11%
            new Holding("INFY",      "Infosys Ltd.",         35 + v, 10.0),    // ~₹6,413  = ~10%
            new Holding("AAPL",      "Apple Inc.",           28 + v,  9.0)     // ~₹5,250  = ~9%
        );
    }

    // ---------------------------------------------------------------
    // BALANCED PORTFOLIO → Expected: LOW risk
    //
    // Mix of Indian and US stocks, but quantities carefully set.
    // All positions well under 20% concentration.
    // Drift should stay within 5% threshold.
    //
    // Target portfolio value: ~₹1,50,000
    // ---------------------------------------------------------------
    private List<Holding> buildBalancedHoldings(int clientId) {
        int v = clientId % 5;
        return Arrays.asList(
            new Holding("RELIANCE",  "Reliance Industries",  6 + v,  12.0),   // ~₹17,913 = ~12%
            new Holding("HDFCBANK",  "HDFC Bank",            10 + v, 11.0),   // ~₹16,789 = ~11%
            new Holding("TCS",       "TCS Ltd.",             4 + v,  10.0),   // ~₹15,648 = ~10%
            new Holding("MSFT",      "Microsoft Corp.",      25 + v, 10.0),   // ~₹10,380 = ~7% (drift ~3%)
            new Holding("INFY",      "Infosys Ltd.",         60 + v, 10.0),   // ~₹10,995 = ~7% (drift ~3%)
            new Holding("ICICIBANK", "ICICI Bank",           12 + v, 10.0),   // ~₹13,047 = ~9%
            new Holding("SBIN",      "State Bank of India",  15 + v,  9.0),   // ~₹12,235 = ~8%
            new Holding("LT",        "Larsen & Toubro",      4 + v,   9.0),   // ~₹13,915 = ~9%
            new Holding("AAPL",      "Apple Inc.",           50 + v,  9.0),   // ~₹9,375  = ~6% (drift ~3%)
            new Holding("WIPRO",     "Wipro Ltd.",           20 + v, 10.0)    // ~₹10,772 = ~7% (drift ~3%)
        );
    }

    // ---------------------------------------------------------------
    // GROWTH PORTFOLIO → Expected: MEDIUM risk (ALLOCATION_DRIFT)
    //
    // Strategy: Set TARGETS that intentionally DON'T match the actual
    // value distribution. This guarantees drift > 5% for several stocks.
    //
    // Key insight: drift = |actual% - target%|
    // If actual is 18% but target is 8%, drift = 10% → BREACH!
    //
    // But NO single stock exceeds 20% → no CONCENTRATION_RISK
    // This gives MEDIUM (drift only) not HIGH.
    //
    // Portfolio value: ~₹1,00,000
    // ---------------------------------------------------------------
    private List<Holding> buildGrowthHoldings(int clientId) {
        int v = clientId % 5;
        // TCS @ ₹3912 × 5 shares = ₹19,560 = ~19% actual BUT target is 8% → drift 11%!
        // BAJFINANCE @ ₹7285 × 2 shares = ₹14,570 = ~14% actual BUT target is 6% → drift 8%!
        // AAPL @ ₹187 × 10 shares = ₹1,875 = ~2% actual BUT target is 12% → drift 10%!
        // This creates guaranteed ALLOCATION_DRIFT breaches without CONCENTRATION breach
        return Arrays.asList(
            new Holding("TCS",       "TCS Ltd.",             5 + v,   8.0),   // actual ~19%, target 8% → drift 11% BREACH
            new Holding("BAJFINANCE","Bajaj Finance",        2 + v,   6.0),   // actual ~14%, target 6% → drift 8% BREACH
            new Holding("NVDA",      "NVIDIA Corp.",         12 + v,  7.0),   // actual ~13%, target 7% → drift 6% BREACH
            new Holding("HDFCBANK",  "HDFC Bank",            7 + v,  12.0),   // actual ~12%, target 12% → OK
            new Holding("RELIANCE",  "Reliance Industries",  3 + v,  12.0),   // actual ~9%, target 12% → drift 3% OK
            new Holding("MSFT",      "Microsoft Corp.",      12 + v, 15.0),   // actual ~5%, target 15% → drift 10% BREACH
            new Holding("AAPL",      "Apple Inc.",           10 + v, 15.0),   // actual ~2%, target 15% → drift 13% BREACH
            new Holding("META",      "Meta Platforms",       8 + v,  10.0),   // actual ~4%, target 10% → drift 6% BREACH
            new Holding("ICICIBANK", "ICICI Bank",           8 + v,   8.0),   // actual ~9%, target 8% → OK
            new Holding("SBIN",      "State Bank of India",  10 + v,  7.0)    // actual ~8%, target 7% → OK
        );
    }

    // ---------------------------------------------------------------
    // AGGRESSIVE PORTFOLIO → Expected: HIGH risk
    //
    // Shows CONCENTRATION_RISK + ALLOCATION_DRIFT breaches.
    //
    // Different clients are over-concentrated in DIFFERENT stocks
    // (realistic — not everyone makes the same mistake).
    //
    // 5 variations based on clientId % 5:
    //   Group 0: Over-concentrated in TCS
    //   Group 1: Over-concentrated in BAJFINANCE
    //   Group 2: Over-concentrated in RELIANCE
    //   Group 3: Over-concentrated in LT (Larsen & Toubro)
    //   Group 4: Over-concentrated in HDFCBANK
    // ---------------------------------------------------------------
    private List<Holding> buildAggressiveHoldings(int clientId) {
        int group = clientId % 5;

        return switch (group) {
            case 0 -> Arrays.asList(
                // Over-concentrated in TCS (₹3912)
                new Holding("TCS",       "TCS Ltd.",            9,  10.0),   // actual ~40% → CONCENTRATION
                new Holding("BAJFINANCE","Bajaj Finance",       2,  15.0),   // actual ~17%
                new Holding("AAPL",      "Apple Inc.",         25,  20.0),   // actual ~5% → DRIFT
                new Holding("MSFT",      "Microsoft Corp.",    12,  20.0),   // actual ~6% → DRIFT
                new Holding("SBIN",      "State Bank of India",12,  15.0),   // actual ~11%
                new Holding("INFY",      "Infosys Ltd.",       60,  20.0)    // actual ~13% → DRIFT
            );
            case 1 -> Arrays.asList(
                // Over-concentrated in BAJFINANCE (₹7285)
                new Holding("BAJFINANCE","Bajaj Finance",       4,  10.0),   // actual ~45% → CONCENTRATION
                new Holding("TCS",       "TCS Ltd.",            3,  15.0),   // actual ~18%
                new Holding("GOOGL",     "Alphabet Inc.",      30,  20.0),   // actual ~8% → DRIFT
                new Holding("AMZN",      "Amazon.com Inc.",    30,  20.0),   // actual ~8% → DRIFT
                new Holding("WIPRO",     "Wipro Ltd.",         15,  15.0),   // actual ~12%
                new Holding("SUNPHARMA", "Sun Pharmaceutical",  3,  20.0)    // actual ~7% → DRIFT
            );
            case 2 -> Arrays.asList(
                // Over-concentrated in RELIANCE (₹2985)
                new Holding("RELIANCE",  "Reliance Industries", 8,  10.0),   // actual ~38% → CONCENTRATION
                new Holding("HDFCBANK",  "HDFC Bank",           5,  15.0),   // actual ~13%
                new Holding("META",      "Meta Platforms",      5,  20.0),   // actual ~4% → DRIFT
                new Holding("AAPL",      "Apple Inc.",          30,  20.0),   // actual ~9% → DRIFT
                new Holding("ICICIBANK", "ICICI Bank",          8,  15.0),   // actual ~14%
                new Holding("NVDA",      "NVIDIA Corp.",        3,  20.0)    // actual ~4% → DRIFT
            );
            case 3 -> Arrays.asList(
                // Over-concentrated in LT (₹3478)
                new Holding("LT",        "Larsen & Toubro",     7,  10.0),   // actual ~37% → CONCENTRATION
                new Holding("KOTAKBANK", "Kotak Mahindra Bank", 4,  15.0),   // actual ~11%
                new Holding("TSLA",      "Tesla Inc.",          40,  20.0),   // actual ~10% → DRIFT
                new Holding("GOOGL",     "Alphabet Inc.",       25,  20.0),   // actual ~7% → DRIFT
                new Holding("HINDUNILVR","Hindustan Unilever",  3,  15.0),   // actual ~12%
                new Holding("SBIN",      "State Bank of India", 15,  20.0)    // actual ~19%
            );
            case 4 -> Arrays.asList(
                // Over-concentrated in HDFCBANK (₹1678) — needs more shares
                new Holding("HDFCBANK",  "HDFC Bank",          12,  10.0),   // actual ~35% → CONCENTRATION
                new Holding("ASIANPAINT","Asian Paints",        3,  15.0),   // actual ~15%
                new Holding("AMZN",      "Amazon.com Inc.",    20,  20.0),   // actual ~6% → DRIFT
                new Holding("MSFT",      "Microsoft Corp.",     8,  20.0),   // actual ~6% → DRIFT
                new Holding("SUNPHARMA", "Sun Pharmaceutical",  3,  15.0),   // actual ~8%
                new Holding("INFY",      "Infosys Ltd.",       80,  20.0)    // actual ~25% → CONCENTRATION
            );
            default -> Arrays.asList(
                new Holding("TCS",       "TCS Ltd.",            9,  10.0),
                new Holding("BAJFINANCE","Bajaj Finance",       2,  15.0),
                new Holding("AAPL",      "Apple Inc.",         25,  20.0),
                new Holding("MSFT",      "Microsoft Corp.",    12,  20.0),
                new Holding("SBIN",      "State Bank of India",12,  15.0),
                new Holding("INFY",      "Infosys Ltd.",       60,  20.0)
            );
        };
    }
}
