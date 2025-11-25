# 🧪 Test Backend Endpoint

## Bước 1: Kiểm tra Backend có chạy không

Mở browser, truy cập:
```
https://fastfood-backend-t8jz.onrender.com/api/
```

**Phải thấy:** JSON response với các endpoints như:
```json
{
  "orders": "...",
  "merchant": "...",
  ...
}
```

## Bước 2: Test endpoint đăng ký bằng Browser Console

1. Mở browser, truy cập bất kỳ trang nào
2. Mở **Console** (F12)
3. Copy và paste code sau:

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
.then(async r => {
  const text = await r.text();
  console.log('Status:', r.status);
  console.log('Response:', text);
  return JSON.parse(text);
})
.then(data => console.log('✅ Success:', data))
.catch(err => console.error('❌ Error:', err));
```

**Kết quả mong đợi:**
- **200 OK** → Endpoint hoạt động ✅
- **404 Not Found** → Endpoint không tồn tại ❌
- **400 Bad Request** → Endpoint hoạt động nhưng data sai (OK, có nghĩa là endpoint tồn tại)
- **500 Internal Server Error** → Backend có lỗi

## Bước 3: Kiểm tra Backend Logs trên Render

1. Vào https://dashboard.render.com/
2. Chọn service `fastfood-backend-t8jz`
3. Vào tab **"Logs"**
4. Xem logs gần đây
5. Tìm dòng có chứa `register/request-otp` hoặc `404`

## Bước 4: Kiểm tra Backend Code đã deploy chưa

1. Vào GitHub repo của bạn
2. Kiểm tra file `backend/accounts/urls.py` có dòng này chưa:
   ```python
   path('register/request-otp/', RegisterRequestOTPView.as_view(), name='register_request_otp'),
   ```

3. Kiểm tra Render đã deploy commit mới nhất chưa:
   - Vào Render → service → tab **"Events"**
   - Xem commit hash có trùng với GitHub không

