# FinTech Risk Monitoring Platform — System Architecture

## Project Overview

The FinTech Risk Monitoring Platform is an AWS-inspired microservices-based application developed using Java Spring Boot and REST APIs.

The platform monitors portfolio risk, market volatility, AI-generated insights, and event-driven financial alerts using cloud-native architectural concepts.

---

# Core Architecture Components

## 1. Frontend Dashboard

Technology:
- HTML
- CSS
- JavaScript

Responsibilities:
- Display portfolio analytics
- Show AI insights
- Visualize market risk
- Display alerts and monitoring data

---

## 2. Portfolio Service

Technology:
- Spring Boot REST API

Responsibilities:
- Manage portfolio information
- Return client portfolio data
- Provide investment details

Endpoint:
```text
/portfolios