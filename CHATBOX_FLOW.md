# AI Chatbox - Luồng Hoạt Động Chi Tiết

## 🎯 Câu hỏi thường gặp

### ❓ "Limit 10 templates có nghĩa là AI chỉ biết 10 templates?"

**KHÔNG!** Limit 10 nghĩa là **mỗi request chỉ gửi tối đa 10 templates RELEVANT nhất** cho Gemini, không phải AI chỉ biết 10 templates cố định.

### ❓ "Nếu tôi thêm template mới vào database thì AI có biết không?"

**CÓ!** AI sẽ biết ngay lập tức vì:
- Mỗi request đều **search real-time** từ database
- Không có cache hay hardcode
- Template mới → xuất hiện trong search results ngay

### ❓ "Tại sao không gửi TẤT CẢ templates cho AI?"

Vì:
1. **Token limit**: Gemini có giới hạn context window
2. **Performance**: Quá nhiều data → slow, expensive
3. **Quality**: 10 relevant templates > 100 random templates

---

## 📊 Kiến trúc RAG Pattern

```
┌─────────────┐
│    USER     │
│ "Tìm CV"   │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────┐
│          ChatController.java                │
│  @PostMapping("/api/chat/message")          │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│          ChatService.java                   │
│  1. detectAndSearchTemplates(userMessage)   │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│      TemplateSearchService.java             │
│  2. searchTemplates(keyword, category,      │
│                      maxPrice, limit=10)    │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│           DATABASE (Supabase)               │
│  SELECT * FROM templates                    │
│  WHERE status = 'PUBLISHED'                 │
│    AND title LIKE '%CV%'                    │
│  ORDER BY downloads DESC                    │
│  LIMIT 10                    ←── DYNAMIC!   │
└──────────────────┬──────────────────────────┘
                   │
                   ▼ Return 10 templates
┌─────────────────────────────────────────────┐
│          ChatService.java                   │
│  3. Pass templates to Gemini                │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│          GeminiService.java                 │
│  4. buildTemplateContext(templates)         │
│     → Format 10 templates thành text        │
│  5. Inject vào prompt                       │
│  6. Call Gemini API                         │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────┐
│         Google Gemini API                   │
│  Đọc context chứa 10 templates              │
│  Generate response dựa trên context         │
└──────────────────┬──────────────────────────┘
                   │
                   ▼ AI Response
┌─────────────────────────────────────────────┐
│            USER                             │
│  "Tôi tìm thấy 5 CV templates..."           │
└─────────────────────────────────────────────┘
```

---

## 🔍 Chi tiết từng bước

### Bước 1: Detect Search Intent

**Code**: `ChatService.detectAndSearchTemplates()`

```java
String userMessage = "Tôi cần template CV cho developer";

// Detect keywords
boolean isSearchIntent =
    message.contains("tìm") ||      // ✅ Match
    message.contains("template") || // ✅ Match
    message.contains("cần");        // ✅ Match

// → isSearchIntent = true → Proceed to search
```

### Bước 2: Search Templates từ Database

**Code**: `TemplateSearchService.searchTemplates()`

```java
// User message: "Tôi cần template CV cho developer"

String keyword = "CV developer";  // Extracted from message
String category = extractCategory("CV"); // → "Resume/CV"
Double maxPrice = null;  // No price mentioned
Integer limit = 10;

// Execute search
List<Template> allTemplates = templateRepository.findByStatus(PUBLISHED);
// → Database có 50 templates PUBLISHED

// Filter by keyword
allTemplates = allTemplates.stream()
    .filter(t -> t.getTitle().contains("CV") ||
                 t.getTitle().contains("developer") ||
                 t.getDescription().contains("CV") ||
                 t.getDescription().contains("developer"))
    .collect(Collectors.toList());
// → Còn 12 templates match keyword

// Sort by relevance
allTemplates.sort((t1, t2) -> {
    int downloadCompare = Integer.compare(t2.downloads, t1.downloads);
    if (downloadCompare != 0) return downloadCompare;
    return Float.compare(t2.rating, t1.rating);
});

// Limit to 10
List<Template> results = allTemplates.stream().limit(10).collect(...);
// → Return top 10 trong 12 templates match
```

**Kết quả**: 10 templates MOST RELEVANT được return

---

### Bước 3: Build Template Context

**Code**: `GeminiService.buildTemplateContext()`

```java
String context = """
=== DANH SÁCH TEMPLATES CÓ SẴN ===

Template #1:
  - ID: 47
  - Tên: CV Developer Full-stack
  - Mô tả: Template CV chuyên nghiệp cho lập trình viên...
  - Giá: MIỄN PHÍ
  - Danh mục: Resume/CV
  - Đánh giá: 4.8/5 ⭐
  - Lượt tải: 1234

Template #2:
  - ID: 23
  - Tên: Mẫu CV cho Software Engineer
  - Mô tả: CV hiện đại, ATS-friendly...
  - Giá: 30000 VNĐ
  - Danh mục: Resume/CV
  - Đánh giá: 4.5/5 ⭐
  - Lượt tải: 856

... (8 templates nữa)

=== KẾT THÚC DANH SÁCH ===

Hãy dựa vào danh sách trên để trả lời câu hỏi của người dùng.
KHÔNG đề cập đến templates không có trong danh sách.
""";
```

---

### Bước 4: Inject Context vào Gemini Prompt

**Code**: `GeminiService.buildRequestBodyWithContext()`

