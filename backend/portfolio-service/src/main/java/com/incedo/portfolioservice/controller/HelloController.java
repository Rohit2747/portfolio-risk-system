package com.incedo.portfolioservice.controller;

import com.incedo.portfolioservice.model.portfolio;
import com.incedo.portfolioservice.service.PortfolioDataStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * PortfolioController
 *
 * Exposes REST APIs for client portfolio data.
 * This service is the source of truth for:
 *   - client identities
 *   - stock holdings (symbol, quantity, target allocation)
 *
 * NOTE: portfolioValue and riskLevel are populated by the
 * Risk Analysis Service, which calls this service and enriches the data.
 */
@RestController
@CrossOrigin(origins = "*")
public class HelloController {

    private final PortfolioDataStore portfolioDataStore;

    public HelloController(PortfolioDataStore portfolioDataStore) {
        this.portfolioDataStore = portfolioDataStore;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/hello")
    public String hello() {
        return "Portfolio Service is running on port 8080";
    }

    /**
     * Returns all 100 client portfolios with their holdings.
     * Portfolio value (0.0) will be populated by Risk Service.
     */
    @GetMapping("/portfolios")
    public List<portfolio> getPortfolios() {
        return portfolioDataStore.getAllPortfolios();
    }

    /**
     * Returns a single portfolio by client ID.
     */
    @GetMapping("/portfolios/{clientId}")
    public portfolio getPortfolioById(@PathVariable int clientId) {
        portfolio p = portfolioDataStore.getPortfolioById(clientId);
        if (p == null) {
            throw new RuntimeException("Portfolio not found for clientId: " + clientId);
        }
        return p;
    }

    /**
     * Returns just the list of all stock symbols used across portfolios.
     * Used by Market Data Service to know which equities to track.
     */
    @GetMapping("/equities")
    public List<String> getEquitySymbols() {
        return java.util.Arrays.stream(PortfolioDataStore.EQUITIES)
            .map(e -> e[0])
            .toList();
    }
}
