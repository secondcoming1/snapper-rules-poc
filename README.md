# SNAPPER Rules POC

Proof of concept for externalizing deterministic SNAPPER decision logic using:

- Java 21
- Spring Boot
- Apache KIE / Drools DMN
- Versioned decision models
- REST APIs

## Current functionality

- Spring Boot health endpoint
- Drools DMN runtime
- Versioned income-source classification decision
- REST endpoint for evaluating the DMN rule

## Prerequisites

- Java 21
- Maven 3.9+

## Run locally

```bash
cd rules-service
./mvnw spring-boot:run

Current rule classifications
EMPLOYER_WAGES → EARNED
SELF_EMPLOYMENT → EARNED
SOCIAL_SECURITY → UNEARNED
PENSION → UNEARNED
Other values → UNCLASSIFIED
Planned next steps
Multi-record income evidence classification
Benefit-month applicability rules
Conflicting evidence classification
Version comparison
FastAPI normalized evidence service
LLM-generated user explanations
PostgreSQL audit storage