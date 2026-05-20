package com.incedo.risk_analysis_service.ai;

import org.springframework.stereotype.Service;

@Service
public class AIRecommendationService {

    public String generateRecommendation(String riskLevel) {

        if (riskLevel.equalsIgnoreCase("HIGH")) {
            return "AI Insight: High portfolio risk detected. Recommended action is reducing sector concentration and increasing diversification.";
        }

        if (riskLevel.equalsIgnoreCase("MEDIUM")) {
            return "AI Insight: Portfolio is moderately balanced. Suggested action is partial reallocation into stable assets.";
        }

        return "AI Insight: Portfolio risk is low. Current investment allocation appears healthy and stable.";
    }
}