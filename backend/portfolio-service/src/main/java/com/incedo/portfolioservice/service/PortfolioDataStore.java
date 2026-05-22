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
 *   Clients 1–25  : Conservative → LOW risk (no breaches)
 *   Clients 26–50 : Balanced → LOW risk (no breaches)
 *   Clients 51–75 : Growth → MEDIUM risk (allocation drift only)
 *   Clients 76–100: Aggressive → HIGH risk (concentration + drift)
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
    // CONSERVATIVE → LOW risk
    // Each client gets a unique subset of 7-8 stocks from a pool of stable stocks.
    // Quantities calibrated so no stock exceeds 18% of portfolio.
    // ---------------------------------------------------------------
    private List<Holding> buildConservativeHoldings(int clientId) {
        Random rand = new Random(clientId * 31L);
        // Pool of stable, similarly-priced stocks
        int[][] stockPool = {
            {13, 15}, // SBIN (815) — index 13
            {12, 14}, // ICICIBANK (1087) — index 12
            {8,  14}, // HDFCBANK (1678) — index 8
            {19, 13}, // SUNPHARMA (1567) — index 19
            {11, 11}, // WIPRO (538) — index 11
            {17, 12}, // KOTAKBANK (1834) — index 17
            {9,  10}, // INFY (183) — index 9
            {0,   9}, // AAPL (187) — index 0
            {14,  5}, // BAJFINANCE (7285) — index 14 (tiny qty)
            {7,   6}, // RELIANCE (2985) — index 7 (tiny qty)
        };

        // Shuffle selection based on clientId
        int start = clientId % 3;
        int totalTarget = 100;
        List<Holding> holdings = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            int idx = (start + i) % stockPool.length;
            int stockIdx = stockPool[idx][0];
            double target = stockPool[idx][1] + (rand.nextInt(3) - 1);  // slight variation ±1%

            String symbol = STOCK_INFO[stockIdx][0];
            String name = STOCK_INFO[stockIdx][1];
            int basePrice = Integer.parseInt(STOCK_INFO[stockIdx][2]);

            // Calculate quantity to match target% of ~₹80,000 portfolio
            int qty = Math.max(1, (int) Math.round((target / 100.0 * 80000) / basePrice));
            qty += rand.nextInt(3) - 1; // ±1 share variation
            if (qty < 1) qty = 1;

            holdings.add(new Holding(symbol, name, qty, target));
        }
        return holdings;
    }

    // ---------------------------------------------------------------
    // BALANCED → LOW risk
    // Mix of Indian + US stocks. Each client gets different quantities.
    // ---------------------------------------------------------------
    private List<Holding> buildBalancedHoldings(int clientId) {
        Random rand = new Random(clientId * 47L);
        // 10 stocks, targets near actual (drift < 5%)
        int[][] stockPool = {
            {7,  12}, // RELIANCE
            {8,  11}, // HDFCBANK
            {10, 10}, // TCS
            {1,  10}, // MSFT
            {9,  10}, // INFY
            {12, 10}, // ICICIBANK
            {13,  9}, // SBIN
            {18,  9}, // LT
            {0,   9}, // AAPL
            {11, 10}, // WIPRO
        };

        int start = (clientId - 26) % 4;
        List<Holding> holdings = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int idx = (start + i) % stockPool.length;
            int stockIdx = stockPool[idx][0];
            double target = stockPool[idx][1] + (rand.nextInt(3) - 1);

            String symbol = STOCK_INFO[stockIdx][0];
            String name = STOCK_INFO[stockIdx][1];
            int basePrice = Integer.parseInt(STOCK_INFO[stockIdx][2]);

            int qty = Math.max(1, (int) Math.round((target / 100.0 * 120000) / basePrice));
            qty += rand.nextInt(3) - 1;
            if (qty < 1) qty = 1;

            holdings.add(new Holding(symbol, name, qty, target));
        }
        return holdings;
    }

    // ---------------------------------------------------------------
    // GROWTH → MEDIUM risk (ALLOCATION_DRIFT only, no concentration)
    // Each client gets different stocks drifting from their targets.
    // Some stocks have HIGH actual % but LOW target → drift > 5%
    // No stock exceeds 20% → no CONCENTRATION_RISK
    // ---------------------------------------------------------------
    private List<Holding> buildGrowthHoldings(int clientId) {
        Random rand = new Random(clientId * 67L);

        // Pool: {stockIndex, quantity multiplier, target %}
        // Expensive stocks get many shares (high actual%) but LOW target → DRIFT BREACH
        // Cheap stocks get few shares (low actual%) but HIGH target → DRIFT BREACH
        int[][] driftConfigs = {
            {10, 5,  8},  // TCS (₹3912) → high value, low target → DRIFT
            {14, 2,  6},  // BAJFINANCE (₹7285) → high value, low target → DRIFT
            {2, 10,  7},  // NVDA (₹875) → moderate value, low target → DRIFT
            {8,  7, 12},  // HDFCBANK → matches target (OK)
            {7,  3, 12},  // RELIANCE → matches target (OK)
            {1, 12, 15},  // MSFT (₹415) → low value, high target → DRIFT
            {0, 10, 15},  // AAPL (₹187) → low value, high target → DRIFT
            {5,  6, 10},  // META (₹505) → moderate
            {12, 8,  8},  // ICICIBANK → matches (OK)
            {13, 10, 7},  // SBIN → matches (OK)
        };

        // Rotate which stocks are included based on clientId
        int rotation = (clientId - 51) % 5;
        List<Holding> holdings = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int cfgIdx = (i + rotation) % driftConfigs.length;
            int stockIdx = driftConfigs[cfgIdx][0];
            int baseQty = driftConfigs[cfgIdx][1];
            double target = driftConfigs[cfgIdx][2];

            String symbol = STOCK_INFO[stockIdx][0];
            String name = STOCK_INFO[stockIdx][1];

            // Add client-specific variation
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
    // Each client has a DIFFERENT dominant stock causing concentration.
    // Uses 10 different "problem stocks" rotating by clientId.
    // ---------------------------------------------------------------
    private List<Holding> buildAggressiveHoldings(int clientId) {
        Random rand = new Random(clientId * 89L);

        // 10 possible "dominant" stocks that can cause concentration
        // Each has enough value at reasonable quantities to exceed 20%
        int[][] dominantStocks = {
            {10, 9},   // TCS (₹3912) × 9 = ₹35,208
            {14, 4},   // BAJFINANCE (₹7285) × 4 = ₹29,140
            {7,  8},   // RELIANCE (₹2985) × 8 = ₹23,880
            {18, 7},   // LT (₹3478) × 7 = ₹24,346
            {8, 12},   // HDFCBANK (₹1678) × 12 = ₹20,136
            {15, 7},   // ASIANPAINT (₹2895) × 7 = ₹20,265
            {16, 8},   // HINDUNILVR (₹2534) × 8 = ₹20,272
            {17, 11},  // KOTAKBANK (₹1834) × 11 = ₹20,174
            {19, 13},  // SUNPHARMA (₹1567) × 13 = ₹20,371
            {12, 19},  // ICICIBANK (₹1087) × 19 = ₹20,653
        };

        // Pick dominant stock based on clientId (each client gets a different one)
        int dominantIdx = (clientId - 76) % 10;
        int domStockIdx = dominantStocks[dominantIdx][0];
        int domQty = dominantStocks[dominantIdx][1] + rand.nextInt(3);

        String domSymbol = STOCK_INFO[domStockIdx][0];
        String domName = STOCK_INFO[domStockIdx][1];

        // Build portfolio: 1 dominant + 5 supporting stocks
        List<Holding> holdings = new ArrayList<>();
        holdings.add(new Holding(domSymbol, domName, domQty, 10.0)); // target 10% but actual >30% → CONCENTRATION + DRIFT

        // Pick 5 different supporting stocks (small positions)
        int[] supportOptions = {0, 1, 3, 4, 5, 6, 9, 11, 13};  // cheap stocks
        int supportStart = (clientId * 3) % supportOptions.length;

        double remainingTarget = 90.0;
        for (int i = 0; i < 5; i++) {
            int sIdx = supportOptions[(supportStart + i) % supportOptions.length];
            if (sIdx == domStockIdx) sIdx = (sIdx + 1) % 20; // avoid duplicate

            String symbol = STOCK_INFO[sIdx][0];
            String name = STOCK_INFO[sIdx][1];
            int basePrice = Integer.parseInt(STOCK_INFO[sIdx][2]);

            double target = remainingTarget / (5 - i) + (rand.nextInt(5) - 2);
            if (target < 10) target = 10;
            if (target > 25) target = 25;

            // Small quantity — these won't breach anything
            int qty = Math.max(1, (int) Math.round((8.0 / 100.0 * 60000) / basePrice));
            qty += rand.nextInt(5);

            holdings.add(new Holding(symbol, name, qty, target));
            remainingTarget -= target;
        }

        return holdings;
    }
}
