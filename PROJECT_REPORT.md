# PandaDocs Backend Project Report

*Báo cáo dự án PandaDocs Backend*

## Executive Summary

PandaDocs is a backend-driven document template marketplace designed around a practical academic product idea: students and other users can browse templates, purchase or download them, store owned assets in a personal library, and interact with a role-based platform that supports sellers, administrators, and AI-assisted discovery. The checked-in implementation is a Spring Boot backend that combines marketplace functionality, file storage, payment orchestration, email-based account flows, and an AI chat assistant for template search and recommendation. In this public snapshot, the application root is the repository root itself.

Về mặt ý tưởng, đây là một mini-project có phạm vi khá rộng so với nhiều đồ án môn học thông thường. Dự án không chỉ dừng ở CRUD cơ bản mà còn mô hình hóa tương đối đầy đủ các vai trò vận hành của một marketplace thực tế: người dùng cuối, người bán, quản trị viên, thanh toán, lưu trữ file, và trợ lý AI.

From a repository perspective, this codebase represents the backend portion of the PandaDocs system rather than a complete full-stack application. The Java application uses Spring Boot 3.5.6 and Java 17 as defined in [pom.xml](./pom.xml), with a layered architecture across controllers, services, repositories, models, DTOs, security, and configuration packages. It also shows clear deployment intent through containerization and Google Cloud Run manifests in [dockerfile](./dockerfile) and [service-deploy.yaml](./service-deploy.yaml).

Tuy nhiên, giá trị của dự án không chỉ nằm ở số lượng tính năng. Điểm đáng chú ý là cách dự án kết hợp nhiều thành phần kỹ thuật tương đối hiện đại: xác thực JWT và OAuth2, PostgreSQL qua JPA, Firebase Storage cho file và ảnh, PayOS cho thanh toán, SMTP cho email, Swagger cho tài liệu API, và Gemini cho AI chat. Điều này cho thấy dự án có định hướng mô phỏng một hệ thống backend gần với tình huống triển khai thật, thay vì chỉ là bài tập mô hình hóa đơn giản.

At the same time, the repository is not yet production-grade. The implementation breadth is strong, but reproducibility, security hardening, and automated verification are still limited. This report therefore presents the project as a serious portfolio-quality backend effort while staying transparent about current risks, technical debt, and repository boundaries.

Nói ngắn gọn, PandaDocs là một backend mini-project có tham vọng lớn, thể hiện tốt năng lực thiết kế hệ thống và tích hợp dịch vụ, nhưng vẫn còn khoảng cách đáng kể giữa mức “hoàn thành chức năng” và mức “sẵn sàng production”.

## Repository Reality Check

This repository is a backend-only public snapshot and does not contain a checked-in React, Next.js, or Vite frontend application. The main source tree is under [src/main/java/com/pandadocs/api](./src/main/java/com/pandadocs/api), while runtime configuration is under [src/main/resources/application.properties](./src/main/resources/application.properties).

Điều này rất quan trọng khi mô tả dự án với giảng viên hoặc đưa vào portfolio: repo hiện tại là backend của hệ thống PandaDocs, không phải toàn bộ sản phẩm hoàn chỉnh từ frontend đến backend.

Frontend behavior can still be inferred from integration-oriented documents such as [frontend-integration.md](./docs/ai-chat/frontend-integration.md) and from environment variables that reference a separate frontend host in [application.properties](./src/main/resources/application.properties) and [service-deploy.yaml](./service-deploy.yaml). The backend clearly expects a separate web client for OAuth redirects, payment return pages, and chat UI rendering.

Nói cách khác, nếu cần demo trải nghiệm người dùng hoàn chỉnh, phần frontend nhiều khả năng tồn tại ở repo khác hoặc mới chỉ được mô tả ở mức tài liệu tích hợp.

## Product Scope and User Roles

PandaDocs models a small but realistic marketplace domain with three main roles:

