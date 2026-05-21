package com.incedo.risk_analysis_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Risk Analysis Service — Port 8082
 *
 * Detects risk threshold breaches by:
 *   1. Fetching portfolios from Portfolio Service (8080)
 *   2. Fetching live prices from Market Data Service (8081)
 *   3. Computing portfolio valuation and risk metrics
 *   4. Publishing RiskAlertEvent for breached portfolios
 *
 * AWS equivalent:
 *   - Subscribes to SNS/SQS for PriceUpdated events
 *   - Publishes RiskThresholdBreached events to SNS
 *   - Stores results in DynamoDB
 */
@SpringBootApplication
public class RiskAnalysisServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskAnalysisServiceApplication.class, args);
    }

    /**
     * RestTemplate for calling Portfolio and Market Data services.
     * AWS equivalent: replaced by event-driven consumption from SQS.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
