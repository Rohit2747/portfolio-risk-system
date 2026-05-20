package com.incedo.risk_analysis_service.events;

import org.springframework.stereotype.Component;

@Component
public class RiskEventPublisher {

    public void publishRiskAlert(RiskAlertEvent event) {

        System.out.println("====================================");
        System.out.println("EVENT PUBLISHED TO EVENT BUS");
        System.out.println("Portfolio: " + event.getPortfolioName());
        System.out.println("Risk Level: " + event.getRiskLevel());
        System.out.println("Alert: " + event.getAlertMessage());
        System.out.println("Timestamp: " + event.getTimestamp());
        System.out.println("====================================");

    }

}