| Role | Purpose | Representative flows |
| --- | --- | --- |
| `USER` | End-user who browses, buys, downloads, and manages owned templates | Signup/signin, profile management, browsing templates, purchasing, downloading, reviewing, collections, notifications, AI chat |
| `SELLER` | Contributor who uploads templates and manages seller profile/payout visibility | Seller registration, seller profile, template upload, dashboard, payout history |
| `ADMIN` | Platform operator who moderates users and content and manages platform-level actions | User management, template moderation, dashboard, revenue, payouts, notifications |

Vai trò trong hệ thống được phân tách tương đối rõ ràng và phù hợp với cách triển khai của một marketplace thực tế. Đây là một điểm cộng lớn về thiết kế nghiệp vụ vì dự án không chỉ xoay quanh một actor duy nhất.

### Main Functional Flows

- Authentication and identity management through [AuthController.java](./src/main/java/com/pandadocs/api/controller/AuthController.java)
- Public template discovery and detail retrieval through [TemplateController.java](./src/main/java/com/pandadocs/api/controller/TemplateController.java)
- User profile and purchase library management through [UserController.java](./src/main/java/com/pandadocs/api/controller/UserController.java)
- Seller onboarding, seller dashboard, and payout history through [SellerController.java](./src/main/java/com/pandadocs/api/controller/SellerController.java)
- Admin moderation and operational actions through [AdminController.java](./src/main/java/com/pandadocs/api/controller/AdminController.java)
- Purchase and payment processing through [PurchaseController.java](./src/main/java/com/pandadocs/api/controller/PurchaseController.java), [PaymentController.java](./src/main/java/com/pandadocs/api/controller/PaymentController.java), and [OrderController.java](./src/main/java/com/pandadocs/api/controller/OrderController.java)
- AI-assisted template discovery through [ChatController.java](./src/main/java/com/pandadocs/api/controller/ChatController.java), [ChatService.java](./src/main/java/com/pandadocs/api/service/ChatService.java), and [GeminiService.java](./src/main/java/com/pandadocs/api/service/GeminiService.java)

Phạm vi chức năng như trên đủ để chứng minh đây là một backend đa module, không phải bài tập CRUD đơn lẻ. Về mặt học thuật, điều này giúp dự án có giá trị trình bày tốt hơn vì có thể bàn đến kiến trúc, phân quyền, transaction, external integration, và trade-off hệ thống.

## Technical Architecture

The application uses a conventional Spring layered architecture rooted at [src/main/java/com/pandadocs/api](./src/main/java/com/pandadocs/api):

- `controller`: REST endpoints and request/response handling
- `service`: business orchestration and third-party integration logic
- `repository`: Spring Data JPA access to persistence
- `model`: JPA entities and supporting enums
- `dto`: API-facing payload models
- `security`: JWT filters, OAuth2 handlers, user details, and security configuration
- `config`: infrastructure and integration configuration
- `validation`: domain-specific validation such as password rules

Kiến trúc này không mới, nhưng rất phù hợp với quy mô mini-project vì dễ mở rộng, dễ giải thích, và đủ quen thuộc để giảng viên hoặc nhà tuyển dụng đọc nhanh là hiểu cách tổ chức hệ thống.

### Verified Codebase Shape

The repository inspection found the following concrete structure:

| Artifact type | Count |
| --- | ---: |
| Controllers | 13 |
| Services | 11 |
| Repositories | 16 |
| Model classes / enums group root | 24 |
| DTO files | 42 |
| Test files | 1 |

These figures indicate a project with meaningful internal structure rather than a single-controller demo. The main runtime entrypoint is [ApiApplication.java](./src/main/java/com/pandadocs/api/ApiApplication.java), which enables asynchronous work, scheduling, and Spring Data web support.

Số lượng class không tự động đồng nghĩa với chất lượng, nhưng trong trường hợp này nó phản ánh một hệ thống đã được tách lớp tương đối rõ ràng và có nhiều use case độc lập.

### Security and Access Model

Security is defined in [WebSecurityConfig.java](./src/main/java/com/pandadocs/api/security/WebSecurityConfig.java). The backend uses:

- Stateless JWT authentication
- Spring Security method-level role checks
- Google OAuth2 login integration
- Role-based protection for `USER`, `SELLER`, and `ADMIN`
- Public access for auth-related routes, template browsing, and Swagger/OpenAPI pages

