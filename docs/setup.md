# Local Setup Guide

This guide explains how to review and run the public PandaDocs Backend snapshot locally.

## 1. Prerequisites

Install the following first:

- Java 17
- Maven 3.9+ or use the included Maven wrapper
- PostgreSQL
- `rg` (optional, only needed for the pre-publish scan script)
- Optional external accounts if you want full feature coverage:
  Firebase, Google OAuth2, PayOS, and Gemini

## 2. Understand What This Public Repo Is

This repository is a **sanitized backend-only snapshot** of a past university project.

- The frontend is separate and not included here.
- Real credentials have been removed.
- Some infrastructure and database setup details were part of the original private environment and are not fully reproduced here.

If you are an interviewer or reviewer, it is completely valid to review the architecture and code without running every cloud integration end to end.

## 3. Choose a Safe Local Secret Strategy

Use one of these approaches:

1. Recommended for local review: copy `src/main/resources/application-local.example.properties` to `src/main/resources/application-local.properties`
2. Use shell environment variables based on [../.env.example](../.env.example)
3. In cloud environments, use a secret manager or deployment environment variables

Important note:

- Spring Boot does **not** automatically load `.env` files by itself.
- `.env.example` is a reference list, not a plug-and-play runtime file.
- The tracked repo should stay clean; your real local values should live only in untracked files or external secret storage.

PowerShell bootstrap command:

```powershell
.\scripts\bootstrap-local.ps1
```

That script will:

- create the ignored `secrets/` folder
- copy `application-local.example.properties` to `application-local.properties` if needed
- show you the exact local run command

Important value groups:

- Database: `spring.datasource.*` or `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- JWT / Auth: `pandadocs.app.jwtSecret` or `JWT_SECRET`, plus Google OAuth values
- Firebase: `firebase.credentials.path`, `firebase.credentials.base64`, or cloud Application Default Credentials
- PayOS: payment credentials and callback URLs
- Mail: SMTP credentials
- Gemini: `gemini.api.key` or `GEMINI_API_KEY`

## 4. Prepare Firebase Safely

For this public repo, do not commit Firebase credentials.

Recommended local approach:

- put the JSON file at `secrets/firebase-service-account.json`
- keep that folder ignored by git
- point `firebase.credentials.path` or `FIREBASE_CREDENTIALS_PATH` to it

Other supported approaches:

- set `FIREBASE_CREDENTIALS_BASE64` with a base64-encoded service-account JSON
- rely on Google Application Default Credentials in cloud runtime

## 5. Provision PostgreSQL

Create a PostgreSQL database and point your local config to it.

Important limitation:

- The application uses `spring.jpa.hibernate.ddl-auto=validate`, so the schema must already exist.
- This public snapshot does **not** include a complete automated schema bootstrap for the whole marketplace.
- The only checked-in migration is the chat-specific one under [../database/migrations/V001__create_chat_tables.sql](../database/migrations/V001__create_chat_tables.sql).

That means a full local startup may require:

- manually recreating the missing marketplace tables, or
- using an existing schema from the original project environment

## 6. Optional External Services

For partial local review, you do not need every integration configured. For full feature testing:

- Firebase is required for file upload/download and signed preview URLs
- Google OAuth2 is required for social login
- PayOS is required for real payment flow testing
- Gemini is required for AI chat responses
- SMTP credentials are required for email verification and password reset

Without those services, the application may still be useful for code review and partial endpoint testing, but related features will fail or be incomplete.

## 7. Run the Application

Recommended PowerShell command:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Equivalent cross-platform command:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Or:

```bash
./mvnw clean package -Dspring.profiles.active=local
java -jar target/api-0.0.1-SNAPSHOT.jar
```

If you prefer pure environment variables instead of `application-local.properties`, run the same commands after exporting the required values.

## 8. Open API Docs

If the server starts successfully, open:

```text
http://localhost:8080/swagger-ui.html
```

This is the easiest way for a reviewer to inspect the API surface.

## 9. Recommended Review Path

If you want to understand the project quickly, review in this order:

1. [../README.md](../README.md)
2. [../PROJECT_REPORT.md](../PROJECT_REPORT.md)
3. `src/main/java/com/pandadocs/api/controller`
4. `src/main/java/com/pandadocs/api/service`
5. [./ai-chat/frontend-integration.md](./ai-chat/frontend-integration.md)

## 10. Before You Push Publicly

Run:

```powershell
.\scripts\preflight-public-check.ps1
```

This does not replace real credential rotation, but it helps catch obvious publication mistakes such as tracked secret files or high-signal key patterns.

## 11. Known Gaps

- Backend only, no checked-in frontend
- Limited automated tests
- Incomplete public DB bootstrap
- Public-safe placeholders instead of real deployment values

Those gaps are intentional to keep the public repo safe and honest.
