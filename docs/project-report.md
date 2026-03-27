# PandaDocs Backend Project Report

*Báo cáo dự án PandaDocs Backend*

## Executive Summary

PandaDocs is a Spring Boot backend for a document-template marketplace. The system models public browsing, paid and free template acquisition, owned-file access, seller workflows, admin moderation, email-based account flows, Google OAuth2 login, PayOS payments, Firebase-backed asset storage, and a Gemini-powered AI assistant for template discovery.

Về mặt học thuật và portfolio, đây là một mini-project có phạm vi khá rộng: không chỉ CRUD cơ bản mà còn có phân quyền nhiều vai trò, tích hợp dịch vụ ngoài, và một luồng sản phẩm tương đối gần với marketplace thật.

This public repository is a **sanitized portfolio snapshot**. It is intended to demonstrate backend engineering ability, not to pretend that the original cloud environment is still live. The historical Google Cloud project, PayOS environment, and some private infrastructure used during development have already expired.

## Repository Scope

This repository is the backend only.

- Main app entrypoint: [ApiApplication.java](../src/main/java/com/pandadocs/api/ApiApplication.java)
- Runtime configuration: [application.properties](../src/main/resources/application.properties)
- Public project overview: [README.md](../README.md)
- Local setup guide: [setup.md](./setup.md)
- Database note: [database/README.md](../database/README.md)

Frontend code is not included here. The backend still references a separate frontend host for OAuth2 return handling, payment return pages, and some user-facing routes.

Nói ngắn gọn: repo hiện tại là backend artifact để review kiến trúc, security flow, persistence, và third-party integration. Nó không phải là full product clone hoàn chỉnh có UI đi kèm trong cùng repo.

## Architecture Overview

The codebase uses a conventional Spring layered structure under [src/main/java/com/pandadocs/api](../src/main/java/com/pandadocs/api):

- `controller`: REST endpoints and access control boundaries
- `service`: business logic and external integrations
- `repository`: Spring Data JPA persistence
- `model`: JPA entities and enums
- `dto`: API payloads
- `security`: JWT filter, OAuth2 integration, and security config
- `config`: infrastructure and third-party configuration
- `validation`: request and password validation rules

This structure is suitable for a fresher backend portfolio because it is easy to explain in an interview and shows separation of concerns clearly.

Về cấu trúc package, dự án hiện đã ở mức ổn để đưa vào CV: không có cảm giác “single-controller demo”, mà thể hiện được tổ chức code theo module backend thông dụng.

## Core Domain and Flows

### Authentication and Account Flows

Key entrypoint: [AuthController.java](../src/main/java/com/pandadocs/api/controller/AuthController.java)

Implemented flows include:

- signup and signin with JWT
- email verification
- forgot-password and reset-password
- Google OAuth2 login

Security is defined centrally in [WebSecurityConfig.java](../src/main/java/com/pandadocs/api/security/WebSecurityConfig.java), with JWT request filtering in [AuthTokenFilter.java](../src/main/java/com/pandadocs/api/security/jwt/AuthTokenFilter.java).

### Marketplace and Asset Flows

Key entrypoint: [TemplateController.java](../src/main/java/com/pandadocs/api/controller/TemplateController.java)

Supported behavior includes:

- public template browsing and detail retrieval
- seller/admin template creation
- protected template downloads
- preview image handling
- review submission and retrieval
- popular-template responses

Files and preview images are stored through Firebase-related services, primarily [FirebaseStorageService.java](../src/main/java/com/pandadocs/api/service/FirebaseStorageService.java).

### Purchase and Payment Flows

Key entrypoints:

- [PurchaseController.java](../src/main/java/com/pandadocs/api/controller/PurchaseController.java)
- [PaymentController.java](../src/main/java/com/pandadocs/api/controller/PaymentController.java)
- [PayOSService.java](../src/main/java/com/pandadocs/api/service/PayOSService.java)

The backend supports free-template acquisition and paid checkout initiation. In the public snapshot, the original PayOS environment is no longer active, but the code still documents how the integration was structured.

### AI Chat and Template Discovery

Key entrypoints:

- [ChatController.java](../src/main/java/com/pandadocs/api/controller/ChatController.java)
- [ChatService.java](../src/main/java/com/pandadocs/api/service/ChatService.java)
- [GeminiService.java](../src/main/java/com/pandadocs/api/service/GeminiService.java)
- [frontend-integration.md](./ai-chat/frontend-integration.md)

The AI flow is more than a generic chatbot wrapper. The backend searches available templates first, injects template context into the prompt, and returns recommendations tied to marketplace data.

Điểm này giúp project khác biệt khi đưa vào CV, vì AI ở đây gắn với dữ liệu nghiệp vụ thay vì chỉ gọi model để trả lời tự do.

## Persistence and Deployment Notes

