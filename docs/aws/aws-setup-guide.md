# AWS Setup Guide — Portfolio Risk Monitoring Platform

This guide explains exactly how to enable each AWS service integration.
Follow in order. All services work locally first without AWS.

---

## Step 1: Create AWS Resources (One-time setup)

### 1a. Create SNS Topics

```bash
# Topic 1: Price updates from Market Data Service
aws sns create-topic --name price-updated --region us-east-1

# Topic 2: Risk alerts from Risk Analysis Service  
aws sns create-topic --name risk-threshold-breached --region us-east-1
```

Save the **Topic ARNs** from the output — you'll need them below.

---

### 1b. Create SQS Queue for AI Insight Service

```bash
# Create the queue that AI Insight Service will poll
aws sqs create-queue \
  --queue-name ai-insight-queue \
  --attributes '{"VisibilityTimeout":"30","MessageRetentionPeriod":"86400"}' \
  --region us-east-1
```

Save the **Queue URL** from the output.

---

### 1c. Subscribe SQS Queue to SNS Topic

```bash
# Get the SQS queue ARN
aws sqs get-queue-attributes \
  --queue-url https://sqs.us-east-1.amazonaws.com/ACCOUNT_ID/ai-insight-queue \
  --attribute-names QueueArn

# Subscribe queue to risk-threshold-breached topic
aws sns subscribe \
  --topic-arn arn:aws:sns:us-east-1:ACCOUNT_ID:risk-threshold-breached \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:us-east-1:ACCOUNT_ID:ai-insight-queue
```

---

### 1d. Add SQS Policy to allow SNS to send messages

```bash
aws sqs set-queue-attributes \
  --queue-url https://sqs.us-east-1.amazonaws.com/ACCOUNT_ID/ai-insight-queue \
  --attributes '{
    "Policy": "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"sns.amazonaws.com\"},\"Action\":\"sqs:SendMessage\",\"Resource\":\"arn:aws:sqs:us-east-1:ACCOUNT_ID:ai-insight-queue\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"arn:aws:sns:us-east-1:ACCOUNT_ID:risk-threshold-breached\"}}}]}"
  }'
```

---

### 1e. Create CloudWatch Log Group

```bash
aws logs create-log-group \
  --log-group-name /portfolio-risk/risk-analysis-service \
  --region us-east-1
```

---

### 1f. Enable Amazon Bedrock Model Access (for AI)

1. Open AWS Console → Bedrock → Model access
2. Click "Manage model access"
3. Enable: **Anthropic → Claude 3 Sonnet**
4. Submit and wait for approval (usually instant)

---

## Step 2: Configure IAM Permissions

Create an IAM policy with these permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SNSPublish",
      "Effect": "Allow",
      "Action": ["sns:Publish"],
      "Resource": [
        "arn:aws:sns:us-east-1:ACCOUNT_ID:price-updated",
        "arn:aws:sns:us-east-1:ACCOUNT_ID:risk-threshold-breached"
      ]
    },
    {
      "Sid": "SQSConsume",
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:us-east-1:ACCOUNT_ID:ai-insight-queue"
    },
    {
      "Sid": "CloudWatchLogs",
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogStreams"
      ],
      "Resource": "arn:aws:logs:us-east-1:ACCOUNT_ID:log-group:/portfolio-risk/*"
    },
    {
      "Sid": "BedrockInvoke",
      "Effect": "Allow",
      "Action": ["bedrock:InvokeModel"],
      "Resource": "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-sonnet-20240229-v1:0"
    }
  ]
}
```

Attach this policy to:
- Your IAM user (for local dev with `~/.aws/credentials`)
- EC2 instance role (for production deployment)

---

## Step 3: Set Environment Variables

Set these before starting each service:

```bash
# Master AWS switch
export AWS_ENABLED=true
export AWS_REGION=us-east-1

# For local dev only (not needed on EC2 with IAM role)
export AWS_ACCESS_KEY_ID=your-access-key
export AWS_SECRET_ACCESS_KEY=your-secret-key