Mô hình bảo mật này phù hợp với sản phẩm marketplace có nhiều vai trò. Việc sử dụng JWT kết hợp OAuth2 cũng cho thấy dự án đã nghĩ đến cả đăng nhập truyền thống lẫn đăng nhập bằng bên thứ ba.

## Core Modules

### 1. Authentication and Account Management

[AuthController.java](./src/main/java/com/pandadocs/api/controller/AuthController.java) implements signup, signin, email verification, logout, forgot-password, and reset-password flows. Password hashing uses BCrypt through Spring Security, while custom password validation is handled in [PasswordValidator.java](./src/main/java/com/pandadocs/api/validation/PasswordValidator.java). JWT generation and validation live in the `security/jwt` package.

Luồng tài khoản trong dự án khá đầy đủ đối với một mini-project: có xác minh email, khôi phục mật khẩu, và hỗ trợ OAuth2. Đây là điểm mạnh đáng kể nếu dùng để trình bày năng lực backend.

### 2. Template Marketplace and Asset Management

[TemplateController.java](./src/main/java/com/pandadocs/api/controller/TemplateController.java) is the largest domain-facing controller. It exposes:

- Public template listing and search
- Template detail and category retrieval
- Seller/admin template creation and upload
- Preview image upload
- Review submission and retrieval
- Protected file download
- Popular templates and preview URL generation

Template files and images are stored through Firebase integration in [FirebaseStorageService.java](./src/main/java/com/pandadocs/api/service/FirebaseStorageService.java). This gives the project a more realistic asset-management story than local disk storage.

Về mặt sản phẩm, đây là phân hệ quan trọng nhất vì nó đại diện cho “hàng hóa” của marketplace. Việc tích hợp upload file, preview image, signed URL, và download có kiểm soát giúp hệ thống có chiều sâu hơn rõ rệt.

### 3. User, Seller, and Admin Workflows

- [UserController.java](./src/main/java/com/pandadocs/api/controller/UserController.java) covers profile access, purchase library retrieval, and avatar upload.
- [SellerController.java](./src/main/java/com/pandadocs/api/controller/SellerController.java) supports seller registration, seller profile, bank information, seller dashboard, and payout history.
- [AdminController.java](./src/main/java/com/pandadocs/api/controller/AdminController.java) implements operational capabilities such as user management, template moderation, dashboard data, suggestions, revenue-related views, notifications, and payout actions.

Ba nhóm controller này thể hiện rõ tư duy phân quyền theo vai trò nghiệp vụ. Đây là một đặc điểm giúp PandaDocs trông giống backend của một nền tảng thực tế hơn là một ứng dụng cá nhân đơn vai trò.

### 4. Purchase and Payment Flow

Purchase creation starts in [PurchaseController.java](./src/main/java/com/pandadocs/api/controller/PurchaseController.java), where free templates can be granted directly to a user library and paid templates generate a PayOS checkout link. Payment completion and webhook handling are implemented in [PaymentController.java](./src/main/java/com/pandadocs/api/controller/PaymentController.java), with gateway interaction delegated to [PayOSService.java](./src/main/java/com/pandadocs/api/service/PayOSService.java).

Luồng thanh toán là một điểm nổi bật về mặt portfolio vì nó cho thấy dự án đã đi xa hơn phần “xem dữ liệu” và chạm đến quy trình giao dịch thực tế, dù hiện tại phần hardening bảo mật vẫn chưa hoàn thiện.

### 5. AI Chat and RAG-style Recommendation

The AI assistant is one of the most distinctive features of the project. [ChatController.java](./src/main/java/com/pandadocs/api/controller/ChatController.java) exposes the chat endpoints, [ChatService.java](./src/main/java/com/pandadocs/api/service/ChatService.java) orchestrates search and response building, and [GeminiService.java](./src/main/java/com/pandadocs/api/service/GeminiService.java) calls Gemini over REST.

The current pattern is effectively lightweight RAG:

1. The user sends a natural-language message.
2. The backend detects search intent and queries available templates.
3. Matching templates are injected into the Gemini prompt as context.
4. Gemini returns a constrained answer focused on those templates.
5. The backend can add action buttons such as buy now, view details, or add to library.

