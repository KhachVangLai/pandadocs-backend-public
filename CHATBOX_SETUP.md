# AI Chatbox Setup Guide

## Tổng Quan

AI Chatbox feature sử dụng Google Gemini AI để giúp users tìm kiếm và mua templates thông qua hội thoại tự nhiên.

### Tính Năng Chính
- ✅ Natural language search cho templates
- ✅ AI recommendations dựa trên user intent
- ✅ Quick purchase actions (Mua ngay / Thêm vào library)
- ✅ Rate limiting (10 messages/hour, 30 messages/day)
- ✅ Session management với TTL 30 phút
- ✅ Auto conversation title generation
- ✅ Prompt injection prevention

---

## Bước 1: Database Migration

### Chạy SQL Migration

Mở Supabase Dashboard → SQL Editor → Copy và run script sau:

```sql
-- File: database/migrations/V001__create_chat_tables.sql

-- Table: chat_conversations (lưu metadata, không lưu messages)
CREATE TABLE IF NOT EXISTS chat_conversations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(200),
    message_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_conversations_user_id ON chat_conversations(user_id);
CREATE INDEX idx_chat_conversations_session_id ON chat_conversations(session_id);
CREATE INDEX idx_chat_conversations_last_activity ON chat_conversations(last_activity_at);

-- Table: chat_quota (rate limiting)
CREATE TABLE IF NOT EXISTS chat_quota (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    hourly_count INT NOT NULL DEFAULT 0,
    daily_count INT NOT NULL DEFAULT 0,
    last_hourly_reset TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_daily_reset TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_quota_user_id ON chat_quota(user_id);
```

### Verify Migration

```sql
-- Check tables created
SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'chat_%';

-- Expected output:
-- chat_conversations
-- chat_quota
```

---

## Bước 2: Get Gemini API Key

### Option 1: Google AI Studio (Free Tier - Recommended)

1. Truy cập https://aistudio.google.com/app/apikey
2. Click **"Get API Key"** → **"Create API key in new project"**
3. Copy API key (format: `AIzaSy...`)

**Free Tier Limits:**
- 1,500 requests/day
- 15 requests/minute
- Đủ cho ~150 users active/day

### Option 2: Vertex AI (Paid - Production)

1. Enable Vertex AI API trong GCP Console
2. Sử dụng Service Account credentials
3. Higher limits nhưng cần billing

---

## Bước 3: Environment Variables

### Local Development (.env)

Thêm vào file `.env`:

```bash
# Gemini AI Configuration
GEMINI_API_KEY=your_api_key_here
GEMINI_MODEL=gemini-1.5-flash

# Chat Configuration (optional, có defaults)
CHAT_RATE_LIMIT_HOURLY=10
CHAT_RATE_LIMIT_DAILY=30
CHAT_SESSION_TIMEOUT=30
CHAT_MAX_MESSAGES=50
CHAT_AUTO_TITLE=true
```

### Google Cloud Run (Production)

Thêm secret trong Cloud Run:

```bash
# Via gcloud CLI
gcloud secrets create GEMINI_API_KEY --data-file=-
# Paste your API key, then Ctrl+D

# Update Cloud Run service
gcloud run services update pandadocs-api \
  --update-secrets=GEMINI_API_KEY=GEMINI_API_KEY:latest \
  --region=asia-southeast1
```

Hoặc qua Console:
1. Cloud Run → Service Details → **VARIABLES & SECRETS**
2. Add Variable: `GEMINI_API_KEY` = `your_api_key`
3. Deploy

---

## Bước 4: Build & Deploy

### Local Testing

```bash
# Build project
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Test chatbox endpoint
curl -X POST http://localhost:8080/api/chat/message \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "Tôi cần template resume cho developer"}'
```

### Deploy to Cloud Run

```bash
# Build Docker image
docker build -t gcr.io/your-gcp-project/pandadocs-api:v1 .

# Push to GCR
docker push gcr.io/your-gcp-project/pandadocs-api:v1

# Deploy
gcloud run deploy pandadocs-api \
  --image gcr.io/your-gcp-project/pandadocs-api:v1 \
  --region asia-southeast1 \
  --platform managed
```

---

## Bước 5: API Testing

### 1. Send Message

```bash
POST /api/chat/message
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "sessionId": "optional-uuid",
  "message": "Tôi cần template presentation về marketing giá dưới 100k"
}
```

**Response:**
```json
{
  "sessionId": "abc-123-def",
  "message": "Tôi tìm thấy 3 templates presentation về marketing...",
  "templates": [
    {
      "id": 5,
      "title": "Marketing Presentation Template",
      "price": 50000,
      "previewImage": "https://...",
      "category": "Presentation",
      "rating": 4.5,
      "downloads": 120
    }
  ],
  "actionButtons": [
    {
      "type": "BUY_NOW",
      "label": "Mua ngay (50000đ)",
      "templateId": 5
    },
    {
      "type": "VIEW_DETAILS",
      "label": "Xem chi tiết",
      "templateId": 5
    },
    {
      "type": "CANCEL",
      "label": "Hủy",
      "templateId": 5
    }
  ],
  "conversationTitle": "Templates Marketing"
}
```

