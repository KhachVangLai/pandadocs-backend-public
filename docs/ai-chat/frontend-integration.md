# AI Chatbox - Frontend Integration Guide

## 📋 Mục lục

1. [API Endpoints](#api-endpoints)
2. [Request/Response Format](#requestresponse-format)
3. [React/Next.js Integration](#reactnextjs-integration)
4. [State Management](#state-management)
5. [Error Handling](#error-handling)
6. [UI/UX Best Practices](#uiux-best-practices)

---

## 🔌 API Endpoints

### Base URL
```
Development: http://localhost:8080
Production: https://your-api.com
```

### Authentication
Tất cả endpoints yêu cầu JWT token trong header:
```
Authorization: Bearer YOUR_JWT_TOKEN
```

---

### 1. Send Message

**POST** `/api/chat/message`

Gửi message của user và nhận response từ AI.

**Request Body:**
```json
{
  "sessionId": "uuid-string-or-null",
  "message": "Tôi cần template CV cho developer"
}
```

**Response (200 OK):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Tôi tìm thấy 5 template CV chuyên nghiệp phù hợp cho developer...",
  "templates": [
    {
      "id": 47,
      "title": "CV Developer Full-stack",
      "description": "Template CV chuyên nghiệp...",
      "price": 0,
      "previewImage": "https://storage.url/preview.jpg",
      "category": "Resume/CV",
      "rating": 4.8,
      "downloads": 1234
    }
  ],
  "actionButtons": [
    {
      "type": "ADD_TO_LIBRARY",
      "label": "Thêm vào library",
      "templateId": 47
    },
    {
      "type": "VIEW_DETAILS",
      "label": "Xem chi tiết",
      "templateId": 47
    },
    {
      "type": "CANCEL",
      "label": "Hủy",
      "templateId": 47
    }
  ],
  "conversationTitle": "Template CV cho developer"
}
```

**Response (429 Too Many Requests):**
```json
{
  "error": "Rate limit exceeded",
  "message": "Bạn đã vượt quá giới hạn 10 tin nhắn/giờ. Vui lòng thử lại sau.",
  "retryAfter": 3600
}
```

**Response (401 Unauthorized):**
```json
{
  "error": "Unauthorized",
  "message": "JWT token không hợp lệ hoặc đã hết hạn"
}
```

---

### 2. Get Session History

**GET** `/api/chat/session/{sessionId}`

Lấy lịch sử chat của một session.

**Response (200 OK):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "conversationTitle": "Template CV cho developer",
  "messageCount": 6,
  "createdAt": "2025-11-03T10:30:00Z",
  "messages": [
    {
      "role": "USER",
      "content": "Tôi cần template CV",
      "timestamp": "2025-11-03T10:30:00Z"
    },
    {
      "role": "ASSISTANT",
      "content": "Tôi tìm thấy 5 templates...",
      "timestamp": "2025-11-03T10:30:05Z"
    }
  ]
}
```

---

### 3. Clear Session

**DELETE** `/api/chat/session/{sessionId}`

Xóa session hiện tại (clear chat history).

**Response (200 OK):**
```json
{
  "message": "Session cleared successfully"
}
```

---

### 4. Handle Purchase Action

**POST** `/api/chat/purchase-action`

Xử lý khi user click vào action buttons (Buy Now, Add to Library, etc.).

**Request Body:**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "templateId": 47,
  "action": "ADD_TO_LIBRARY"
}
```

**Response - Add to Library:**
```json
{
  "action": "ADD_TO_LIBRARY",
  "templateId": 47,
  "endpoint": "/api/users/library/47",
  "message": "Adding to library..."
}
```

**Response - Buy Now:**
```json
{
  "action": "REDIRECT_TO_PURCHASE",
  "templateId": 47,
  "endpoint": "/api/purchases",
  "message": "Redirecting to checkout..."
}
```

**Response - View Details:**
```json
{
  "action": "VIEW_DETAILS",
  "templateId": 47,
  "url": "/templates/47",
  "message": "Opening template details..."
}
```

---

## 💻 React/Next.js Integration

### 1. Setup API Client

```typescript
// lib/api/chatApi.ts

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

interface SendMessageRequest {
  sessionId: string | null;
  message: string;
}

interface ChatMessageResponse {
  sessionId: string;
  message: string;
  templates: Template[];
  actionButtons: ActionButton[] | null;
  conversationTitle: string | null;
}

interface Template {
  id: number;
  title: string;
  description: string;
  price: number;
  previewImage: string;
  category: string;
  rating: number;
  downloads: number;
}

interface ActionButton {
  type: 'BUY_NOW' | 'ADD_TO_LIBRARY' | 'VIEW_DETAILS' | 'CANCEL';
  label: string;
  templateId: number;
}

// Send message to AI
export async function sendChatMessage(
  request: SendMessageRequest,
  token: string
): Promise<ChatMessageResponse> {
  const response = await fetch(`${API_BASE_URL}/api/chat/message`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    if (response.status === 429) {
      const error = await response.json();
      throw new Error(error.message || 'Rate limit exceeded');
    }
    throw new Error('Failed to send message');
  }

  return response.json();
}

// Get session history
export async function getChatSession(
  sessionId: string,
  token: string
) {
  const response = await fetch(`${API_BASE_URL}/api/chat/session/${sessionId}`, {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error('Failed to get session');
  }

  return response.json();
}

// Clear session
export async function clearChatSession(
  sessionId: string,
  token: string
) {
  const response = await fetch(`${API_BASE_URL}/api/chat/session/${sessionId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  if (!response.ok) {
    throw new Error('Failed to clear session');
  }

  return response.json();
}

// Handle action button click
export async function handlePurchaseAction(
  sessionId: string,
  templateId: number,
  action: string,
  token: string
) {
  const response = await fetch(`${API_BASE_URL}/api/chat/purchase-action`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    body: JSON.stringify({ sessionId, templateId, action }),
  });

  if (!response.ok) {
    throw new Error('Failed to handle action');
  }

  return response.json();
}
```

---

### 2. Chat Component (React)

```typescript
// components/ChatBox.tsx

'use client';

import { useState, useEffect, useRef } from 'react';
import { useAuth } from '@/hooks/useAuth'; // Your auth hook
import { sendChatMessage, handlePurchaseAction } from '@/lib/api/chatApi';

interface Message {
  role: 'USER' | 'ASSISTANT';
  content: string;
  templates?: Template[];
  actionButtons?: ActionButton[];
}

export default function ChatBox() {
  const { user, token } = useAuth();
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto scroll to bottom when new message
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSendMessage = async () => {
    if (!inputMessage.trim() || isLoading) return;

    const userMessage = inputMessage.trim();
    setInputMessage('');
    setError(null);

    // Add user message to UI immediately
    setMessages(prev => [...prev, {
      role: 'USER',
      content: userMessage,
    }]);

    setIsLoading(true);

    try {
      const response = await sendChatMessage(
        {
          sessionId,
          message: userMessage,
        },
        token
      );

      // Update session ID if new session
      if (!sessionId) {
        setSessionId(response.sessionId);
      }

      // Add AI response to UI
      setMessages(prev => [...prev, {
        role: 'ASSISTANT',
        content: response.message,
        templates: response.templates,
        actionButtons: response.actionButtons,
      }]);

    } catch (err: any) {
      setError(err.message || 'Đã có lỗi xảy ra');
      console.error('Chat error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleActionClick = async (
    action: string,
    templateId: number
  ) => {
    if (action === 'CANCEL') {
      return; // Just ignore
    }

    setIsLoading(true);
    try {
      const response = await handlePurchaseAction(
        sessionId!,
        templateId,
        action,
        token
      );

      if (response.action === 'VIEW_DETAILS') {
        // Navigate to template detail page
        window.location.href = response.url;
      } else if (response.action === 'REDIRECT_TO_PURCHASE') {
        // Redirect to purchase flow
        window.location.href = `/checkout?templateId=${templateId}`;
      } else if (response.action === 'ADD_TO_LIBRARY') {
        // Call add to library API
        // Then show success message
        alert('Đã thêm vào library!');
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  return (
    <div className="flex flex-col h-[600px] border rounded-lg shadow-lg bg-white">
      {/* Header */}
      <div className="p-4 border-b bg-blue-600 text-white">
        <h2 className="text-lg font-semibold">AI Trợ lý PandaDocs</h2>
        <p className="text-sm opacity-90">Tìm kiếm template nhanh chóng</p>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.map((msg, index) => (
          <div
            key={index}
            className={`flex ${msg.role === 'USER' ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[80%] rounded-lg p-3 ${
                msg.role === 'USER'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-800'
              }`}
            >
              <p className="whitespace-pre-wrap">{msg.content}</p>

              {/* Template Cards */}
              {msg.templates && msg.templates.length > 0 && (
                <div className="mt-3 space-y-2">
                  {msg.templates.slice(0, 3).map(template => (
                    <div
                      key={template.id}
                      className="bg-white rounded p-2 border"
                    >
                      <div className="flex gap-2">
                        <img
                          src={template.previewImage}
                          alt={template.title}
                          className="w-16 h-16 object-cover rounded"
                        />
                        <div className="flex-1">
                          <h4 className="font-semibold text-sm text-gray-900">
                            {template.title}
                          </h4>
                          <p className="text-xs text-gray-600">
                            {template.price === 0 ? 'MIỄN PHÍ' : `${template.price.toLocaleString()}đ`}
                          </p>
                          <div className="flex items-center gap-1 text-xs text-gray-500">
                            <span>⭐ {template.rating}</span>
                            <span>•</span>
                            <span>{template.downloads} downloads</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Action Buttons */}
              {msg.actionButtons && msg.actionButtons.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2">
                  {msg.actionButtons.map((btn, btnIndex) => (
                    <button
                      key={btnIndex}
                      onClick={() => handleActionClick(btn.type, btn.templateId)}
                      disabled={isLoading}
                      className={`px-3 py-1 rounded text-sm font-medium ${
                        btn.type === 'CANCEL'
                          ? 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                          : 'bg-blue-600 text-white hover:bg-blue-700'
                      } disabled:opacity-50`}
                    >
                      {btn.label}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}

        {/* Loading indicator */}
        {isLoading && (
          <div className="flex justify-start">
            <div className="bg-gray-100 rounded-lg p-3">
              <div className="flex gap-1">
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-100"></div>
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-200"></div>
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Error message */}
      {error && (
        <div className="px-4 py-2 bg-red-50 border-t border-red-200 text-red-600 text-sm">
          {error}
        </div>
      )}

      {/* Input */}
      <div className="p-4 border-t">
        <div className="flex gap-2">
          <input
            type="text"
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Nhập tin nhắn..."
            disabled={isLoading}
            className="flex-1 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
          />
          <button
            onClick={handleSendMessage}
            disabled={isLoading || !inputMessage.trim()}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Gửi
          </button>
        </div>
      </div>
    </div>
  );
}
```

---

### 3. Zustand State Management (Optional)

```typescript
// store/chatStore.ts

import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface Message {
  role: 'USER' | 'ASSISTANT';
  content: string;
  timestamp: string;
  templates?: Template[];
  actionButtons?: ActionButton[];
}

interface ChatState {
  sessionId: string | null;
  messages: Message[];
  conversationTitle: string | null;

  setSessionId: (id: string) => void;
  addMessage: (message: Message) => void;
  clearChat: () => void;
  setConversationTitle: (title: string) => void;
}

export const useChatStore = create<ChatState>()(
  persist(
    (set) => ({
      sessionId: null,
      messages: [],
      conversationTitle: null,

      setSessionId: (id) => set({ sessionId: id }),

      addMessage: (message) => set((state) => ({
        messages: [...state.messages, message],
      })),

      clearChat: () => set({
        sessionId: null,
        messages: [],
        conversationTitle: null,
      }),

      setConversationTitle: (title) => set({ conversationTitle: title }),
    }),
    {
      name: 'chat-storage',
      partialize: (state) => ({
        sessionId: state.sessionId,
        messages: state.messages,
        conversationTitle: state.conversationTitle,
      }),
    }
  )
);
```

---

## 🎨 UI/UX Best Practices

### 1. Loading States

```typescript
// Show typing indicator while AI is thinking
{isLoading && (
  <div className="flex gap-1">
    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" />
    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-100" />
    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-200" />
  </div>
)}
```

### 2. Error Handling

```typescript
// Show error message with retry option
{error && (
  <div className="bg-red-50 border border-red-200 rounded p-3">
    <p className="text-red-600">{error}</p>
    <button
      onClick={() => handleRetry()}
      className="mt-2 text-sm text-red-700 underline"
    >
      Thử lại
    </button>
  </div>
)}
```

### 3. Rate Limit Handling

```typescript
// Show countdown when rate limited
const [rateLimitRetryAfter, setRateLimitRetryAfter] = useState<number | null>(null);

useEffect(() => {
  if (rateLimitRetryAfter && rateLimitRetryAfter > 0) {
    const timer = setInterval(() => {
      setRateLimitRetryAfter(prev => (prev! > 0 ? prev! - 1 : null));
    }, 1000);
    return () => clearInterval(timer);
  }
}, [rateLimitRetryAfter]);

// In UI
{rateLimitRetryAfter && (
  <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
    <p>Bạn đã vượt quá giới hạn tin nhắn.</p>
    <p className="text-sm">Thử lại sau: {rateLimitRetryAfter}s</p>
  </div>
)}
```

### 4. Auto-save Session

```typescript
// Persist session to localStorage
useEffect(() => {
  if (sessionId) {
    localStorage.setItem('chatSessionId', sessionId);
  }
}, [sessionId]);

// Restore session on mount
useEffect(() => {
  const savedSessionId = localStorage.getItem('chatSessionId');
  if (savedSessionId) {
    // Load session history
    getChatSession(savedSessionId, token)
      .then(data => {
        setSessionId(data.sessionId);
        setMessages(data.messages);
      })
      .catch(() => {
        // Session expired, start new
        localStorage.removeItem('chatSessionId');
      });
  }
}, []);
```

### 5. Template Preview Modal

```typescript
// Click on template card to preview
const [selectedTemplate, setSelectedTemplate] = useState<Template | null>(null);

<TemplatePreviewModal
  template={selectedTemplate}
  onClose={() => setSelectedTemplate(null)}
  onAddToLibrary={(id) => handleActionClick('ADD_TO_LIBRARY', id)}
  onBuyNow={(id) => handleActionClick('BUY_NOW', id)}
/>
```

---

## 🧪 Testing Examples

### 1. Unit Test (Jest + React Testing Library)

```typescript
// __tests__/ChatBox.test.tsx

import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ChatBox from '@/components/ChatBox';
import { sendChatMessage } from '@/lib/api/chatApi';

jest.mock('@/lib/api/chatApi');

describe('ChatBox', () => {
  it('sends message and displays AI response', async () => {
    const mockResponse = {
      sessionId: 'test-session',
      message: 'Tôi tìm thấy 5 templates',
      templates: [],
      actionButtons: null,
      conversationTitle: 'Test',
    };

    (sendChatMessage as jest.Mock).mockResolvedValue(mockResponse);

    render(<ChatBox />);

    const input = screen.getByPlaceholderText('Nhập tin nhắn...');
    const sendButton = screen.getByText('Gửi');

    fireEvent.change(input, { target: { value: 'Tìm template CV' } });
    fireEvent.click(sendButton);

    await waitFor(() => {
      expect(screen.getByText('Tôi tìm thấy 5 templates')).toBeInTheDocument();
    });
  });

  it('handles rate limit error', async () => {
    (sendChatMessage as jest.Mock).mockRejectedValue(
      new Error('Rate limit exceeded')
    );

    render(<ChatBox />);

    // Test rate limit handling
  });
});
```

---

## 📱 Mobile Responsive

```typescript
// Mobile-optimized chat box
<div className="flex flex-col h-screen md:h-[600px] md:rounded-lg">
  {/* Header */}
  <div className="sticky top-0 z-10 p-4 border-b bg-blue-600">
    <button
      onClick={() => setIsChatOpen(false)}
      className="md:hidden mb-2"
    >
      ← Back
    </button>
    <h2>AI Trợ lý</h2>
  </div>

  {/* Messages - full height on mobile */}
  <div className="flex-1 overflow-y-auto p-4">
    {/* Messages */}
  </div>

  {/* Input - fixed at bottom on mobile */}
  <div className="sticky bottom-0 p-4 border-t bg-white">
    {/* Input */}
  </div>
</div>
```

---

## 🚀 Performance Optimization

### 1. Debounce typing indicator

```typescript
const [isTyping, setIsTyping] = useState(false);
const typingTimeoutRef = useRef<NodeJS.Timeout>();

const handleInputChange = (value: string) => {
  setInputMessage(value);

  // Clear previous timeout
  if (typingTimeoutRef.current) {
    clearTimeout(typingTimeoutRef.current);
  }

  // Set typing indicator
  setIsTyping(true);

  // Clear typing after 1s
  typingTimeoutRef.current = setTimeout(() => {
    setIsTyping(false);
  }, 1000);
};
```

### 2. Lazy load template images

```typescript
<img
  src={template.previewImage}
  alt={template.title}
  loading="lazy"
  className="w-16 h-16 object-cover rounded"
/>
```

### 3. Virtual scrolling for long chat history

```typescript
import { useVirtualizer } from '@tanstack/react-virtual';

const parentRef = useRef<HTMLDivElement>(null);

const virtualizer = useVirtualizer({
  count: messages.length,
  getScrollElement: () => parentRef.current,
  estimateSize: () => 100,
});
```

---

## 🔐 Security Considerations

1. **Always validate JWT token** before making API calls
2. **Sanitize user input** before display (XSS prevention)
3. **Don't store JWT in localStorage** (use httpOnly cookies if possible)
4. **Rate limit on frontend** (disable send button after limit reached)
5. **Validate all API responses** before rendering

---

## 📚 Complete Example

See working example at: `/examples/chat-integration-example.tsx`

---

## 🆘 Troubleshooting

### Issue 1: CORS errors
**Solution**: Ensure backend allows your frontend origin in CORS config

### Issue 2: 401 Unauthorized
**Solution**: Check JWT token is valid and not expired

### Issue 3: Messages not persisting
**Solution**: Ensure sessionId is being stored and passed in subsequent requests

### Issue 4: Slow responses
**Solution**: Show loading indicator, consider websockets for streaming

---

## 📞 Support

Questions? Check:
- [setup.md](setup.md) - Backend setup
- [flow.md](flow.md) - How RAG works
- [debug-guide.md](debug-guide.md) - Debugging guide
