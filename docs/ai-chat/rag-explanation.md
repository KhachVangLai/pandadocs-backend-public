# RAG (Retrieval Augmented Generation) Pattern

## Vấn Đề Ban Đầu ❌

Trước khi implement RAG, Gemini AI **không biết gì** về templates cụ thể trên website:

```
User: "Tôi cần template resume cho developer"
  ↓
TemplateSearchService tìm → [Template A, Template B, Template C]
  ↓
Gemini generate response... NHƯNG KHÔNG BIẾT KẾT QUẢ TÌM KIẾM!
  ↓
Gemini: "Bạn có thể thử tìm template resume trên PandaDocs..." (nói chung chung)
```

**Vấn đề**: Gemini đang "nói mù" - không dựa trên data thực tế từ database!

---

## Giải Pháp: RAG Pattern ✅

**RAG** = **Retrieval** (Tìm kiếm) + **Augmented** (Tăng cường) + **Generation** (Sinh text)

### Flow Mới:

```
User: "Tôi cần template resume cho developer"
  ↓
1. RETRIEVAL: TemplateSearchService tìm templates
   → Result: [Template A, Template B, Template C]
  ↓
2. AUGMENTATION: Inject template data vào prompt
   → Context: "=== DANH SÁCH TEMPLATES CÓ SẴN ===
                Template #1: Developer Resume Template
                  - ID: 5
                  - Giá: 50,000 VNĐ
                  - Đánh giá: 4.5/5
                Template #2: Professional CV Template
                  - ID: 8
                  - Giá: MIỄN PHÍ
                  ..."
  ↓
3. GENERATION: Gemini generate response DỰA TRÊN DATA THỰC TẾ
   → "Tôi tìm thấy 2 templates resume phù hợp:
      1. Developer Resume Template (50,000đ) - Đánh giá 4.5⭐
      2. Professional CV Template (MIỄN PHÍ)
      Bạn muốn xem template nào?"
```

---

## Implementation Details

### 1. Template Context Building

File: [`GeminiService.java:168`](F:/FPTU/PandaProject/api/src/main/java/com/pandadocs/api/service/GeminiService.java:168)

```java
private String buildTemplateContext(List<TemplateCardDTO> templates) {
    if (templates.isEmpty()) {
        return "Hiện tại không tìm thấy template nào phù hợp.";
    }

    StringBuilder context = new StringBuilder();
    context.append("\n\n=== DANH SÁCH TEMPLATES CÓ SẴN ===\n\n");

    for (TemplateCardDTO template : templates) {
        context.append(String.format("Template #%d:\n", i + 1));
        context.append(String.format("  - ID: %d\n", template.getId()));
        context.append(String.format("  - Tên: %s\n", template.getTitle()));
        context.append(String.format("  - Mô tả: %s\n", template.getDescription()));
        context.append(String.format("  - Giá: %s\n",
            template.getPrice() == 0 ? "MIỄN PHÍ" : String.format("%.0f VNĐ", template.getPrice())));
        context.append(String.format("  - Danh mục: %s\n", template.getCategory()));
        context.append(String.format("  - Đánh giá: %.1f/5 ⭐\n", template.getRating()));
        context.append(String.format("  - Lượt tải: %d\n", template.getDownloads()));
    }

    context.append("=== KẾT THÚC DANH SÁCH ===\n");
    context.append("Hãy dựa vào danh sách trên để trả lời. ");
    context.append("KHÔNG đề cập templates không có trong danh sách.\n");

    return context.toString();
}
```

**Kết quả**: Template data được format thành text structure mà Gemini có thể hiểu.

---

### 2. Context Injection

File: [`GeminiService.java:250`](F:/FPTU/PandaProject/api/src/main/java/com/pandadocs/api/service/GeminiService.java:250)