# SNS Topic ARNs (replace ACCOUNT_ID)
export SNS_PRICE_TOPIC_ARN=arn:aws:sns:us-east-1:ACCOUNT_ID:price-updated
export SNS_RISK_TOPIC_ARN=arn:aws:sns:us-east-1:ACCOUNT_ID:risk-threshold-breached

# SQS Queue URL (replace ACCOUNT_ID)
export SQS_RISK_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/ACCOUNT_ID/ai-insight-queue

# AI Provider
export AI_PROVIDER=BEDROCK
# OR for OpenAI:
# export AI_PROVIDER=OPENAI
# export OPENAI_API_KEY=sk-your-key-here
```

---

## Step 4: Start Services with AWS Enabled

```bash
# Terminal 1 — Portfolio Service
cd backend/portfolio-service
mvn spring-boot:run

# Terminal 2 — Market Data Service (publishes PriceUpdated to SNS every 30s)
cd backend/market-data-service
AWS_ENABLED=true mvn spring-boot:run

# Terminal 3 — Risk Analysis Service (publishes RiskThresholdBreached to SNS + logs to CloudWatch)
cd backend/risk-analysis-service
AWS_ENABLED=true mvn spring-boot:run

# Terminal 4 — AI Insight Service (polls SQS + calls Bedrock/OpenAI)
cd backend/ai-insight-service
AWS_ENABLED=true AI_PROVIDER=BEDROCK mvn spring-boot:run
```

---

## Step 5: Verify AWS Integration

### Check SNS published events:
```bash
# List recent SNS publishes (via CloudWatch metrics)
aws cloudwatch get-metric-statistics \
  --namespace AWS/SNS \
  --metric-name NumberOfMessagesSent \
  --dimensions Name=TopicName,Value=risk-threshold-breached \
  --start-time 2026-01-01T00:00:00Z \
  --end-time 2026-12-31T00:00:00Z \
  --period 3600 \
  --statistics Sum
```

### Check SQS queue depth:
```bash
aws sqs get-queue-attributes \
  --queue-url https://sqs.us-east-1.amazonaws.com/ACCOUNT_ID/ai-insight-queue \
  --attribute-names ApproximateNumberOfMessages
```

### Check CloudWatch logs:
```bash
# View recent log entries
aws logs filter-log-events \
  --log-group-name /portfolio-risk/risk-analysis-service \
  --limit 20
```

### Test AI Insight Service:
```bash
# Check SQS processing cache
curl http://localhost:8083/ai-insight/cached

# Check event processing log
curl http://localhost:8083/ai-insight/events/log

# View a Bedrock prompt
curl http://localhost:8083/ai-insight/prompt/78
```

---

## Architecture Overview

```
Market Data Service (8081)
    ↓ [every 30s] SNS: price-updated
    
Portfolio Service (8080)
    ↓ [REST GET /portfolios]

Risk Analysis Service (8082)
    ↓ calls Portfolio + Market Data via REST
    ↓ detects breaches
    ↓ SNS: risk-threshold-breached
    ↓ CloudWatch Logs: /portfolio-risk/risk-analysis-service

AI Insight Service (8083)
    ↓ SQS: polls ai-insight-queue (subscribed to risk-threshold-breached)
    ↓ Bedrock: invokes Claude 3 Sonnet
    ↓ returns structured AI commentary

Frontend Dashboard
    ↓ polls all 4 services via REST
    ↓ displays real-time risk alerts + AI insights
```

---

## AWS Services Used (4 minimum from problem statement)

| Service | Usage | Required? |
|---------|-------|-----------|
| Amazon SNS | Event publishing (price-updated, risk-threshold-breached) | ✅ Yes |
| Amazon SQS | Event consumption (ai-insight-queue) | ✅ Yes |
| Amazon CloudWatch Logs | Structured operational logging | ✅ Yes |
| Amazon Bedrock (Claude) | LLM-powered AI insight generation | ✅ Yes |
| Amazon EC2 | Hosting Spring Boot services | Optional (bonus) |
| Amazon API Gateway | Frontend API routing | Optional (bonus) |
| Amazon DynamoDB | Persisting risk events and insights | Optional (bonus) |