Đây là phần giúp dự án khác biệt nhất khi so với các bài làm backend thông thường. Tính năng AI không chỉ gọi model trả lời tự do, mà còn gắn với dữ liệu nghiệp vụ của hệ thống, vì vậy có giá trị giải thích tốt trong buổi bảo vệ hoặc demo.

## Data and Persistence

The primary persistence layer is PostgreSQL via Spring Data JPA, configured in [application.properties](./src/main/resources/application.properties). The application runs with `spring.jpa.hibernate.ddl-auto=validate`, which means the database schema must exist and must match the entity model before the service starts successfully.

Điểm này cho thấy dự án đã chuyển sang tư duy “kiểm soát schema” thay vì để Hibernate tự tạo mọi thứ ở runtime. Tuy nhiên, nó cũng làm việc setup môi trường khó hơn nếu tài liệu migration chưa đầy đủ.

### Main Domain Entities

The inspected model layer includes entities for:

- Users and roles
- Templates and categories
- Orders and order items
- Library ownership records
- Reviews
- Collections
- Notifications
- Seller profiles and seller payouts
- Suggestions
- Chat conversation metadata and chat quota tracking

This entity set is sufficient to model a marketplace lifecycle from onboarding to purchase and post-purchase access.

### Migration Strategy

The repository does not include a complete automated migration story. [database/README.md](./database/README.md) documents manual execution of SQL scripts, and the only checked-in migration is [V001__create_chat_tables.sql](./database/migrations/V001__create_chat_tables.sql), which adds chat-specific tables. There is no full migration history for the entire marketplace schema.

Đây là một hạn chế lớn về khả năng tái hiện môi trường. Người mới clone repo sẽ khó dựng được hệ thống hoàn chỉnh chỉ dựa trên code hiện tại, đặc biệt nếu database gốc đã được tạo thủ công trước đó.

### Hybrid Chat Persistence Model

The AI chat feature is split across two storage approaches:

- Chat metadata and rate-limit quota are persisted in PostgreSQL through `ChatConversation` and `ChatQuota`
- Actual in-session chat messages are stored in memory by [ChatSessionManager.java](./src/main/java/com/pandadocs/api/service/ChatSessionManager.java)

This design is easy to implement and suitable for an initial prototype, but it introduces durability limits in cloud environments where instances can restart or scale down.

Mô hình này hợp lý cho giai đoạn thử nghiệm, nhưng chưa bền vững nếu coi hệ thống là production-grade, đặc biệt trên Cloud Run nơi instance có thể bị thay thế bất kỳ lúc nào.

## Integrations and Deployment

PandaDocs integrates several external services that materially increase the technical depth of the project:

| Integration | Purpose | Primary source anchor |
| --- | --- | --- |
| PostgreSQL | Core relational persistence | [application.properties](./src/main/resources/application.properties) |
| Firebase Storage | Template files, avatars, preview images, signed URLs | [FirebaseStorageService.java](./src/main/java/com/pandadocs/api/service/FirebaseStorageService.java) |
| PayOS | Payment link creation and payment verification flow | [PayOSService.java](./src/main/java/com/pandadocs/api/service/PayOSService.java) |
| Google Gemini | AI chat and template recommendation | [GeminiService.java](./src/main/java/com/pandadocs/api/service/GeminiService.java) |
| SMTP / SendGrid-style mail | Verification and password reset email | [EmailService.java](./src/main/java/com/pandadocs/api/service/EmailService.java) |
| Swagger / OpenAPI | API exploration and documentation | [pom.xml](./pom.xml), [WebSecurityConfig.java](./src/main/java/com/pandadocs/api/security/WebSecurityConfig.java) |
| Docker | Containerized packaging | [dockerfile](./dockerfile) |
| Google Cloud Run | Intended production deployment target | [service-deploy.yaml](./service-deploy.yaml) |

