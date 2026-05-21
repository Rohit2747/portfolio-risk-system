package com.incedo.market_data_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Market Data Service — Port 8081
 *
 * Simulates live equity price streaming for 20 stocks.
 * Prices update every 5 seconds via @Scheduled task.
 *
 * AWS equivalent: This service would publish PriceUpdatedEvent
 * to an Amazon SNS topic every 5 seconds.
 */
@SpringBootApplication
@EnableScheduling
public class MarketDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketDataServiceApplication.class, args);
    }
}
