# Transaction Insights Agent (MVP)

This repository contains the Transaction Insights Agent MVP: a Java Spring Boot backend (Maven), PostgreSQL infra, and a planned Expo frontend.

Quickstart

1. Start infrastructure (Postgres + backend container placeholder):

```bash
# from repo root
docker compose -f infra/docker-compose.yml up --build -d
```

2. Build backend locally:

```bash
cd backend
mvn clean package
# or ./mvnw clean package if you have the wrapper
```

3. Run the backend locally:

```bash
java -jar backend/target/backend-0.0.1-SNAPSHOT.jar
```

4. Test upload endpoint (stub):

```bash
curl -v -F "file=@/path/to/sample.csv" http://localhost:8080/api/upload
```

Environment variables

- `SPRING_DATASOURCE_URL` (defaults in `application.yml`) 
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `ANTHROPIC_API_KEY` (for LLM integration)

Next tasks

- Implement robust CSV parsing, validation, and staged inserts (service exists but can be improved).
- Implement typed tool DTOs and repository queries for `sumByCategory`, `largestTransaction`, and `trendByCategory`.
- Wire Anthropic function-calling for tool invocation and add call logging/tracing.
- Build the Expo frontend with CSV upload and chat UI.

See `backend/` for the Spring Boot Maven app and `infra/` for database init scripts.
