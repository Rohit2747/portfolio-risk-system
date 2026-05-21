package com.incedo.risk_analysis_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * AwsConfig
 *
 * Creates all AWS SDK v2 client beans for the Risk Analysis Service.
 *
 * Credentials resolution order (DefaultCredentialsProvider):
 *   1. Environment variables:  AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
 *   2. Java system properties: aws.accessKeyId, aws.secretAccessKey
 *   3. ~/.aws/credentials file (local dev)
 *   4. IAM role attached to EC2/ECS instance (production — recommended)
 *
 * The `aws.enabled` flag allows running fully locally without any
 * AWS credentials. When false, all AWS calls are skipped gracefully.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * SNS Client — used by RiskEventPublisher to publish
     * RiskThresholdBreached events to the SNS topic.
     */
    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    /**
     * CloudWatch Logs Client — used by CloudWatchLogger to push
     * structured log entries to the /portfolio-risk/ log group.
     */
    @Bean
    public CloudWatchLogsClient cloudWatchLogsClient() {
        return CloudWatchLogsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    /**
     * CloudWatch Metrics Client — used to publish custom metrics
     * (e.g., TotalBreachCount, HighRiskPortfolioCount).
     */
    @Bean
    public CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
}