### 2. Get Session

```bash
GET /api/chat/session?sessionId=abc-123-def
Authorization: Bearer {JWT_TOKEN}
```

### 3. Purchase Action

```bash
POST /api/chat/purchase-action
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "sessionId": "abc-123-def",
  "action": "BUY_NOW",
  "templateId": 5
}
```

**Response:**
```json
{
  "action": "REDIRECT_TO_PURCHASE",
  "templateId": 5,
  "endpoint": "/api/purchases",
  "message": "Redirecting to checkout..."
}
```

### 4. Clear Session

```bash
DELETE /api/chat/session?sessionId=abc-123-def
Authorization: Bearer {JWT_TOKEN}
```

---

## Rate Limiting

### User Quotas
- **10 messages per hour**
- **30 messages per day**

### Exceeded Response

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Bạn đã vượt quá giới hạn 10 tin nhắn mỗi giờ. Vui lòng thử lại sau."
}
```

Status Code: `429 Too Many Requests`

---

## Session Management

### Session Lifecycle
1. **Create**: Frontend gửi message với `sessionId: null` → Backend tạo UUID
2. **Active**: Session tồn tại trong memory 30 phút kể từ last activity
3. **Expire**: Auto cleanup mỗi 5 phút (scheduled task)

### Session Limits
- **Max messages per session**: 50 (tự động trim old messages)
- **Timeout**: 30 minutes inactivity
- **Concurrent sessions**: 1 active session per user

---

## AI System Prompt

Gemini được configure với system prompt để:
- ✅ CHỈ nói về templates trên PandaDocs
- ✅ KHÔNG trả lời câu hỏi ngoài lề
- ✅ Hỏi xác nhận trước khi hiển thị action buttons
- ✅ Luôn thân thiện và sử dụng tiếng Việt

**File:** `GeminiService.java:SYSTEM_PROMPT`

---

## Troubleshooting

### Problem: "Missing artifact google-ai-generativeai"

**Solution:** Dependency đã được remove. Sử dụng REST API thay vì SDK.

```xml
<!-- ✅ Correct dependencies -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
</dependency>
```

### Problem: "Gemini API error: 429 Too Many Requests"

**Cause:** Exceeded free tier limits (1,500 requests/day)

**Solutions:**
1. Wait until daily reset (midnight PT)
2. Increase user rate limits (reduce quota)
3. Upgrade to paid tier

### Problem: "Chat sessions disappearing"

**Cause:** Cloud Run scales to zero → in-memory sessions lost

**Solutions:**
1. Set min instances = 1 (prevent scale to zero)
2. Migrate to Redis for distributed sessions
3. Accept behavior (sessions are ephemeral by design)

### Problem: "Duplicate constructor ChatSessionNotFoundException"

**Solution:** Fixed - removed duplicate constructor.

---

## Monitoring

### Logs to Watch

```bash
# Gemini API calls
grep "Gemini API" logs

# Rate limit events
grep "Rate limit" logs

# Session cleanup
grep "Cleaned up" logs
```

### Metrics to Track
- Chat messages per day
- API error rate
- Average response time
- Rate limit hit rate

---

## Architecture Diagram

```
┌─────────────────┐
│   Frontend      │
│   (Vercel)      │
└────────┬────────┘
         │ REST API
         │ JWT Auth
┌────────▼─────────────────────────────────────┐
│          Backend (Cloud Run)                  │
│                                               │
│  ChatController → ChatService                 │
│                      ↓                        │
│         ┌────────────┼────────────┐           │
│         │            │            │           │
│    GeminiService  SearchService  RateLimiter │
│         │            │            │           │
│         │            │            │           │
│    ┌────▼────┐  ┌───▼────┐  ┌───▼────┐      │
│    │ Gemini  │  │Template│  │ChatQuota│      │
│    │   API   │  │  Repo  │  │  Repo  │      │
│    └─────────┘  └────────┘  └────────┘      │
└───────────────────────────────────────────────┘
         │                    │
    ┌────▼────┐          ┌────▼────┐
    │ Gemini  │          │Supabase │
    │   AI    │          │   DB    │
    └─────────┘          └─────────┘
```

---

## Next Steps

1. ✅ Run database migration
2. ✅ Get Gemini API key
3. ✅ Add environment variables
4. ✅ Deploy backend
5. 🔲 Implement frontend chatbox UI
6. 🔲 Add analytics tracking
7. 🔲 Monitor usage and optimize

---

## Support

- **Documentation**: Xem code comments trong các service files
- **Issues**: Report tại GitHub Issues
- **API Docs**: Swagger UI tại `/swagger-ui.html`

**Developed by**: Claude Code Assistant
**Date**: 2025-11-02
