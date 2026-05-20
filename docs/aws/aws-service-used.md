# AWS Services Used — FinTech Risk Monitoring Platform

## 1. Amazon EC2
Purpose:
Hosts Spring Boot microservices and backend APIs.

Usage in Project:
- Portfolio Service deployment
- Market Data Service deployment
- Risk Analysis Service deployment

Why Used:
Provides scalable cloud compute infrastructure for backend services.

---

## 2. Amazon S3
Purpose:
Stores generated AI reports and dashboard exports.

Usage in Project:
- AI-generated portfolio reports
- Risk analysis exports
- Dashboard snapshots

Why Used:
Provides secure and scalable object storage.

---

## 3. Amazon RDS
Purpose:
Stores portfolio and market-related structured data.

Usage in Project:
- Client portfolio data
- Stock market records
- Risk score history

Why Used:
Provides managed relational database support.

---

## 4. Amazon EventBridge / SNS
Purpose:
Handles event-driven communication between services.

Usage in Project:
- MarketDataUpdatedEvent
- RiskAlertEvent
- PortfolioUpdateEvent

Why Used:
Enables publish-subscribe architecture and microservice communication.

---

## 5. Amazon CloudWatch
Purpose:
Monitoring and logging.

Usage in Project:
- API monitoring
- Error tracking
- Service health monitoring

Why Used:
Provides operational visibility and logging.

---

## 6. Amazon Bedrock
Purpose:
AI-powered recommendation generation.

Usage in Project:
- AI investment recommendations
- Portfolio risk summaries
- Health score insights

Why Used:
Provides Generative AI capability using foundation models.

---

# Final AWS Architecture

Frontend Dashboard
↓
API Gateway
↓
Spring Boot Microservices on EC2
↓
Amazon RDS
↓
EventBridge/SNS Events
↓
CloudWatch Monitoring
↓
Bedrock AI Recommendations