The deployment files show a clear cloud deployment intention. [dockerfile](./dockerfile) uses a multi-stage Maven build and a lightweight runtime image, while [service-deploy.yaml](./service-deploy.yaml) configures Cloud Run environment variables, secrets, resource limits, and a mounted Firebase credential volume. The deployment configuration also points to a separate frontend host through environment variables such as `OAUTH2_FRONTEND_REDIRECT_URL` and payment return URLs.

Về mặt portfolio, phần triển khai là một điểm cộng mạnh. Dù chưa hoàn thiện hoàn toàn, việc có Docker và manifest Cloud Run giúp dự án trông gần với quy trình triển khai thật hơn nhiều so với các đồ án chỉ chạy local.

## Strengths

### Realistic Product Scope

The project models a believable marketplace domain instead of a toy example. It includes multiple actor types, transactional flows, owned-content access, moderation, and AI-assisted discovery.

Điểm mạnh này khiến dự án dễ kể chuyện hơn khi trình bày: không chỉ có “backend quản lý dữ liệu”, mà là một hệ thống phục vụ một ý tưởng sản phẩm rõ ràng.

### Meaningful Third-Party Integration

The backend is not isolated from real-world services. It integrates cloud storage, payment processing, email delivery, and AI inference. This materially increases the engineering scope and demonstrates practical integration experience.

Việc tích hợp nhiều dịch vụ ngoài cho thấy dự án không chỉ mạnh ở code nội bộ mà còn ở khả năng kết nối hệ thống.

### Clear Layering and Separation of Concerns

The controller-service-repository split is consistent, DTO usage is extensive, and major features are broken into dedicated modules. This makes the codebase easier to explain, navigate, and extend.

Sự tách lớp rõ ràng là một điểm cộng lớn nếu dùng dự án để chứng minh tư duy thiết kế backend có tổ chức.

### Distinctive AI Feature

The AI assistant is not just a chatbot bolt-on. It is connected to product data through a template-search-first flow, making it a business-facing feature rather than a generic demo prompt.

Đây là yếu tố giúp dự án nổi bật hơn trong portfolio, vì AI được dùng để hỗ trợ nghiệp vụ chứ không chỉ để “cho có”.

### Deployment Awareness

The presence of [dockerfile](./dockerfile), [env-vars.yaml](./env-vars.yaml), and [service-deploy.yaml](./service-deploy.yaml) indicates that the project was designed with deployment in mind, not only local development.

## Current Limitations / Hạn chế hiện tại

### 1. Backend-only Repository Scope

This repository does not contain a checked-in frontend implementation. The backend references an external frontend for OAuth redirects, payment result pages, and chat UI integration, but the UI code itself is not part of the inspected repo.

Điều này không làm dự án yếu đi về backend, nhưng cần nói rõ để tránh tạo kỳ vọng sai rằng repo hiện tại chứa toàn bộ hệ thống.

### 2. Documentation and Implementation Drift

There is drift between some integration documents and the checked-in controller behavior. For example, [frontend-integration.md](./docs/ai-chat/frontend-integration.md) documents path-style chat session routes, while [ChatController.java](./src/main/java/com/pandadocs/api/controller/ChatController.java) uses query-parameter-based session endpoints. The guide also suggests flows that do not map cleanly to existing controllers.

Đây là dạng “nợ tài liệu” thường gặp khi tính năng thay đổi nhanh hơn tài liệu. Tuy không phá vỡ kiến trúc, nó làm onboarding và demo trở nên khó hơn.

### 3. Minimal Automated Testing

The project currently contains only one test file, [ApiApplicationTests.java](./src/test/java/com/pandadocs/api/ApiApplicationTests.java), which is a simple context-load smoke test. There are no meaningful controller, service, repository, payment, chat, or security tests checked into the repo.

Về mặt học thuật và kỹ thuật, đây là hạn chế đáng kể. Phạm vi chức năng lớn nhưng độ phủ kiểm thử rất thấp, nên mức độ tin cậy thực tế của hệ thống khó được chứng minh bằng automation.

### 4. Local Verification Gap in Current Environment

During inspection in the current environment, Java was available but the Maven wrapper could not be used successfully: `mvnw.cmd` failed before Maven startup. As a result, this report is grounded mainly in static repository inspection rather than a full successful local test run.

