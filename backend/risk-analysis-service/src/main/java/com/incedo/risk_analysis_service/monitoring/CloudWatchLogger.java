package com.incedo.risk_analysis_service.monitoring;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CloudWatchLogger {

    public void logServiceAccess(String serviceName, String endpoint) {

        System.out.println("====================================");
        System.out.println("[AWS CLOUDWATCH MONITORING]");
        System.out.println("Service: " + serviceName);
        System.out.println("Endpoint Accessed: " + endpoint);
        System.out.println("Timestamp: " + LocalDateTime.now());
        System.out.println("Status: SUCCESS");
        System.out.println("====================================");

    }

}