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
 *  - CONSERVATIVE  : bonds-heavy, low equity
 *  - BALANCED      : equal mix
 *  - GROWTH        : equity-heavy
 *  - AGGRESSIVE    : high risk, concentrated equity
 *
 * Target allocations across holdings always sum to 100%.
 */
@Service
public class PortfolioDataStore {

    // ---------------------------------------------------------------
    // 20 Equities: symbol, name, base price (used for initial valuation)
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
     * Client groups:
     *   1–25   : Conservative (low risk profile)
     *   26–50  : Balanced
     *   51–75  : Growth
     *   76–100 : Aggressive (high risk profile)
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
    // CONSERVATIVE PORTFOLIO: 8 holdings, spread across stable stocks
    // Total target allocation = 100%
    // Quantities are calibrated to match target allocation % by value
    // (high-price stocks get fewer shares, low-price stocks get more)
    // ---------------------------------------------------------------
    private List<Holding> buildConservativeHoldings(int clientId) {
        int v = (clientId % 5) + 1;  // variation factor 1-5
        // Target portfolio ~₹10L. Quantities set so value% ≈ target%
        // HDFCBANK=₹1679, so 18% of 10L = ₹1.8L → ~107 shares
        // TCS=₹3912, so 12% of 10L = ₹1.2L → ~30 shares
        return Arrays.asList(
            new Holding("HDFCBANK",  "HDFC Bank",            100 + v*2,  18.0),
            new Holding("HINDUNILVR","Hindustan Unilever",    60 + v*2,  16.0),
            new Holding("KOTAKBANK", "Kotak Mahindra Bank",   72 + v*2,  14.0),
            new Holding("SBIN",      "State Bank of India",  160 + v*3,  14.0),
            new Holding("TCS",       "TCS Ltd.",              28 + v,    12.0),
            new Holding("INFY",      "Infosys Ltd.",         500 + v*10, 10.0),
            new Holding("SUNPHARMA", "Sun Pharmaceutical",   48 + v*2,   8.0),
            new Holding("ASIANPAINT","Asian Paints",          26 + v,    8.0)
        );
    }

    // ---------------------------------------------------------------
    // BALANCED PORTFOLIO: 10 holdings, mix of sectors
    // Total target allocation = 100%
    // Quantities calibrated so actual allocation ≈ target allocation
    // Should result in LOW or MEDIUM risk (minor drift possible)
    // ---------------------------------------------------------------
    private List<Holding> buildBalancedHoldings(int clientId) {
        int v = (clientId % 5) + 1;
        // Target portfolio ~₹8L
        return Arrays.asList(
            new Holding("AAPL",      "Apple Inc.",           50 + v*3,  12.0),
            new Holding("MSFT",      "Microsoft Corp.",      22 + v*2,  12.0),
            new Holding("RELIANCE",  "Reliance Industries",  30 + v*2,  12.0),
            new Holding("HDFCBANK",  "HDFC Bank",            45 + v*2,  10.0),
            new Holding("TCS",       "TCS Ltd.",             19 + v,    10.0),
            new Holding("INFY",      "Infosys Ltd.",        400 + v*10, 10.0),
            new Holding("ICICIBANK", "ICICI Bank",           55 + v*2,   8.0),
            new Holding("LT",        "Larsen & Toubro",      17 + v,     8.0),
            new Holding("WIPRO",     "Wipro Ltd.",          120 + v*5,   9.0),
            new Holding("SUNPHARMA", "Sun Pharmaceutical",   43 + v*2,   9.0)
        );
    }

    // ---------------------------------------------------------------
    // GROWTH PORTFOLIO: 10 holdings, more tech/growth stocks
    // Total target allocation = 100%
    // Tech-heavy — some allocation drift expected (MEDIUM risk)
    // Quantities slightly off-target to create realistic drift
    // ---------------------------------------------------------------
    private List<Holding> buildGrowthHoldings(int clientId) {
        int v = (clientId % 5) + 1;
        // Target portfolio ~₹6L. Some stocks intentionally over-weighted by qty
        return Arrays.asList(
            new Holding("NVDA",      "NVIDIA Corp.",         12 + v,    15.0),  // ₹875 × 12 = ₹10.5K
            new Holding("AAPL",      "Apple Inc.",           55 + v*3,  15.0),  // ₹187 × 55 = ₹10.3K
            new Holding("MSFT",      "Microsoft Corp.",      20 + v*2,  12.0),
            new Holding("AMZN",      "Amazon.com Inc.",      45 + v*3,  12.0),
            new Holding("GOOGL",     "Alphabet Inc.",        40 + v*2,  10.0),
            new Holding("META",      "Meta Platforms",       14 + v,    10.0),
            new Holding("TSLA",      "Tesla Inc.",           32 + v*2,   8.0),
            new Holding("RELIANCE",  "Reliance Industries",  18 + v,     8.0),
            new Holding("TCS",       "TCS Ltd.",              9 + v,     5.0),
            new Holding("BAJFINANCE","Bajaj Finance",         5 + v,     5.0)
        );
    }

    // ---------------------------------------------------------------
    // AGGRESSIVE PORTFOLIO: 6 holdings, concentrated in high-vol stocks
    // Total target allocation = 100%
    // NOTE: Quantities intentionally create >20% concentration in NVDA/TSLA
    // This guarantees CONCENTRATION_RISK breach → HIGH risk level
    // ---------------------------------------------------------------
    private List<Holding> buildAggressiveHoldings(int clientId) {
        int v = (clientId % 5) + 1;
        // NVDA and TSLA get disproportionally high quantities
        // to ensure concentration breach (>20% of portfolio value)
        return Arrays.asList(
            new Holding("NVDA",   "NVIDIA Corp.",    30 + v*3, 30.0),   // ₹875×33 = ₹28.9K → ~35% of portfolio
            new Holding("TSLA",   "Tesla Inc.",     100 + v*5, 25.0),   // ₹172×105 = ₹18.1K → ~22%
            new Holding("META",   "Meta Platforms",  10 + v,   20.0),   // ₹505×11 = ₹5.6K → ~7% (under target)
            new Holding("AMZN",   "Amazon.com Inc.", 30 + v*2, 10.0),
            new Holding("GOOGL",  "Alphabet Inc.",   25 + v*2,  8.0),
            new Holding("AAPL",   "Apple Inc.",      20 + v*2,  7.0)
        );
    }
}