```json
{
  "contents": [
    {
      "role": "user",
      "parts": [{
        "text": "Bạn là trợ lý AI của PandaDocs...\n\n=== DANH SÁCH TEMPLATES CÓ SẴN ===\n\nTemplate #1:\n..."
      }]
    },
    {
      "role": "user",
      "parts": [{
        "text": "Tôi cần template CV cho developer"
      }]
    }
  ]
}
```

**Token count ước tính**:
- System prompt: ~500 tokens
- Template context (10 templates): ~2,000 tokens
- User message: ~20 tokens
- **Total**: ~2,520 tokens (OK cho Gemini 1.5 Flash - limit 30k tokens)

---

### Bước 5: Gemini Generate Response

Gemini đọc context và generate response:

```
"Tôi tìm thấy 10 template CV chuyên nghiệp phù hợp cho developer.
Đặc biệt, tôi giới thiệu bạn:

1. CV Developer Full-stack (MIỄN PHÍ) - Đánh giá 4.8/5
2. Mẫu CV cho Software Engineer (30,000đ) - Đánh giá 4.5/5
...

Bạn có muốn xem chi tiết template nào không?"
```

---

## 🔄 Ví dụ thực tế

### Scenario 1: Database ban đầu có 20 templates

```
User: "Tìm template presentation"

Database query:
  SELECT * FROM templates
  WHERE status = 'PUBLISHED'
    AND (title LIKE '%presentation%' OR description LIKE '%presentation%')
  ORDER BY downloads DESC
  LIMIT 10;

Result: 8 templates match
→ AI nhận 8 templates
→ AI response: "Tôi tìm thấy 8 templates về presentation..."
```

### Scenario 2: Bạn thêm 5 presentation templates mới

```
(Không cần restart application!)

User: "Tìm template presentation"

Database query:
  SELECT * FROM templates
  WHERE status = 'PUBLISHED'
    AND (title LIKE '%presentation%' OR description LIKE '%presentation%')
  ORDER BY downloads DESC
  LIMIT 10;

Result: 13 templates match (8 cũ + 5 mới)
→ Lấy top 10 theo downloads
→ AI nhận 10 templates (có thể bao gồm templates mới nếu chúng có downloads cao)
→ AI response: "Tôi tìm thấy nhiều templates về presentation..."
```

### Scenario 3: User hỏi cụ thể hơn

```
User: "Tìm template presentation miễn phí"

Search logic:
  - Keyword: "presentation"
  - MaxPrice: 0 (detected "miễn phí")

Database query:
  SELECT * FROM templates
  WHERE status = 'PUBLISHED'
    AND (title LIKE '%presentation%' OR description LIKE '%presentation%')
    AND price = 0
  ORDER BY downloads DESC
  LIMIT 10;

Result: 3 free templates match
→ AI nhận 3 templates
→ AI response: "Tôi tìm thấy 3 templates presentation miễn phí..."
```

---

## ⚙️ Tùy chỉnh số lượng templates

### Option 1: Tăng limit cố định (hiện tại: 10)

File: `ChatService.java`

```java
// Line 95
templates = templateSearchService.getPopularTemplates(10); // ← Đổi thành 15

// Line 280
return templateSearchService.searchTemplates(userMessage, category, maxPrice, 10); // ← Đổi thành 15
```

**Lưu ý**: Không nên > 20 vì:
- Gemini response chậm
- Token cost tăng
- Quality giảm (quá nhiều options)

### Option 2: Dynamic limit dựa trên user plan

```java
// VIP users → 20 templates
// Free users → 10 templates

int limit = user.isPremium() ? 20 : 10;
return templateSearchService.searchTemplates(userMessage, category, maxPrice, limit);
```

### Option 3: Config trong application.properties

```properties
# Add to application.properties
chat.max-templates-per-request=10

# Add to GeminiConfig.java
@Value("${chat.max-templates-per-request:10}")
private int maxTemplatesPerRequest;

// Use in ChatService.java
return templateSearchService.searchTemplates(userMessage, category, maxPrice,
    geminiConfig.getMaxTemplatesPerRequest());
```

---

## 📈 Monitoring & Debugging

### Log để xem AI nhận bao nhiêu templates:

```
INFO ChatService - Passing 10 templates to Gemini for context
INFO TemplateSearchService - Found 12 templates matching criteria
```

Từ logs trên:
- Database có 12 templates match
- Nhưng chỉ pass 10 (top relevant nhất) cho Gemini
- 2 templates còn lại bị skip vì rank thấp hơn

### Test thử:

```bash
# 1. Gửi message
curl -X POST http://localhost:8080/api/chat/message \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{"message": "Tìm template CV"}'

# 2. Xem logs
tail -f logs/spring-boot-logger.log | grep "templates"

# 3. Check response
# Đếm số templates trong response.templates array
```

---

## 🎯 Kết luận

| Câu hỏi | Trả lời |
|---------|---------|
| AI có biết tất cả templates trong database? | **CÓ** - Search real-time mỗi request |
| AI có giới hạn chỉ 10 templates? | **KHÔNG** - Mỗi request chọn top 10 RELEVANT nhất |
| Thêm template mới thì AI biết không? | **CÓ** - Ngay lập tức, không cần restart |
| Tại sao không gửi hết templates cho AI? | Performance, cost, quality |
| Có thể tăng limit lên không? | **CÓ** - Nhưng không nên quá 20 |

**TL;DR**:
- Database có 100 templates → AI có thể "thấy" cả 100
- Nhưng mỗi request chỉ gửi 10 **RELEVANT nhất** cho Gemini để optimize performance
- Templates mới → AI biết ngay vì search real-time