Persistence is handled with PostgreSQL via Spring Data JPA.

- Maven project definition: [pom.xml](../pom.xml)
- Database migration note: [database/README.md](../database/README.md)
- Chat migration sample: [V001__create_chat_tables.sql](../database/migrations/V001__create_chat_tables.sql)

Deployment evidence is preserved in:

- [dockerfile](../dockerfile)
- [env-vars.yaml](../env-vars.yaml)
- [service-deploy.yaml](../service-deploy.yaml)

These files are intentionally kept as **historical implementation evidence**. They show that the project once targeted containerized/cloud deployment, but they should not be interpreted as proof of a currently working public environment.

## What Is Strong About This Repo

### 1. Realistic Backend Scope

The project models users, sellers, admins, templates, orders, reviews, collections, notifications, payouts, and AI-assisted discovery in one coherent backend.

### 2. Clear Separation of Concerns

Controllers, services, repositories, DTOs, and security components are split in a way that is easy to explain and maintain.

### 3. Third-Party Integration Experience

The repo shows experience with:

- JWT auth
- Google OAuth2
- PostgreSQL/JPA
- Firebase Storage
- PayOS
- SMTP mail
- Swagger/OpenAPI
- Docker / Cloud Run style deployment

### 4. Public-Safe Documentation Posture

The repo now documents its limitations directly instead of hiding them. For a fresher portfolio, that honesty is a strength.

## Current Limitations

### 1. Backend-Only Snapshot

The frontend is not present in this repository. Reviewers should evaluate this primarily as a backend codebase.

### 2. Historical Infrastructure Has Expired

The original Google Cloud project and paid integration environment are no longer running, so full end-to-end reproduction is not guaranteed.

### 3. Local Startup Is Best-Effort

The repo includes a safe local bootstrap flow, but full startup still depends on recreating database schema and third-party credentials.

### 4. Database Bootstrap Is Incomplete Publicly

The public repo does not contain a full checked-in migration history for the entire marketplace schema.

### 5. Chat Session Durability Is Prototype-Level

[ChatSessionManager.java](../src/main/java/com/pandadocs/api/service/ChatSessionManager.java) still stores in-session messages in memory, which is fine for a prototype but not ideal for production durability.

### 6. Documentation Drift Still Exists in Historical AI Docs

Some AI-chat documents were preserved from the original development period and may not match the current code line-for-line.

## Security and Review Readiness

Compared with the earlier project state, the public snapshot is now safer to review:

- sensitive files are excluded by [.gitignore](../.gitignore)
- runtime secrets are environment-driven or local-only
- the public preflight script checks for common publication mistakes
- notification ownership checks are enforced
- payment routes now enforce ownership or admin access
- the PayOS webhook now fails closed on invalid signatures
- OAuth2 login no longer returns the JWT in query parameters
- noisy mail/debug logging and leftover test endpoints have been removed

This does **not** make the project production-grade, but it does make the repository substantially more credible as a public portfolio artifact.

## Portfolio Framing Recommendation

If you put this repo on a Java backend fresher CV, position it like this:

> Spring Boot backend for a document-template marketplace with JWT/OAuth2 authentication, PostgreSQL, Firebase Storage, PayOS payment flow, and Gemini-based AI template recommendation.

Vietnamese version:

> Backend Spring Boot cho nền tảng marketplace bán template tài liệu, có JWT/OAuth2, PostgreSQL, Firebase Storage, PayOS và AI chat hỗ trợ tìm template.

That framing is accurate, concise, and strong enough for recruiter or interviewer screening.

## Final Assessment

PandaDocs is a strong **portfolio backend** and a respectable university project. Its biggest value is breadth: authentication, role-based access, persistence, file storage, payments, documentation, and AI-assisted search all appear in one system.

Điểm hợp lý nhất khi đánh giá dự án này là: rất tốt để đưa vào CV và demo tư duy backend, nhưng vẫn nên được trình bày như một “public-safe academic portfolio snapshot”, không phải production system đang active.

## Appendix: Key Files Worth Reviewing First

- [README.md](../README.md)
- [setup.md](./setup.md)
- [AuthController.java](../src/main/java/com/pandadocs/api/controller/AuthController.java)
- [TemplateController.java](../src/main/java/com/pandadocs/api/controller/TemplateController.java)
- [PaymentController.java](../src/main/java/com/pandadocs/api/controller/PaymentController.java)
- [WebSecurityConfig.java](../src/main/java/com/pandadocs/api/security/WebSecurityConfig.java)
- [ChatService.java](../src/main/java/com/pandadocs/api/service/ChatService.java)
- [GeminiService.java](../src/main/java/com/pandadocs/api/service/GeminiService.java)
- [database/README.md](../database/README.md)
- [service-deploy.yaml](../service-deploy.yaml)