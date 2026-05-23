package com.incedo.portfolioservice.service;

import com.incedo.portfolioservice.model.Holding;
import com.incedo.portfolioservice.model.portfolio;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * PortfolioDataStore
 *
 * Generates 100 unique client portfolios using a seeded random approach.
 * Each client gets a unique combination of stocks and quantities based on
 * their clientId as a seed — ensuring deterministic but varied output.
 *
 * Risk distribution:
 *   Clients 1–30  : Conservative → LOW risk (no breaches)
 *   Clients 31–60 : Balanced → LOW risk (no breaches)
 *   Clients 61–85 : Growth → MODERATE risk (allocation drift only)
 *   Clients 86–100: Aggressive → HIGH risk (concentration + drift)
 *
 * Base prices used for calibration:
 *   AAPL=187, MSFT=415, NVDA=875, AMZN=182, GOOGL=175, META=505, TSLA=172
 *   RELIANCE=2985, HDFCBANK=1678, INFY=183, TCS=3912, WIPRO=538
 *   ICICIBANK=1087, SBIN=815, BAJFINANCE=7285, ASIANPAINT=2895
 *   HINDUNILVR=2534, KOTAKBANK=1834, LT=3478, SUNPHARMA=1567
 */
@Service
public class PortfolioDataStore {

    // 20 Equities with base prices for quantity calculation
    private static final String[][] STOCK_INFO = {
        // symbol, name, basePrice
        { "AAPL",       "Apple Inc.",            "187"  },
        { "MSFT",       "Microsoft Corp.",       "415"  },
        { "NVDA",       "NVIDIA Corp.",          "875"  },
        { "AMZN",       "Amazon.com Inc.",       "182"  },
        { "GOOGL",      "Alphabet Inc.",         "175"  },
        { "META",       "Meta Platforms",        "505"  },
        { "TSLA",       "Tesla Inc.",            "172"  },
        { "RELIANCE",   "Reliance Industries",   "2985" },
        { "HDFCBANK",   "HDFC Bank",             "1678" },
        { "INFY",       "Infosys Ltd.",          "183"  },
        { "TCS",        "TCS Ltd.",              "3912" },
        { "WIPRO",      "Wipro Ltd.",            "538"  },
        { "ICICIBANK",  "ICICI Bank",            "1087" },
        { "SBIN",       "State Bank of India",   "815"  },
        { "BAJFINANCE", "Bajaj Finance",         "7285" },
        { "ASIANPAINT", "Asian Paints",          "2895" },
        { "HINDUNILVR", "Hindustan Unilever",    "2534" },
        { "KOTAKBANK",  "Kotak Mahindra Bank",   "1834" },
        { "LT",         "Larsen & Toubro",       "3478" },
        { "SUNPHARMA",  "Sun Pharmaceutical",    "1567" }
    };

    // Public accessor for equities list (used by controller)
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

    // ---------------------------------------------------------------
    // CONSERVATIVE → LOW risk
    // 8 equally-weighted stocks (~12.5% each)
    // Targets set to match actual allocation closely (within 2-3%)
    // This ensures NO DRIFT breach even with price fluctuations
    // ---------------------------------------------------------------
    private List<Holding> buildConservativeHoldings(int clientId) {
        Random rand = new Random(clientId * 31L);
        // Conservative: 8 equally-weighted stocks (~12.5% each)
        // Targets set to match actual allocation closely (within 2-3%)
        // This ensures NO DRIFT breach even with price fluctuations
        int[][] stockPool = {
            {13, 12}, // SBIN
            {12, 13}, // ICICIBANK
            {8,  12}, // HDFCBANK
            {19, 13}, // SUNPHARMA
            {11, 12}, // WIPRO
            {17, 13}, // KOTAKBANK
            {9,  12}, // INFY
            {0,  13}, // AAPL
        };

        List<Holding> holdings = new ArrayList<>();
        int start = clientId % stockPool.length;

        for (int i = 0; i < 8; i++) {
            int idx = (start + i) % stockPool.length;
            int stockIdx = stockPool[idx][0];
            // Target allocation is what we expect the actual to be (~12-13%)
            double target = stockPool[idx][1] + (rand.nextInt(2));

            String symbol = STOCK_INFO[stockIdx][0];
            String name = STOCK_INFO[stockIdx][1];
            int basePrice = Integer.parseInt(STOCK_INFO[stockIdx][2]);

            // Calculate quantity to MATCH the target allocation of ~80,000 portfolio
            int qty = Math.max(1, (int) Math.round((target / 100.0 * 80000) / basePrice));

            // Set target to actual calculated allocation (so drift is ~0%)
            double actualTarget = (qty * basePrice) / 80000.0 * 100.0;
            actualTarget = Math.round(actualTarget * 10.0) / 10.0;

            holdings.add(new Holding(symbol, name, qty, actualTarget));
        }
        return holdings;
    }