Điểm này không nhất thiết có nghĩa project không chạy được ở mọi máy, nhưng nó cho thấy quy trình local verification hiện chưa thực sự trơn tru và đáng tin cậy.

### 5. Payment Security Gaps

[PayOSService.java](./src/main/java/com/pandadocs/api/service/PayOSService.java) currently contains a placeholder `verifyWebhookSignature()` implementation that always returns `true`. In [PaymentController.java](./src/main/java/com/pandadocs/api/controller/PaymentController.java), the signature verification block is commented out. In addition, [WebSecurityConfig.java](./src/main/java/com/pandadocs/api/security/WebSecurityConfig.java) does not clearly permit the external PayOS webhook route, which creates a likely integration risk for real webhook delivery.

Đây là rủi ro nghiêm trọng nhất trong phân hệ thanh toán. Tính năng payment có mặt, nhưng phần xác minh callback từ cổng thanh toán chưa đủ chặt để coi là production-safe.

### 6. In-memory Chat Session Fragility

[ChatSessionManager.java](./src/main/java/com/pandadocs/api/service/ChatSessionManager.java) stores chat messages in memory only. This makes the feature simple and responsive for a prototype, but session state can be lost on instance restart, redeploy, or scale events in Cloud Run.

Hạn chế này đã được tài liệu chat thừa nhận gián tiếp, và nó là trade-off chấp nhận được cho prototype, nhưng không phải lựa chọn tốt cho hệ thống cần độ bền phiên làm việc cao.

### 7. Sensitive Artifact Exposure

The original private source contained a Firebase service-account JSON file and a backup properties file with historical credential-looking values. Those artifacts are intentionally omitted from this public snapshot and should never be tracked in a public repository.

Đây là vấn đề về secret hygiene. Dù là project học tập, việc commit credential thật hoặc dữ liệu cấu hình nhạy cảm vẫn là một điểm trừ lớn về an toàn thông tin.

### 8. Local Secret Handling Still Requires Operator Discipline

The public snapshot now routes payment callback URLs and most runtime-sensitive values through environment-backed configuration rather than hardcoded controller constants. It also includes a local-profile template and publication check scripts. However, safe use still depends on the developer keeping real secrets in untracked local files or external secret managers and rotating any credentials that were exposed in the original private history.

Vấn đề này không quá lớn về mặt kiến trúc, nhưng nó phản ánh tình trạng cấu hình chưa đồng nhất giữa code và environment.

### 9. Verbose Mail Diagnostics

[EmailService.java](./src/main/java/com/pandadocs/api/service/EmailService.java) no longer logs secret previews, but it still emits fairly verbose startup diagnostics such as SMTP host, username, and whether a password is present. That is much safer than exposing credential fragments, yet it is still noisier than a production-hardened logging posture would usually allow.

Đây là một chi tiết nhỏ nhưng quan trọng, vì nó cho thấy hệ thống vẫn còn thiếu bước hardening log trước khi có thể được xem là an toàn hơn trong môi trường thật.

### 10. Missing Ownership Enforcement in Notifications

[NotificationController.java](./src/main/java/com/pandadocs/api/controller/NotificationController.java) includes a comment acknowledging that an ownership check should be added before marking a notification as read. This indicates a known authorization gap in the current implementation.

Việc để lại comment kiểu này là hữu ích cho phát triển, nhưng nó cũng xác nhận rằng một số ràng buộc bảo mật vẫn chưa được đóng kín trong code hiện tại.

## Final Assessment

PandaDocs is a strong backend-centric mini-project with clear portfolio value. It demonstrates a much wider engineering surface than a typical classroom CRUD assignment by combining a marketplace domain, role-based access control, file storage, payment orchestration, email flows, and AI-assisted product discovery in one coherent backend system.

Nếu đánh giá ở góc độ năng lực kỹ thuật, dự án thể hiện tốt khả năng thiết kế backend đa module, tích hợp dịch vụ bên ngoài, tổ chức code theo lớp, và mô hình hóa nghiệp vụ có nhiều vai trò. Đây là những điểm đủ tốt để đưa vào portfolio hoặc dùng làm nội dung trình bày với giảng viên.

