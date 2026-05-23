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
        // Conservative: ONLY low-volatility stocks (vol <= 0.012)
        // All 8 stocks have similar volatility so portfolio stays balanced
        // Target = 12.5% each (100/8). Actual will stay within 3-4% of target.
        // LOW volatility stocks: HDFCBANK(0.010), TCS(0.010), RELIANCE(0.012),
        //   ICICIBANK(0.012), ASIANPAINT(0.012), KOTAKBANK(0.012), HINDUNILVR(0.008), MSFT(0.012)
        int[] lowVolStocks = {8, 10, 7, 12, 15, 17, 16, 1};
        // basePrices:        1678, 3912, 2985, 1087, 2895, 1834, 2534, 415

        List<Holding> holdings = new ArrayList<>();
        int start = clientId % lowVolStocks.length;
        double portfolioSize = 100000.0;

        for (int i = 0; i < 8; i++) {
            int stockIdx = lowVolStocks[(start + i) % lowVolStocks.length];
            String symbol = STOCK_INFO[stockIdx][0];
            String name = STOCK_INFO[stockIdx][1];
            int basePrice = Integer.parseInt(STOCK_INFO[stockIdx][2]);

            // Each stock targets 12.5% of portfolio
            double targetPercent = 12.5;
            int qty = Math.max(1, (int) Math.round((targetPercent / 100.0 * portfolioSize) / basePrice));

            // Set target to the ACTUAL computed allocation so drift starts at 0%
            double actualPercent = (qty * basePrice) / portfolioSize * 100.0;
            actualPercent = Math.round(actualPercent * 10.0) / 10.0;

            holdings.add(new Holding(symbol, name, qty, actualPercent));
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
        // Balanced: 10 low-to-medium volatility stocks at 10% each
        // Using ONLY stocks with volatility <= 0.015
        // This ensures drift stays well under 8% threshold
        int[] balancedStocks = {7, 8, 10, 1, 12, 13, 15, 17, 16, 18};
        // RELIANCE, HDFCBANK, TCS, MSFT, ICICIBANK, SBIN, ASIANPAINT, KOTAKBANK, HINDUNILVR, LT

        List<Holding> holdings = new ArrayList<>();
        int start = (clientId - 31) % 4;
        double portfolioSize = 120000.0;

        for (int i = 0; i < 10; i++) {
            int stockIdx = balancedStocks[(start + i) % balancedStocks.length];
            String symbol = STOCK_INFO[stockIdx][0];
            String name = STOCK_INFO[stockIdx][1];
            int basePrice = Integer.parseInt(STOCK_INFO[stockIdx][2]);

            // Each stock targets 10% of portfolio
            double targetPercent = 10.0;
            int qty = Math.max(1, (int) Math.round((targetPercent / 100.0 * portfolioSize) / basePrice));

            // Set target to ACTUAL computed allocation
            double actualPercent = (qty * basePrice) / portfolioSize * 100.0;
            actualPercent = Math.round(actualPercent * 10.0) / 10.0;

            holdings.add(new Holding(symbol, name, qty, actualPercent));
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

        // Growth portfolios: intentional ALLOCATION_DRIFT but NO concentration (all < 20%)
        // Strategy: use ONLY mid-to-cheap stocks (< ₹2000) with 10 holdings each
        // Some holdings have actual% HIGH but target% LOW → DRIFT
        // Some holdings have actual% LOW but target% HIGH → DRIFT
        // With 10 holdings of similar-priced stocks, no single one exceeds 15%
        //
        // Each config: {stockIndex, qty, fakeTarget%}
        int[][] allDriftConfigs = {
            // Stocks with qty that gives ~15% actual, target says 3% → drift ~12% ✓
            {12, 8,  3},   // ICICIBANK (1087×8=8696) ~15% actual, target 3% → drift 12%
            {13, 10, 3},   // SBIN (815×10=8150) ~14% actual, target 3% → drift 11%
            {8,  5,  3},   // HDFCBANK (1678×5=8390) ~14% actual, target 3% → drift 11%
            {11, 14, 3},   // WIPRO (538×14=7532) ~13% actual, target 3% → drift 10%
            {19, 5,  3},   // SUNPHARMA (1567×5=7835) ~13% actual, target 3% → drift 10%
            {17, 4,  3},   // KOTAKBANK (1834×4=7336) ~13% actual, target 3% → drift 10%
            // Stocks with low qty giving ~2% actual, target says 20% → drift ~18% ✓
            {0,  6, 20},   // AAPL (187×6=1122) ~2% actual, target 20% → drift 18%
            {3,  6, 20},   // AMZN (182×6=1092) ~2% actual, target 20% → drift 18%
            {4,  6, 20},   // GOOGL (175×6=1050) ~2% actual, target 20% → drift 18%
            {6,  6, 20},   // TSLA (172×6=1032) ~2% actual, target 20% → drift 18%
            {9,  6, 20},   // INFY (183×6=1098) ~2% actual, target 20% → drift 18%
            {1,  3, 20},   // MSFT (415×3=1245) ~2% actual, target 20% → drift 18%
            {5,  2, 20},   // META (505×2=1010) ~2% actual, target 20% → drift 18%
            {2,  1, 15},   // NVDA (875×1=875) ~1.5% actual, target 15% → drift 13.5%
            {18, 1, 15},   // LT (3478×1=3478) ~6% actual, target 15% → drift 9%
        };

        // Each client gets 10 stocks from the 15 configs (more holdings = safer distribution)
        int startOffset = ((clientId - 61) * 2) % 15;
        List<Holding> holdings = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int cfgIdx = (startOffset + i) % allDriftConfigs.length;
            int stockIdx = allDriftConfigs[cfgIdx][0];
            int baseQty = allDriftConfigs[cfgIdx][1];
            double target = allDriftConfigs[cfgIdx][2];

            String symbol = STOCK_INFO[stockIdx][0];
            String name = STOCK_INFO[stockIdx][1];

            // Minimal client variation (±1 share for stocks with qty > 5)
            int qty = baseQty;
            if (baseQty > 5) {
                qty = baseQty + rand.nextInt(3) - 1;
            }
            if (qty < 1) qty = 1;

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