    // ---------------------------------------------------------------
    // BALANCED → LOW risk
    // 10 stocks with weights around 10% each
    // Targets set to match actual so NO breaches occur
    // ---------------------------------------------------------------
    private List<Holding> buildBalancedHoldings(int clientId) {
        Random rand = new Random(clientId * 47L);
        // Balanced: 10 stocks with weights around 10% each
        // Targets set to match actual so NO breaches occur
        int[][] stockPool = {
            {7,  10}, // RELIANCE
            {8,  10}, // HDFCBANK
            {10, 10}, // TCS
            {1,  10}, // MSFT
            {9,  10}, // INFY
            {12, 10}, // ICICIBANK
            {13, 10}, // SBIN
            {18, 10}, // LT
            {0,  10}, // AAPL
            {11, 10}, // WIPRO
        };

        List<Holding> holdings = new ArrayList<>();
        int start = (clientId - 31) % 4;

        for (int i = 0; i < 10; i++) {
            int idx = (start + i) % stockPool.length;
            int stockIdx = stockPool[idx][0];
            double target = stockPool[idx][1] + (rand.nextInt(2));

            String symbol = STOCK_INFO[stockIdx][0];
            String name = STOCK_INFO[stockIdx][1];
            int basePrice = Integer.parseInt(STOCK_INFO[stockIdx][2]);

            int qty = Math.max(1, (int) Math.round((target / 100.0 * 120000) / basePrice));

            // Set target to match actual allocation (prevents drift breach)
            double actualTarget = (qty * basePrice) / 120000.0 * 100.0;
            actualTarget = Math.round(actualTarget * 10.0) / 10.0;

            holdings.add(new Holding(symbol, name, qty, actualTarget));
        }
        return holdings;
    }

