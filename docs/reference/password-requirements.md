# Password Requirements

## Yêu cầu mật khẩu mạnh

Từ phiên bản hiện tại, hệ thống đã áp dụng **validation mật khẩu mạnh** để tăng cường bảo mật.

### Quy tắc mật khẩu

Mật khẩu phải đáp ứng **TẤT CẢ** các yêu cầu sau:

1. ✅ **Tối thiểu 8 ký tự**
2. ✅ **Ít nhất 1 chữ thường** (a-z)
3. ✅ **Ít nhất 1 chữ hoa** (A-Z)
4. ✅ **Ít nhất 1 chữ số** (0-9)
5. ✅ **Ít nhất 1 ký tự đặc biệt** từ danh sách: `@$!%*?&`

### Ví dụ

#### ✅ Mật khẩu hợp lệ:
- `Password123!`
- `MyP@ssw0rd`
- `Secure$Pass1`
- `Admin@2024`

#### ❌ Mật khẩu KHÔNG hợp lệ:
- `password` - thiếu chữ hoa, số, ký tự đặc biệt
- `PASSWORD123!` - thiếu chữ thường
- `Password` - thiếu số và ký tự đặc biệt
- `Pass1!` - quá ngắn (< 8 ký tự)
- `Password123` - thiếu ký tự đặc biệt

### API Endpoints áp dụng

Validation này được áp dụng cho các endpoints:

1. **POST /api/auth/signup** - Đăng ký tài khoản mới
   ```json
   {
     "username": "john_doe",
     "email": "john@example.com",
     "password": "SecurePass123!"
   }
   ```

2. **POST /api/auth/reset-password** - Reset mật khẩu
   ```json
   {
     "token": "reset-token-here",
     "newPassword": "NewSecure123!"
   }
   ```

### Response lỗi

Khi mật khẩu không đạt yêu cầu, API trả về HTTP 400 Bad Request với message cụ thể:

```json
{
  "password": "Password must contain at least one uppercase letter"
}
```

Các message lỗi có thể có:
- `"Password must be at least 8 characters long"`
- `"Password must contain at least one lowercase letter"`
- `"Password must contain at least one uppercase letter"`
- `"Password must contain at least one number"`
- `"Password must contain at least one special character (@$!%*?&)"`

### Frontend Implementation

Frontend nên implement real-time validation để UX tốt hơn:

```javascript
// Regex pattern (giống backend)
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;

function validatePassword(password) {
  if (password.length < 8) {
    return "Mật khẩu phải có ít nhất 8 ký tự";
  }
  if (!/[a-z]/.test(password)) {
    return "Mật khẩu phải chứa ít nhất 1 chữ thường";
  }
  if (!/[A-Z]/.test(password)) {
    return "Mật khẩu phải chứa ít nhất 1 chữ hoa";
  }
  if (!/\d/.test(password)) {
    return "Mật khẩu phải chứa ít nhất 1 chữ số";
  }
  if (!/[@$!%*?&]/.test(password)) {
    return "Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt (@$!%*?&)";
  }
  return null; // Valid
}
```

### Security Notes

- Mật khẩu được mã hóa bằng BCrypt trước khi lưu vào database
- Mật khẩu **KHÔNG BAO GIỜ** được trả về trong API response
- Token reset password có thời hạn 1 giờ
- Không giới hạn độ dài tối đa (nhưng recommend < 128 ký tự)
