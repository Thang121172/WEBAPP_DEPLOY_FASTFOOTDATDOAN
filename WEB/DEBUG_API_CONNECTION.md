# 🐛 Debug: Kiểm tra kết nối API trong Browser

## Cách kiểm tra nhanh

### Bước 1: Mở trang đăng ký
```
https://fastfooddatdoan.netlify.app/register
```

### Bước 2: Mở Developer Tools
- Nhấn **F12** hoặc **Ctrl + Shift + I**
- Chọn tab **Console**

### Bước 3: Chạy lệnh này trong Console

```javascript
// Kiểm tra biến môi trường VITE_API_BASE
console.log('API_BASE:', import.meta.env.VITE_API_BASE);

// Nếu undefined → env var chưa được set
// Nếu có giá trị → sẽ hiển thị URL backend
```

### Bước 4: Test kết nối trực tiếp

```javascript
// Test xem backend có chạy không
fetch('https://fastfood-backend-t8jz.onrender.com/api/')
  .then(r => r.json())
  .then(data => console.log('✅ Backend OK:', data))
  .catch(err => console.error('❌ Backend ERROR:', err));
```

**Lưu ý:** Thay `https://fastfood-backend-t8jz.onrender.com` bằng URL backend thật của bạn!

### Bước 5: Test đăng ký API

```javascript
// Test API đăng ký (sẽ fail nhưng sẽ thấy lỗi gì)
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
  .then(data => console.log('✅ API OK:', data))
  .catch(err => console.error('❌ API ERROR:', err));
```

## Các lỗi thường gặp

### 1. `VITE_API_BASE` = undefined
**Nguyên nhân:** Biến môi trường chưa được set trên Netlify  
**Giải pháp:** Thêm `VITE_API_BASE` vào Netlify env vars và redeploy

### 2. CORS error
**Lỗi:** `Access to fetch at '...' from origin '...' has been blocked by CORS policy`  
**Nguyên nhân:** CORS chưa được cấu hình đúng trên backend  
**Giải pháp:** Kiểm tra `CORS_ORIGINS` trên Render

### 3. Network error / Failed to fetch
**Nguyên nhân:** Backend không chạy hoặc URL sai  
**Giải pháp:** Kiểm tra backend URL trên Render

### 4. 404 Not Found
**Nguyên nhân:** URL backend sai hoặc route không tồn tại  
**Giải pháp:** Kiểm tra URL có `/api` ở cuối không

