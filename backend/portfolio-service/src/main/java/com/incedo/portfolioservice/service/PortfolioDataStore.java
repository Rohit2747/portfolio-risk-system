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
    // ---------------------------------------------------------------
    private List<Holding> buildConservativeHoldings(int clientId) {
        int base = (clientId % 5) * 10 + 50;  // quantity variation: 50–90
        return Arrays.asList(
            new Holding("HDFCBANK",  "HDFC Bank",            base + 20, 20.0),
            new Holding("HINDUNILVR","Hindustan Unilever",   base + 10, 18.0),
            new Holding("KOTAKBANK", "Kotak Mahindra Bank",  base,      15.0),
            new Holding("SBIN",      "State Bank of India",  base,      15.0),
            new Holding("TCS",       "TCS Ltd.",             base - 10, 12.0),
            new Holding("INFY",      "Infosys Ltd.",         base - 10, 10.0),
            new Holding("SUNPHARMA", "Sun Pharmaceutical",  base - 20,  5.0),
            new Holding("ASIANPAINT","Asian Paints",        base - 20,  5.0)
        );
    }

    // ---------------------------------------------------------------
    // BALANCED PORTFOLIO: 10 holdings, mix of sectors
    // Total target allocation = 100%
    // ---------------------------------------------------------------
    private List<Holding> buildBalancedHoldings(int clientId) {
        int base = (clientId % 5) * 8 + 40;
        return Arrays.asList(
            new Holding("AAPL",      "Apple Inc.",           base + 30, 12.0),
            new Holding("MSFT",      "Microsoft Corp.",      base + 25, 12.0),
            new Holding("RELIANCE",  "Reliance Industries",  base + 20, 12.0),
            new Holding("HDFCBANK",  "HDFC Bank",            base + 15, 10.0),
            new Holding("TCS",       "TCS Ltd.",             base + 10, 10.0),
            new Holding("INFY",      "Infosys Ltd.",         base + 5,  10.0),
            new Holding("ICICIBANK", "ICICI Bank",           base,       8.0),
            new Holding("LT",        "Larsen & Toubro",      base,       8.0),
            new Holding("WIPRO",     "Wipro Ltd.",           base - 5,   9.0),
            new Holding("SUNPHARMA", "Sun Pharmaceutical",  base - 10,   9.0)
        );
    }

    // ---------------------------------------------------------------
    // GROWTH PORTFOLIO: 10 holdings, more tech/growth stocks
    // Total target allocation = 100%
    // ---------------------------------------------------------------
    private List<Holding> buildGrowthHoldings(int clientId) {
        int base = (clientId % 5) * 12 + 60;
        return Arrays.asList(
            new Holding("NVDA",      "NVIDIA Corp.",         base + 20, 15.0),
            new Holding("AAPL",      "Apple Inc.",           base + 15, 15.0),
            new Holding("MSFT",      "Microsoft Corp.",      base + 10, 12.0),
            new Holding("AMZN",      "Amazon.com Inc.",      base + 10, 12.0),
            new Holding("GOOGL",     "Alphabet Inc.",        base + 5,  10.0),
            new Holding("META",      "Meta Platforms",       base,      10.0),
            new Holding("TSLA",      "Tesla Inc.",           base - 5,   8.0),
            new Holding("RELIANCE",  "Reliance Industries",  base - 5,   8.0),
            new Holding("TCS",       "TCS Ltd.",             base - 10,  5.0),
            new Holding("BAJFINANCE","Bajaj Finance",        base - 10,  5.0)
        );
    }

    // ---------------------------------------------------------------
    // AGGRESSIVE PORTFOLIO: 6 holdings, concentrated in high-vol stocks
    // Total target allocation = 100%
    // NOTE: some holdings will intentionally breach the 20% threshold
    // ---------------------------------------------------------------
    private List<Holding> buildAggressiveHoldings(int clientId) {
        int base = (clientId % 5) * 15 + 80;
        return Arrays.asList(
            new Holding("NVDA",   "NVIDIA Corp.",    base + 50, 30.0),   // >20% — intentional breach
            new Holding("TSLA",   "Tesla Inc.",      base + 40, 25.0),   // >20% — intentional breach
            new Holding("META",   "Meta Platforms",  base + 30, 20.0),
            new Holding("AMZN",   "Amazon.com Inc.", base + 20, 10.0),
            new Holding("GOOGL",  "Alphabet Inc.",   base + 10,  8.0),
            new Holding("AAPL",   "Apple Inc.",      base,       7.0)
        );
    }
}
