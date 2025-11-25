# 🔍 Debug lỗi 404: Backend endpoint không tìm thấy

## ❌ Lỗi hiện tại
```
Failed to load resource: the server responded with a status of 404 ()
```

**Request URL:** `https://fastfood-backend-t8jz.onrender.com/api/accounts/register/request-otp/`

## 🔍 Kiểm tra: Backend endpoint có tồn tại không?

### Endpoint đúng theo code:
- Path: `/api/accounts/register/request-otp/`
- Method: POST
- View: `RegisterRequestOTPView`

### Các khả năng gây lỗi 404:

#### 1. Backend chưa được deploy đúng code mới nhất
**Giải pháp:**
- Kiểm tra code đã được push lên GitHub chưa
- Kiểm tra Render đã deploy code mới nhất chưa

#### 2. URL routing có vấn đề
**Kiểm tra:**
- Backend có đang chạy đúng không
- URL pattern có đúng không

#### 3. Trailing slash issue
Django có thể nhạy cảm với trailing slash. Thử cả 2:
- `/api/accounts/register/request-otp/` (có `/` cuối)
- `/api/accounts/register/request-otp` (không có `/` cuối)

---

## 🧪 Test trực tiếp Backend

### Test 1: Kiểm tra backend có chạy không

Mở browser, truy cập:
```
https://fastfood-backend-t8jz.onrender.com/api/
```

**Phải thấy:** JSON response với các endpoints

### Test 2: Test endpoint đăng ký bằng curl

```bash
curl -X POST https://fastfood-backend-t8jz.onrender.com/api/accounts/register/request-otp/ \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","role":"customer"}'
```

**Kết quả mong đợi:**
- `200 OK` → Endpoint hoạt động
- `404 Not Found` → Endpoint không tồn tại
- `400 Bad Request` → Endpoint tồn tại nhưng data sai
- `500 Internal Server Error` → Backend có lỗi

---

## 🔧 Các bước kiểm tra

### Bước 1: Kiểm tra backend logs trên Render

1. Vào https://dashboard.render.com/
2. Chọn service `fastfood-backend-t8jz`
3. Vào tab **"Logs"**
4. Xem logs gần đây khi có request đến
5. Tìm lỗi 404 hoặc routing error

### Bước 2: Kiểm tra URL trong Frontend

Mở **Network** tab (F12) khi đăng ký, xem:
- **Request URL** đầy đủ là gì?
- **Request Method:** POST
- **Status Code:** 404

### Bước 3: Test endpoint trực tiếp

Dùng Postman, curl, hoặc browser console:

```javascript
fetch('https://fastfood-backend-t8jz.onrender.com/api/accounts/register/request-otp/', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'test@example.com',
    password: 'test123',
    role: 'customer'
  })
})
.then(r => r.json())
.then(data => console.log('✅ Success:', data))
.catch(err => console.error('❌ Error:', err));
```

---

## 🆘 Giải pháp khả thi

### Giải pháp 1: Kiểm tra backend code đã deploy chưa

1. Kiểm tra GitHub có code mới nhất chưa
2. Kiểm tra Render đã auto-deploy chưa
3. Nếu chưa, manual deploy trên Render

### Giải pháp 2: Kiểm tra URL routing

Có thể backend routing chưa đúng. Kiểm tra:
- `backend/core/urls.py` có include `accounts.urls` chưa
- `backend/accounts/urls.py` có path `register/request-otp/` chưa

### Giải pháp 3: Kiểm tra Django settings

Backend có thể chưa load đúng settings. Kiểm tra:
- `DJANGO_SETTINGS_MODULE` trên Render
- `ALLOWED_HOSTS` có đúng không

---

## 📝 Thông tin cần cung cấp

Để debug tốt hơn, cần:
1. **Backend logs** từ Render (khi có request 404)
2. **Request URL đầy đủ** từ Network tab
3. **Response body** của lỗi 404 (nếu có)
4. **Backend đã deploy code mới nhất chưa?**

