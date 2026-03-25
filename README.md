# PandaDocs Backend

Sanitized public portfolio snapshot of a Spring Boot backend for a document-template marketplace.

## Project Snapshot

PandaDocs Backend is a Java/Spring Boot service for a template marketplace with:

- JWT and Google OAuth2 authentication
- role-based flows for `USER`, `SELLER`, and `ADMIN`
- PostgreSQL persistence with Spring Data JPA
- Firebase Storage for template files, avatars, and preview images
- PayOS integration for paid checkout
- Gemini-powered AI chat for template discovery

This repository is a **public-safe snapshot** of a past university project. It is shared to demonstrate backend design, domain modeling, security/authentication flows, third-party integrations, and documentation quality. The frontend is a separate application and is not included here.

## For Interviewers / Recruiters

- This is a **past academic project**, not an actively maintained production system.
- The repo was published as a **sanitized portfolio snapshot** after removing sensitive artifacts and private deployment context.
- It is intended to show backend engineering ability: Spring Boot architecture, REST API design, auth, payments, cloud storage, and AI-assisted product search.
- Known limitations are intentionally preserved and documented rather than hidden.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Web, Security, Validation, Data JPA
- PostgreSQL
- Firebase Admin SDK / Google Cloud Storage
- PayOS Java SDK
- OkHttp + Gson
- Swagger / OpenAPI
- Docker

## Key Features

- User authentication with username/password and Google OAuth2
- Marketplace flows for browsing, purchasing, and downloading templates
- Seller registration, template upload, and seller dashboard data
- Admin moderation and operational dashboard flows
- AI chat assistant with template-search-first recommendation logic
- File storage and preview handling via Firebase Storage

## Architecture Summary

The backend follows a standard layered Spring structure:

- `src/main/java/com/pandadocs/api/controller`: REST endpoints
- `src/main/java/com/pandadocs/api/service`: business logic and integrations
- `src/main/java/com/pandadocs/api/repository`: JPA repositories
- `src/main/java/com/pandadocs/api/model`: domain entities and enums
- `src/main/java/com/pandadocs/api/dto`: request/response models
- `src/main/java/com/pandadocs/api/security`: JWT, OAuth2, and access control

For a deeper technical review, see [PROJECT_REPORT.md](./PROJECT_REPORT.md).

## Quick Start

1. Install Java 17 and Maven.
2. Copy [.env.example](./.env.example) and prepare equivalent environment variables.
3. Provision PostgreSQL and any external services you want to exercise.
4. Review the full setup guide at [docs/setup.md](./docs/setup.md).
5. Start the application:

```bash
./mvnw spring-boot:run
```

6. Open Swagger UI after startup:

```text
http://localhost:8080/swagger-ui.html
```

## Documentation

- [docs/README.md](./docs/README.md): documentation index
- [docs/setup.md](./docs/setup.md): step-by-step local setup
- [PROJECT_REPORT.md](./PROJECT_REPORT.md): bilingual project report
- [SECURITY_ROTATION_CHECKLIST.md](./SECURITY_ROTATION_CHECKLIST.md): publication safety checklist
- [docs/ai-chat/frontend-integration.md](./docs/ai-chat/frontend-integration.md): AI chat frontend contract

## Limitations

- This is a backend-only repo; the frontend is separate.
- Database bootstrap is not fully automated in this public snapshot.
- Some legacy docs are preserved for context and may reflect historical implementation state.
- Test coverage is limited and the repo is presented primarily for architecture/code review, not as a finished production deployment.

## Repository Status

This repo should be treated as a **portfolio snapshot**:

- stable enough to review
- not guaranteed to receive ongoing feature development
- intentionally public-safe rather than infrastructure-complete

## Security Note

The original private project contained deployment-specific values and sensitive artifacts. This public version removes those files and replaces deployable examples with placeholders. Before publishing your own fork, verify that:

- no real credentials are tracked
- no backup config files remain
- no secret previews are logged
- deployment manifests use placeholders or environment-driven values only

See [SECURITY_ROTATION_CHECKLIST.md](./SECURITY_ROTATION_CHECKLIST.md) for the credential rotation checklist that should be completed outside this repository.
