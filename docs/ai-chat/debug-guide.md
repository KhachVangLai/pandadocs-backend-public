# AI Chatbox - Hướng Dẫn Debug

## Vấn đề: Nhận message "Xin lỗi, tôi không thể xử lý yêu cầu..." với templates array rỗng

### Có 3 nguyên nhân chính:

---

## 1. GEMINI_API_KEY chưa được cấu hình

### ✅ Kiểm tra:

Mở file `src/main/resources/application-local.properties` và kiểm tra:

```properties
# Google Gemini Configuration
gemini.api.key=your_gemini_api_key_here  # ← Dùng local config, không commit key thật
gemini.api.base-url=https://generativelanguage.googleapis.com
gemini.api.model=gemini-1.5-flash
```

### 🔧 Cách fix:

1. Truy cập: https://aistudio.google.com/app/apikey
2. Tạo API key mới (miễn phí)
3. Copy API key và paste vào `application-local.properties`:
   ```properties
   gemini.api.key=your_gemini_api_key_here
   ```
4. Restart Spring Boot application

### 📋 Kiểm tra log:

Nếu API key chưa được set, bạn sẽ thấy log:
```
ERROR GeminiService - GEMINI_API_KEY is not configured!
```

---

## 2. Database chưa có Templates với status PUBLISHED

### ✅ Kiểm tra:

Kết nối đến Supabase PostgreSQL và chạy query:

```sql
-- Đếm số templates với status PUBLISHED
SELECT COUNT(*) FROM templates WHERE status = 'PUBLISHED';

-- Xem danh sách templates PUBLISHED
SELECT id, title, price, category_id, downloads, rating, status
FROM templates
WHERE status = 'PUBLISHED'
LIMIT 10;
```

### 🔧 Cách fix:

**Nếu kết quả = 0**, bạn cần:

1. **Thêm templates mới** hoặc
2. **Update templates hiện có thành PUBLISHED**:

```sql
-- Cập nhật template có ID = 1 thành PUBLISHED
UPDATE templates SET status = 'PUBLISHED' WHERE id = 1;

-- Hoặc update tất cả
UPDATE templates SET status = 'PUBLISHED';
```

### 📋 Kiểm tra log:

Trong Spring Boot logs, bạn sẽ thấy:
```
INFO TemplateSearchService - Found 0 templates matching criteria
INFO ChatService - Passing 0 templates to Gemini for context
```

---

## 3. Gemini API call thất bại (Network, Quota, API Error)

### ✅ Kiểm tra logs:

Chạy application và gửi message qua chatbox, sau đó xem logs:

**Log thành công:**
```
INFO GeminiService - Calling Gemini API: https://generativelanguage.googleapis.com with model gemini-1.5-flash
INFO GeminiService - Gemini API call successful, response length: 1234 chars
```

**Log lỗi (ví dụ):**
```
ERROR GeminiService - Gemini API error: HTTP 403 - {"error": {"code": 403, "message": "API key not valid"}}
ERROR GeminiService - Gemini API error: HTTP 429 - {"error": {"code": 429, "message": "Resource exhausted"}}
ERROR ChatService - Error calling Gemini API: ...
```

### 🔧 Cách fix theo từng error:

#### HTTP 401/403 - API Key không hợp lệ
- Kiểm tra lại API key trong application-local.properties
- Tạo API key mới tại https://aistudio.google.com/app/apikey
- Đảm bảo không có khoảng trắng thừa trong API key

#### HTTP 429 - Quota vượt quá giới hạn
- Gemini 1.5 Flash free tier: **1,500 requests/day**
- Đợi 24h để quota reset
- Hoặc nâng cấp lên paid plan

#### HTTP 400 - Request không hợp lệ
- Kiểm tra request body format
- Enable debug log để xem request body:
  ```properties
  logging.level.com.pandadocs.api.service.GeminiService=DEBUG
  ```

#### HTTP 500 - Gemini API internal error
- Thử lại sau vài phút
- Gemini API đôi khi bị downtime

---

## Quy trình Debug từng bước

### Bước 1: Enable verbose logging

Thêm vào `application-local.properties`:
```properties
# Enable debug logs for chatbox services
logging.level.com.pandadocs.api.service.ChatService=DEBUG
logging.level.com.pandadocs.api.service.GeminiService=DEBUG
logging.level.com.pandadocs.api.service.TemplateSearchService=DEBUG
```

### Bước 2: Restart application
```bash
mvn spring-boot:run
```

### Bước 3: Gửi test message qua API

```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "sessionId": null,
    "message": "Tìm template về CV"
  }'
```

### Bước 4: Phân tích logs

Logs sẽ cho bạn biết chính xác vấn đề ở đâu:

```
# 1. Templates search
INFO TemplateSearchService - Found 5 templates matching criteria

# 2. Templates passed to Gemini
INFO ChatService - Passing 5 templates to Gemini for context

# 3. Gemini API call
INFO GeminiService - Calling Gemini API: https://generativelanguage.googleapis.com with model gemini-1.5-flash
DEBUG GeminiService - Request body: {...}
INFO GeminiService - Gemini API call successful, response length: 456 chars
DEBUG GeminiService - Full Gemini API response: {...}

# 4. Final response
DEBUG ChatService - Gemini response: Tôi tìm thấy 5 template về CV...
```

---

## Test nhanh với cURL

### 1. Kiểm tra health của chatbox endpoint

```bash
# Login trước để lấy JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'

# Copy accessToken từ response
```

### 2. Gửi message test

```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "sessionId": null,
    "message": "Xin chào"
  }'
```

### 3. Expected response (thành công)

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Xin chào! Tôi là trợ lý AI của PandaDocs. Tôi có thể giúp bạn tìm template phù hợp...",
  "templates": [
    {
      "id": 1,
      "title": "Template CV chuyên nghiệp",
      "price": 50000,
      "category": "Resume/CV",
      "rating": 4.5,
      "downloads": 1234
    }
  ],
  "actionButtons": null,
  "conversationTitle": "Xin chào"
}
```

---

## Checklist Tổng hợp

- [ ] GEMINI_API_KEY đã được set trong application-local.properties
- [ ] Database có ít nhất 1 template với status=PUBLISHED
- [ ] Application đã restart sau khi config GEMINI_API_KEY
- [ ] Logs không có error "GEMINI_API_KEY is not configured"
- [ ] Logs không có error "Gemini API error: HTTP 4xx/5xx"
- [ ] TemplateSearchService tìm thấy > 0 templates
- [ ] Gemini API call successful (HTTP 200)

---

## Fallback Response

Nếu Gemini API fail nhưng có templates, bạn sẽ nhận được fallback response:

```
Tôi tìm thấy 5 templates có thể phù hợp với bạn:

1. **Template CV chuyên nghiệp**
   Mô tả ngắn...
   Giá: **50000 VNĐ**
   Danh mục: Resume/CV

2. **Mẫu báo cáo khoa học**
   ...

Bạn có muốn xem chi tiết template nào không?
```

Nếu bạn thấy response này → Gemini API đang fail, nhưng template search hoạt động tốt.

---

## Liên hệ hỗ trợ

Nếu vẫn gặp vấn đề sau khi kiểm tra tất cả các bước trên:
1. Export full logs (from application start to API call)
2. Screenshot response body
3. Share database query results
