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
    // GROWTH PORTFOLIO → Expected: MEDIUM risk
    //
    // Tech-heavy with volatile stocks (NVDA, TSLA, META).
    // Quantities intentionally cause allocation drift > 5% for some stocks
    // but NO single stock exceeds 20% concentration.
    //
    // Target portfolio value: ~₹1,20,000
    // ---------------------------------------------------------------
    private List<Holding> buildGrowthHoldings(int clientId) {
        int v = clientId % 5;
        // NVDA is expensive (₹875) — give it enough shares to drift above target
        // but not enough to breach 20% concentration
        return Arrays.asList(
            new Holding("NVDA",      "NVIDIA Corp.",         18 + v, 10.0),   // ~₹15,757 = ~13% (drift 3% from 10%)
            new Holding("MSFT",      "Microsoft Corp.",      30 + v, 10.0),   // ~₹12,456 = ~10%
            new Holding("AAPL",      "Apple Inc.",           55 + v, 10.0),   // ~₹10,312 = ~9%
            new Holding("META",      "Meta Platforms",       22 + v,  8.0),   // ~₹11,127 = ~9% (drift ~1%)
            new Holding("AMZN",      "Amazon.com Inc.",      45 + v, 10.0),   // ~₹8,203  = ~7% (drift 3%)
            new Holding("TSLA",      "Tesla Inc.",           55 + v,  6.0),   // ~₹9,482  = ~8% (drift 2%)
            new Holding("GOOGL",     "Alphabet Inc.",        40 + v,  9.0),   // ~₹7,024  = ~6% (drift 3%)
            new Holding("TCS",       "TCS Ltd.",             4 + v,  12.0),   // ~₹15,648 = ~13% (drift 1%)
            new Holding("BAJFINANCE","Bajaj Finance",        2 + v,  10.0),   // ~₹14,570 = ~12% (drift 2%)
            new Holding("RELIANCE",  "Reliance Industries",  5 + v,  15.0)    // ~₹14,927 = ~12% (drift 3%)
        );
    }

    // ---------------------------------------------------------------
    // AGGRESSIVE PORTFOLIO → Expected: HIGH risk
    //
    // Concentrated in just 5-6 volatile stocks.
    // NVDA and TSLA intentionally exceed 20% concentration threshold.
    //
    // Target portfolio value: ~₹2,00,000
    // ---------------------------------------------------------------
    private List<Holding> buildAggressiveHoldings(int clientId) {
        int v = clientId % 5;
        // NVDA at ₹875 × 60+ shares = ₹52,500+ which is >25% of ~₹2,00,000
        // TSLA at ₹172 × 300+ shares = ₹51,720+ which is >25% of ~₹2,00,000
        return Arrays.asList(
            new Holding("NVDA",   "NVIDIA Corp.",    60 + v*5, 15.0),   // ~₹52,500 = ~26% (BREACH >20%)
            new Holding("TSLA",   "Tesla Inc.",     300 + v*10, 15.0),  // ~₹51,720 = ~26% (BREACH >20%)
            new Holding("META",   "Meta Platforms",  30 + v*3, 20.0),   // ~₹15,174 = ~8%
            new Holding("AMZN",   "Amazon.com Inc.",100 + v*5, 20.0),   // ~₹18,230 = ~9%
            new Holding("GOOGL",  "Alphabet Inc.",   80 + v*5, 15.0),   // ~₹14,048 = ~7%
            new Holding("AAPL",   "Apple Inc.",     150 + v*5, 15.0)    // ~₹28,125 = ~14%
        );
    }
}