At the same time, the project is more convincing as an ambitious backend prototype than as a hardened production system. The biggest gaps are limited test coverage, partial documentation drift, incomplete migration reproducibility, and several security/configuration issues that should be addressed before public deployment or open-source publication.

Kết luận cân bằng nhất là: PandaDocs đã đạt mức rất tốt về độ rộng chức năng và ý tưởng triển khai backend, nhưng vẫn cần thêm kiểm thử, chuẩn hóa tài liệu, quản lý secrets, và hardening bảo mật để tiến gần hơn tới chất lượng production.

## Appendix: Key Files Inspected

The following files were the main anchors for this report:

- [pom.xml](./pom.xml)
- [src/main/resources/application.properties](./src/main/resources/application.properties)
- [src/main/java/com/pandadocs/api/ApiApplication.java](./src/main/java/com/pandadocs/api/ApiApplication.java)
- [src/main/java/com/pandadocs/api/security/WebSecurityConfig.java](./src/main/java/com/pandadocs/api/security/WebSecurityConfig.java)
- [src/main/java/com/pandadocs/api/security/oauth2/OAuth2LoginSuccessHandler.java](./src/main/java/com/pandadocs/api/security/oauth2/OAuth2LoginSuccessHandler.java)
- [src/main/java/com/pandadocs/api/controller/AuthController.java](./src/main/java/com/pandadocs/api/controller/AuthController.java)
- [src/main/java/com/pandadocs/api/controller/TemplateController.java](./src/main/java/com/pandadocs/api/controller/TemplateController.java)
- [src/main/java/com/pandadocs/api/controller/UserController.java](./src/main/java/com/pandadocs/api/controller/UserController.java)
- [src/main/java/com/pandadocs/api/controller/SellerController.java](./src/main/java/com/pandadocs/api/controller/SellerController.java)
- [src/main/java/com/pandadocs/api/controller/AdminController.java](./src/main/java/com/pandadocs/api/controller/AdminController.java)
- [src/main/java/com/pandadocs/api/controller/PurchaseController.java](./src/main/java/com/pandadocs/api/controller/PurchaseController.java)
- [src/main/java/com/pandadocs/api/controller/PaymentController.java](./src/main/java/com/pandadocs/api/controller/PaymentController.java)
- [src/main/java/com/pandadocs/api/controller/ChatController.java](./src/main/java/com/pandadocs/api/controller/ChatController.java)
- [src/main/java/com/pandadocs/api/controller/NotificationController.java](./src/main/java/com/pandadocs/api/controller/NotificationController.java)
- [src/main/java/com/pandadocs/api/service/ChatService.java](./src/main/java/com/pandadocs/api/service/ChatService.java)
- [src/main/java/com/pandadocs/api/service/GeminiService.java](./src/main/java/com/pandadocs/api/service/GeminiService.java)
- [src/main/java/com/pandadocs/api/service/ChatSessionManager.java](./src/main/java/com/pandadocs/api/service/ChatSessionManager.java)
- [src/main/java/com/pandadocs/api/service/PayOSService.java](./src/main/java/com/pandadocs/api/service/PayOSService.java)
- [src/main/java/com/pandadocs/api/service/EmailService.java](./src/main/java/com/pandadocs/api/service/EmailService.java)
- [database/README.md](./database/README.md)
- [database/migrations/V001__create_chat_tables.sql](./database/migrations/V001__create_chat_tables.sql)
- [dockerfile](./dockerfile)
- [env-vars.yaml](./env-vars.yaml)
- [service-deploy.yaml](./service-deploy.yaml)
- [frontend-integration.md](./docs/ai-chat/frontend-integration.md)

---

**One-sentence characterization**

PandaDocs is a Spring Boot backend for a document-template marketplace that combines role-based commerce flows, cloud storage, payment integration, and a Gemini-powered template recommendation assistant in a single academic portfolio project.

Mô tả ngắn gọn: PandaDocs là backend Spring Boot cho một nền tảng mua bán template tài liệu, kết hợp phân quyền người dùng, lưu trữ file trên cloud, thanh toán trực tuyến, và trợ lý AI hỗ trợ tìm kiếm template.
