package com.incedo.risk_analysis_service.controller;

import com.incedo.risk_analysis_service.events.RiskAlertEvent;
import com.incedo.risk_analysis_service.events.RiskEventPublisher;
import com.incedo.risk_analysis_service.model.riskAnalysis;
import com.incedo.risk_analysis_service.monitoring.CloudWatchLogger;
import com.incedo.risk_analysis_service.service.RiskCalculationEngine;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RiskAnalysisController
 *
 * Exposes REST APIs for risk analysis results.
 * On each call:
 *   1. Fetches live data from Portfolio + Market Data services
 *   2. Runs risk calculation engine
 *   3. Publishes RiskAlertEvent for any HIGH/MEDIUM breach (→ SNS in AWS)
 *   4. Logs to CloudWatch
 *   5. Returns structured JSON response
 */
@RestController
@CrossOrigin(origins = "*")
public class riskAnalysisController {

    private final RiskCalculationEngine riskCalculationEngine;
    private final RiskEventPublisher riskEventPublisher;
    private final CloudWatchLogger cloudWatchLogger;

    public riskAnalysisController(
            RiskCalculationEngine riskCalculationEngine,
            RiskEventPublisher riskEventPublisher,
            CloudWatchLogger cloudWatchLogger) {
        this.riskCalculationEngine = riskCalculationEngine;
        this.riskEventPublisher = riskEventPublisher;
        this.cloudWatchLogger = cloudWatchLogger;
    }

    /**
     * Health check
     */
    @GetMapping("/hello")
    public String hello() {
        return "Risk Analysis Service is running on port 8082";
    }

    /**
     * Returns full risk analysis for all 100 client portfolios.
     *
     * This is the primary endpoint for the dashboard.
     * Each call fetches fresh data from Portfolio and Market Data services.
     */
    @GetMapping("/risk-analysis")
    public List<riskAnalysis> getAllRiskAnalysis() {

        cloudWatchLogger.logServiceAccess("RiskAnalysisService", "/risk-analysis");

        List<riskAnalysis> results = riskCalculationEngine.analyzeAllPortfolios();

        // Publish events for portfolios with breaches
        for (riskAnalysis analysis : results) {
            if (analysis.isHasBreaches()) {
                RiskAlertEvent event = new RiskAlertEvent(
                    analysis.getClientId(),
                    analysis.getClientName(),
                    analysis.getRiskLevel(),
                    analysis.getBreaches(),
                    analysis.getTotalPortfolioValue(),
                    analysis.getDailyChangePercent(),
                    analysis.getAlertTimestamp()
                );
                riskEventPublisher.publishRiskAlert(event);
            }
        }

        cloudWatchLogger.logMetric("RiskAnalysisService", "TotalPortfoliosAnalyzed", results.size());
        cloudWatchLogger.logMetric("RiskAnalysisService", "PortfoliosWithBreaches",
            (int) results.stream().filter(riskAnalysis::isHasBreaches).count());

        return results;
    }

    /**
     * Returns risk analysis for a single client.
     */
    @GetMapping("/risk-analysis/{clientId}")
    public riskAnalysis getRiskForClient(@PathVariable int clientId) {

        cloudWatchLogger.logServiceAccess("RiskAnalysisService", "/risk-analysis/" + clientId);

        return riskCalculationEngine.analyzeAllPortfolios().stream()
            .filter(r -> r.getClientId() == clientId)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No analysis found for clientId: " + clientId));
    }

    /**
     * Returns only portfolios with active risk breaches.
     * Used by the AI Insight Service to know who needs explanations.
     */
    @GetMapping("/risk-analysis/breached")
    public List<riskAnalysis> getBreachedPortfolios() {
        return riskCalculationEngine.analyzeAllPortfolios().stream()
            .filter(riskAnalysis::isHasBreaches)
            .toList();
    }
}
