# Transaction Insights Agent

A full-stack personal finance analytics app that lets users upload CSV transaction exports, review spending trends, and ask an AI agent questions about the loaded data.

## What it does

- Upload and normalize CSV bank/credit card exports
- Store transactions in PostgreSQL
- Aggregate spend by category over a date range
- Show top spending categories and transaction totals
- Answer finance questions with a grounded Anthropic-powered agent
- Serve a browser dashboard for upload, analytics, and AI analysis

## Tech stack

- Java 17 + Spring Boot 3
- Maven
- PostgreSQL
- Docker Compose
- Anthropic API integration
- Static HTML/CSS/JS dashboard served by the Spring app

## Local setup

1. Start the local stack:

```bash
cd /Users/annabellortiz/Desktop/Projects/Transaction-Insights-Project
docker compose -f infra/docker-compose.yml up --build -d
```

2. Build and run the backend:

```bash
cd backend
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

3. Open the app:

```text
http://localhost:8080
```

## Sample data

A sample April 2026 dataset is included at the repo root:

- `sample_transactions.csv`

You can upload it through the dashboard or with:

```bash
curl -sS -X POST http://localhost:8080/api/upload -F "file=@sample_transactions.csv"
```

## Environment variables

For the Anthropic integration, set:

- `ANTHROPIC_API_KEY`
- `ANTHROPIC_WORKSPACE_ID` or `ANTHROPIC_ORGANIZATION_ID`

The app also reads database defaults from the Spring configuration.

## Project summary

This project demonstrates a practical AI-enabled finance workflow: ingest real transaction exports, normalize inconsistent formats, compute summary metrics, and let an LLM explain the data using the loaded records instead of relying on generic fallback responses.

## Repository layout

- `backend/` — Spring Boot application
- `infra/` — Docker and database schema files
- `sample_transactions.csv` — sample transaction export for demo/testing