```java
private JsonObject buildRequestBodyWithContext(
        String userMessage,
        List<ChatMessage> conversationHistory,
        String templateContext) {

    // Add system prompt + template context as FIRST message
    JsonObject systemContent = new JsonObject();
    systemContent.addProperty("role", "user");
    JsonObject systemPart = new JsonObject();
    systemPart.addProperty("text", SYSTEM_PROMPT + templateContext); // ← Inject here!
    // ...
}
```

**System Prompt** giờ bao gồm:
1. **Instructions**: "Bạn là trợ lý AI của PandaDocs..."
2. **Rules**: "CHỈ nói về templates trong CONTEXT..."
3. **Template Data**: "=== DANH SÁCH TEMPLATES CÓ SẴN ===..."

---

### 3. ChatService Integration

File: [`ChatService.java:89-97`](F:/FPTU/PandaProject/api/src/main/java/com/pandadocs/api/service/ChatService.java:89)

```java
// OLD (Wrong ❌):
List<TemplateCardDTO> templates = detectAndSearchTemplates(userMessage);
String aiResponse = geminiService.generateResponse(userMessage, session.getMessages());
// → Gemini KHÔNG biết templates!

// NEW (Correct ✅):
List<TemplateCardDTO> templates = detectAndSearchTemplates(userMessage);
String aiResponse = geminiService.generateResponseWithContext(
    userMessage,
    session.getMessages(),
    templates  // ← Pass template data to Gemini!
);
// → Gemini biết chính xác templates nào có sẵn!
```

---

## System Prompt Rules

File: [`GeminiService.java:45`](F:/FPTU/PandaProject/api/src/main/java/com/pandadocs/api/service/GeminiService.java:45)

```
QUY TẮC BẮT BUỘC:
1. CHỈ nói về các templates được cung cấp trong CONTEXT bên dưới
2. KHÔNG trả lời câu hỏi ngoài lề
3. Nếu người dùng hỏi ngoài chủ đề templates, từ chối lịch sự
4. Khi người dùng muốn mua/thêm template, hỏi XÁC NHẬN trước
5. Luôn thân thiện, ngắn gọn, sử dụng tiếng Việt
6. CHỈ đề cập templates có trong CONTEXT, KHÔNG bịa thêm templates
```

**Rule #6** là quan trọng nhất - đảm bảo Gemini **KHÔNG bịa** templates không tồn tại!

---

## Example Conversation

### Input:
```json
{
  "message": "Tôi cần template presentation về marketing giá dưới 100k"
}
```

### Processing:

**Step 1: Search Templates**
```java
TemplateSearchService.searchTemplates(
    query: "presentation marketing",
    maxPrice: 100000
)
→ Returns: [
    {id: 5, title: "Marketing Presentation", price: 50000},
    {id: 12, title: "Digital Marketing Slides", price: 75000}
]
```

**Step 2: Build Context**
```
=== DANH SÁCH TEMPLATES CÓ SẴN ===

Template #1:
  - ID: 5
  - Tên: Marketing Presentation
  - Mô tả: Professional marketing slides for business
  - Giá: 50,000 VNĐ
  - Danh mục: Presentation
  - Đánh giá: 4.5/5 ⭐
  - Lượt tải: 120

Template #2:
  - ID: 12
  - Tên: Digital Marketing Slides
  - Mô tả: Modern template for digital marketing
  - Giá: 75,000 VNĐ
  - Danh mục: Presentation
  - Đánh giá: 4.2/5 ⭐
  - Lượt tải: 85

=== KẾT THÚC DANH SÁCH ===
```

**Step 3: Gemini Response** (based on context)
```
"Tôi tìm thấy 2 templates presentation về marketing trong khoảng giá của bạn:

1. **Marketing Presentation** - 50,000đ
   - Đánh giá rất tốt (4.5⭐) với 120 lượt tải
   - Thích hợp cho business presentations

2. **Digital Marketing Slides** - 75,000đ
   - Phong cách hiện đại (4.2⭐)
   - Tập trung vào digital marketing

Bạn muốn xem chi tiết template nào?"
```

