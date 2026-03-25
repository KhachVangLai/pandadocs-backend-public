# PandaDocs Backend

Public-safe portfolio edition of a Spring Boot backend for a document-template marketplace.

## Overview

PandaDocs Backend is a Java/Spring Boot service for a template marketplace with:

- JWT and Google OAuth2 authentication
- role-based flows for `USER`, `SELLER`, and `ADMIN`
- PostgreSQL persistence with Spring Data JPA
- Firebase Storage for template files, avatars, and preview images
- PayOS integration for paid checkout
- Gemini-powered AI chat for template discovery

This repository is a **sanitized public snapshot**. It intentionally excludes private deployment history, credential-bearing artifacts, and internal environment values. The frontend is a separate application and is not included here.

## Stack

- Java 17
- Spring Boot 3
- Spring Web, Security, Validation, Data JPA
- PostgreSQL
- Firebase Admin SDK / Google Cloud Storage
- PayOS Java SDK
- OkHttp + Gson
- Swagger / OpenAPI
- Docker

## Repository Notes

- `src/main/java/com/pandadocs/api`: application source
- `src/main/resources/application.properties`: env-driven runtime config
- `database/`: migration notes and SQL
- `PROJECT_REPORT.md`: detailed bilingual project report
- `CHATBOX_*` docs: AI chat design and integration notes

This public repo uses clean history and placeholder configuration values. Real credentials must come from environment variables or secret managers.

## Local Setup

1. Install Java 17 and Maven.
2. Copy `.env.example` to `.env` or export equivalent environment variables.
3. Provision PostgreSQL and required cloud services.
4. Run the application:

```bash
./mvnw spring-boot:run
```

Or build a jar:

```bash
./mvnw clean package
java -jar target/api-0.0.1-SNAPSHOT.jar
```

## Environment Variables

Use [.env.example](./.env.example) as the reference template. Important groups:

- Database: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- Security: `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- Storage: `FIREBASE_CREDENTIALS_PATH`, `FIREBASE_STORAGE_BUCKET`
- Payments: `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`, URLs
- Email: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- AI Chat: `GEMINI_API_KEY`, chat quota values

## Security Notice

The original private project contained deployment-specific values and sensitive artifacts. This public version removes those files and replaces deployable examples with placeholders. Before publishing your own fork, verify that:

- no real credentials are tracked
- no backup config files remain
- no secret previews are logged
- deployment manifests use placeholders or environment-driven values only

See [SECURITY_ROTATION_CHECKLIST.md](./SECURITY_ROTATION_CHECKLIST.md) for the credential rotation checklist that should be completed outside this repository.
