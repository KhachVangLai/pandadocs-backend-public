# Local Setup Guide

This guide explains how to review and run the public PandaDocs Backend snapshot locally.

## 1. Prerequisites

Install the following first:

- Java 17
- Maven 3.9+ or use the included Maven wrapper
- PostgreSQL
- Optional external accounts if you want full feature coverage:
  Firebase, Google OAuth2, PayOS, and Gemini

## 2. Understand What This Public Repo Is

This repository is a **sanitized backend-only snapshot** of a past university project.

- The frontend is separate and not included here.
- Real credentials have been removed.
- Some infrastructure and database setup details were part of the original private environment and are not fully reproduced here.

If you are an interviewer or reviewer, it is completely valid to review the architecture and code without running every cloud integration end to end.

## 3. Prepare Environment Variables

Use [../.env.example](../.env.example) as the reference list.

Important groups:

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- JWT / Auth: `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- Firebase: `FIREBASE_CREDENTIALS_PATH`, `FIREBASE_STORAGE_BUCKET`
- PayOS: payment credentials and callback URLs
- Mail: SMTP credentials
- Gemini: `GEMINI_API_KEY`

You can provide these via shell environment variables or a local `.env` file if your environment supports it.

## 4. Provision PostgreSQL

Create a PostgreSQL database and point `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` to it.

Important limitation:

- The application uses `spring.jpa.hibernate.ddl-auto=validate`, so the schema must already exist.
- This public snapshot does **not** include a complete automated schema bootstrap for the whole marketplace.
- The only checked-in migration is the chat-specific one under [../database/migrations/V001__create_chat_tables.sql](../database/migrations/V001__create_chat_tables.sql).

That means a full local startup may require:

- manually recreating the missing marketplace tables, or
- using an existing schema from the original project environment

## 5. Optional External Services

For partial local review, you do not need every integration configured. For full feature testing:

- Firebase is required for file upload/download and signed preview URLs
- Google OAuth2 is required for social login
- PayOS is required for real payment flow testing
- Gemini is required for AI chat responses
- SMTP credentials are required for email verification and password reset

Without those services, the application may still be useful for code review and partial endpoint testing, but related features will fail or be incomplete.

## 6. Run the Application

From the repository root:

```bash
./mvnw spring-boot:run
```

Or:

```bash
./mvnw clean package
java -jar target/api-0.0.1-SNAPSHOT.jar
```

## 7. Open API Docs

If the server starts successfully, open:

```text
http://localhost:8080/swagger-ui.html
```

This is the easiest way for a reviewer to inspect the API surface.

## 8. Recommended Review Path

If you want to understand the project quickly, review in this order:

1. [../README.md](../README.md)
2. [../PROJECT_REPORT.md](../PROJECT_REPORT.md)
3. `src/main/java/com/pandadocs/api/controller`
4. `src/main/java/com/pandadocs/api/service`
5. [./ai-chat/frontend-integration.md](./ai-chat/frontend-integration.md)

## 9. Known Gaps

- Backend only, no checked-in frontend
- Limited automated tests
- Incomplete public DB bootstrap
- Public-safe placeholders instead of real deployment values

Those gaps are intentional to keep the public repo safe and honest.
