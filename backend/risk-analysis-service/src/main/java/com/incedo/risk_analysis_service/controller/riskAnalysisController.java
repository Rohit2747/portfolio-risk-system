package com.incedo.risk_analysis_service.controller;

import com.incedo.risk_analysis_service.model.riskAnalysis;
import com.incedo.risk_analysis_service.events.RiskAlertEvent;
import com.incedo.risk_analysis_service.events.RiskEventPublisher;
import com.incedo.risk_analysis_service.monitoring.CloudWatchLogger;
import com.incedo.risk_analysis_service.ai.AIRecommendationService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class riskAnalysisController {

    private final RiskEventPublisher riskEventPublisher;
    private final CloudWatchLogger cloudWatchLogger;
    private final AIRecommendationService aiRecommendationService;

    public riskAnalysisController(
            RiskEventPublisher riskEventPublisher,
            CloudWatchLogger cloudWatchLogger,
            AIRecommendationService aiRecommendationService) {

        this.riskEventPublisher = riskEventPublisher;
        this.cloudWatchLogger = cloudWatchLogger;
        this.aiRecommendationService = aiRecommendationService;
    }

    @GetMapping("/risk-analysis")
    public List<riskAnalysis> getRiskAnalysis() {

        cloudWatchLogger.logServiceAccess(
                "Risk Analysis Service",
                "/risk-analysis"
        );

        List<riskAnalysis> riskList = List.of(

                new riskAnalysis(
                        "Tech Portfolio",
                        250000,
                        "MEDIUM",
                        aiRecommendationService.generateRecommendation("MEDIUM")
                ),

                new riskAnalysis(
                        "Banking Portfolio",
                        500000,
                        "HIGH",
                        aiRecommendationService.generateRecommendation("HIGH")
                ),

                new riskAnalysis(
                        "Startup Portfolio",
                        120000,
                        "LOW",
                        aiRecommendationService.generateRecommendation("LOW")
                )
        );

        for (riskAnalysis risk : riskList) {

            if (risk.getRiskLevel().equals("HIGH")
                    || risk.getRiskLevel().equals("MEDIUM")) {

                RiskAlertEvent event = new RiskAlertEvent(
                        risk.getPortfolioName(),
                        risk.getRiskLevel(),
                        risk.getSuggestion(),
                        LocalDateTime.now().toString()
                );

                riskEventPublisher.publishRiskAlert(event);
            }
        }

        return riskList;
    }
}