---

## Benefits of RAG

✅ **Accurate Recommendations**
- Gemini chỉ recommend templates TỒN TẠI trong database
- Không bịa thông tin (no hallucination)

✅ **Up-to-date Information**
- Templates mới được search real-time từ database
- Không cần retrain AI model

✅ **Detailed Responses**
- Gemini biết giá, rating, downloads, category
- Có thể so sánh và giải thích chi tiết

✅ **Flexible**
- Có thể thêm/xóa templates mà không cần update AI
- Search logic có thể improve independently

✅ **Cost Effective**
- Không cần fine-tune model
- Sử dụng generic Gemini model

---

## Limitations & Considerations

### 1. Context Length
- Gemini có giới hạn context window (~1M tokens cho gemini-1.5-flash)
- Hiện tại limit 5 templates per response để tránh overflow
- Description được trim xuống 200 chars

### 2. Search Quality
- RAG chỉ tốt nếu **Retrieval** (search) tốt
- Nếu search sai templates → Gemini recommend sai
- Cần improve `TemplateSearchService` để tăng accuracy

### 3. Latency
- Thêm 1 step (search templates) trước khi call Gemini
- Total latency: ~1-3 seconds (acceptable)

### 4. Cost
- Inject template context → tăng số tokens
- 5 templates ≈ 500-1000 tokens extra
- Still trong free tier limits (1M tokens/minute)

---

## Future Improvements

### 1. Vector Search (Semantic Search)
Thay vì keyword matching, dùng embeddings:

```
User: "Tôi cần template để apply việc"
                ↓
        Convert to Vector
                ↓
    Similarity Search in Vector DB
                ↓
    Find: Resume/CV templates (semantic match!)
```

**Implementation**:
- Use Gemini Embeddings API
- Store embeddings trong PostgreSQL (pgvector extension)
- Cosine similarity search

### 2. Multi-turn Context
Giữ template context qua nhiều turns:

```
User: "Tìm template resume"
AI: "Tôi tìm thấy 3 templates..." [Context: Template A, B, C]

User: "Cái đầu tiên đắt quá, giảm giá được không?"
AI: [Still knows Template A từ previous turn]
```

### 3. Smart Caching
Cache template data để tránh query lại database:

```java
@Cacheable(value = "templateSearch", key = "#query + #maxPrice")
public List<TemplateCardDTO> searchTemplates(String query, Double maxPrice) {
    // ...
}
```

### 4. Hybrid Search
Combine keyword + semantic + popularity:

```
Score = 0.4 * keyword_match
      + 0.3 * semantic_similarity
      + 0.2 * popularity
      + 0.1 * recency
```

---

## Debugging RAG

### Enable Debug Logging

```properties
# application-local.properties
logging.level.com.pandadocs.api.service.GeminiService=DEBUG
```

### Check Context Injection

Look for log:
```
DEBUG GeminiService - Template context:
=== DANH SÁCH TEMPLATES CÓ SẴN ===
Template #1: ...
```

### Verify Gemini Understands

Ask Gemini:
```
User: "Bạn đang thấy những templates nào?"
Gemini: "Tôi đang thấy 3 templates:
         1. Marketing Presentation (50,000đ)
         2. Developer Resume (MIỄN PHÍ)
         ..."
```

---

## References

- **RAG Paper**: https://arxiv.org/abs/2005.11401
- **Gemini API Docs**: https://ai.google.dev/docs
- **Code**: [`GeminiService.java`](F:/FPTU/PandaProject/api/src/main/java/com/pandadocs/api/service/GeminiService.java:1)

---

**TL;DR**: RAG = Tìm kiếm templates từ database → Inject vào prompt → Gemini generate response dựa trên DATA THỰC TẾ thay vì "nói mù"! 🚀
