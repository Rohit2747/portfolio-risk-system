package com.incedo.ai_insight_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Insight Service — Port 8083
 *
 * The 4th microservice. Consumes RiskThresholdBreached events and generates
 * structured AI-powered portfolio explanations.
 *
 * Event consumption:
 *   LOCAL: SQS poller disabled; REST endpoints called directly by Risk Service/frontend
 *   AWS:   SqsRiskEventConsumer polls ai-insight-queue every 5 seconds
 *
 * AI generation:
 *   MOCK:    Rule-based contextual explanations (default)
 *   OPENAI:  GPT-3.5-turbo via REST API (set ai.provider=OPENAI + OPENAI_API_KEY)
 *   BEDROCK: Amazon Bedrock Claude (set ai.provider=BEDROCK + AWS credentials)
 *
 * Endpoints:
 *   POST /ai-insight                       → generate from JSON body
 *   GET  /ai-insight/portfolio/{id}        → fetch risk data + generate
 *   GET  /ai-insight/all-breached          → all breached portfolios
 *   GET  /ai-insight/cached                → show SQS-processed cache
 *   GET  /ai-insight/prompt/{id}           → view Bedrock/OpenAI prompt
 *   GET  /ai-insight/events/log            → SQS processing audit log
 */
@SpringBootApplication
@EnableScheduling
public class AIInsightServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIInsightServiceApplication.class, args);
    }
}
