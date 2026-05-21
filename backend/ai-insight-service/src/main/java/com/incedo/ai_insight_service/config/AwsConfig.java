package com.incedo.ai_insight_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AwsConfig for AI Insight Service.
 *
 * Creates:
 *   - SqsClient:            polls ai-insight-queue for RiskThresholdBreached events
 *   - BedrockRuntimeClient: calls Claude/Titan models for AI commentary
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * SQS Client — polls the ai-insight-queue subscribed to the
     * risk-threshold-breached SNS topic.
     */
    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    /**
     * Bedrock Runtime Client — invokes Claude 3 Sonnet (or Titan)
     * to generate plain-language portfolio risk explanations.
     *
     * Requires:
     *   - IAM permission: bedrock:InvokeModel
     *   - Model access enabled in AWS Bedrock console for your region
     */
    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        return BedrockRuntimeClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
}
