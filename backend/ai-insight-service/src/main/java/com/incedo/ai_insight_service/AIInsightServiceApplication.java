package com.incedo.ai_insight_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI Insight Service — Port 8083
 *
 * The 4th microservice. Consumes risk alert events and generates
 * structured AI-powered portfolio explanations.
 *
 * Current mode: Rule-based contextual AI (MOCK provider)
 * AWS upgrade:  Subscribe to SQS queue, call Amazon Bedrock for LLM insights
 *
 * Endpoints:
 *   POST /ai-insight                    → generate insight from JSON body
 *   GET  /ai-insight/portfolio/{id}     → fetch risk data + generate insight
 *   GET  /ai-insight/all-breached       → insights for all breached portfolios
 *   GET  /ai-insight/prompt/{id}        → view the Bedrock/OpenAI prompt
 */
@SpringBootApplication
public class AIInsightServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIInsightServiceApplication.class, args);
    }
}