    // ---------------------------------------------------------------
    // GROWTH → MEDIUM risk (ALLOCATION_DRIFT only, no concentration)
    // Each client gets DIFFERENT stocks drifting from their targets.
    // Multiple drift breaches per client (3-5 stocks drifting).
    // No stock exceeds 20% → no CONCENTRATION_RISK.
    // Uses 15 different stock configs rotated per client.
    // ---------------------------------------------------------------
    private List<Holding> buildGrowthHoldings(int clientId) {
        Random rand = new Random(clientId * 67L);

        // 15 possible drift-causing configs: {stockIndex, baseQty, target%}
        // Quantities kept low for expensive stocks to prevent >20% concentration
        // Expensive stocks with moderate shares, low target → DRIFT
        // Cheap stocks with few shares, high target → DRIFT
        int[][] allDriftConfigs = {
            {10, 3,  7},   // TCS (3912) → moderate value, low target → DRIFT
            {14, 1,  5},   // BAJFINANCE (7285) → moderate value, low target → DRIFT
            {2,  8,  6},   // NVDA (875) → moderate value, low target → DRIFT
            {7,  2,  6},   // RELIANCE (2985) → moderate value, low target → DRIFT
            {18, 2,  6},   // LT (3478) → moderate value, low target → DRIFT
            {15, 2,  6},   // ASIANPAINT (2895) → moderate value, low target → DRIFT
            {0, 12, 18},   // AAPL (187) → low value, high target → DRIFT
            {1,  5, 16},   // MSFT (415) → low value, high target → DRIFT
            {3, 12, 16},   // AMZN (182) → low value, high target → DRIFT
            {4, 12, 16},   // GOOGL (175) → low value, high target → DRIFT
            {6, 12, 16},   // TSLA (172) → low value, high target → DRIFT
            {9, 12, 14},   // INFY (183) → low value, high target → DRIFT
            {8,  4, 12},   // HDFCBANK → moderate
            {12, 5,  8},   // ICICIBANK → moderate
            {13, 7,  7},   // SBIN → moderate
        };

        // Each client gets a different selection of 8 stocks from the 15
        int startOffset = ((clientId - 61) * 3) % 15;
        List<Holding> holdings = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            int cfgIdx = (startOffset + i * 2) % allDriftConfigs.length;
            int stockIdx = allDriftConfigs[cfgIdx][0];
            int baseQty = allDriftConfigs[cfgIdx][1];
            double target = allDriftConfigs[cfgIdx][2];

            String symbol = STOCK_INFO[stockIdx][0];
            String name = STOCK_INFO[stockIdx][1];

            // Client-specific variation
            int qty = baseQty + rand.nextInt(4) - 1;
            if (qty < 1) qty = 1;
            target += rand.nextInt(3) - 1;
            if (target < 3) target = 3;

            holdings.add(new Holding(symbol, name, qty, target));
        }
        return holdings;
    }

    // ---------------------------------------------------------------
    // AGGRESSIVE → HIGH risk (CONCENTRATION_RISK + ALLOCATION_DRIFT)
    // Each client has TWO dominant stocks causing concentration (>20%).
    // Plus allocation drift on remaining stocks.
    // This shows multiple breach types per client.
    // ---------------------------------------------------------------
    private List<Holding> buildAggressiveHoldings(int clientId) {
        Random rand = new Random(clientId * 89L);

        // Pairs of dominant stocks — each client gets 2 concentrated positions
        // {stockIndex, quantity} — each pair causes >20% concentration
        int[][][] dominantPairs = {
            {{10, 8}, {14, 3}},   // TCS + BAJFINANCE
            {{14, 4}, {7, 7}},    // BAJFINANCE + RELIANCE
            {{7, 8}, {18, 6}},    // RELIANCE + LT
            {{18, 7}, {8, 11}},   // LT + HDFCBANK
            {{8, 12}, {15, 7}},   // HDFCBANK + ASIANPAINT
            {{15, 7}, {16, 8}},   // ASIANPAINT + HINDUNILVR
            {{16, 8}, {17, 10}},  // HINDUNILVR + KOTAKBANK
            {{17, 11}, {10, 7}},  // KOTAKBANK + TCS
            {{10, 9}, {7, 6}},    // TCS + RELIANCE
            {{14, 3}, {18, 7}},   // BAJFINANCE + LT
        };

        // Pick pair based on clientId
        int pairIdx = (clientId - 86) % 10;
        int[][] pair = dominantPairs[pairIdx];

        int dom1StockIdx = pair[0][0];
        int dom1Qty = pair[0][1] + rand.nextInt(2);
        int dom2StockIdx = pair[1][0];
        int dom2Qty = pair[1][1] + rand.nextInt(2);

        List<Holding> holdings = new ArrayList<>();

        // First dominant stock — target 10% but actual >25% → CONCENTRATION + DRIFT
        holdings.add(new Holding(
            STOCK_INFO[dom1StockIdx][0], STOCK_INFO[dom1StockIdx][1],
            dom1Qty, 10.0
        ));

        // Second dominant stock — target 10% but actual >20% → CONCENTRATION + DRIFT
        holdings.add(new Holding(
            STOCK_INFO[dom2StockIdx][0], STOCK_INFO[dom2StockIdx][1],
            dom2Qty, 10.0
        ));

        // Pick 4 supporting stocks (small positions with drift)
        int[] supportOptions = {0, 1, 3, 4, 5, 6, 9, 11, 13, 19};
        int supportStart = (clientId * 3) % supportOptions.length;

        for (int i = 0; i < 4; i++) {
            int sIdx = supportOptions[(supportStart + i) % supportOptions.length];
            // Avoid duplicates with dominant stocks
            if (sIdx == dom1StockIdx || sIdx == dom2StockIdx) {
                sIdx = supportOptions[(supportStart + i + 5) % supportOptions.length];
            }

            String symbol = STOCK_INFO[sIdx][0];
            String name = STOCK_INFO[sIdx][1];
            int basePrice = Integer.parseInt(STOCK_INFO[sIdx][2]);

            // Small quantity but HIGH target → creates ALLOCATION_DRIFT breach too
            double target = 15.0 + rand.nextInt(6);
            int qty = Math.max(1, (int) Math.round((5.0 / 100.0 * 50000) / basePrice));
            qty += rand.nextInt(3);

            holdings.add(new Holding(symbol, name, qty, target));
        }

        return holdings;
    }
}
