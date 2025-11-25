# 🔧 Hướng dẫn sửa lỗi "Lỗi kết nối máy chủ"

## ❌ Vấn đề
Khi đăng ký, hiển thị lỗi: **"Lỗi kết nối máy chủ"**

## 🔍 Nguyên nhân
Frontend (Netlify) không thể kết nối đến Backend (Render). Có thể do:
1. **Biến môi trường `VITE_API_BASE` chưa được set trên Netlify**
2. **Backend URL không đúng**
3. **CORS chưa được cấu hình đúng**

## ✅ Giải pháp từng bước

### Bước 1: Kiểm tra Backend URL trên Render

1. Vào https://dashboard.render.com/
2. Chọn service **`fastfood-backend`**
3. Copy **URL** của service (ví dụ: `https://fastfood-backend-xxxx.onrender.com`)
4. Thêm `/api` vào cuối: `https://fastfood-backend-xxxx.onrender.com/api`

### Bước 2: Kiểm tra biến môi trường trên Netlify

1. Vào https://app.netlify.com/
2. Chọn site **`fastfooddatdoan`** (hoặc tên site của bạn)
3. Vào **Site settings** → **Environment variables**
4. Kiểm tra biến `VITE_API_BASE`:
   - **Phải có giá trị**: `https://fastfood-backend-xxxx.onrender.com/api`
   - **KHÔNG có dấu `/` ở cuối** (ví dụ: `https://fastfood-backend-xxxx.onrender.com/api` ✅, không phải `https://fastfood-backend-xxxx.onrender.com/api/` ❌)

### Bước 3: Nếu chưa có biến `VITE_API_BASE`, thêm mới

1. Trong **Environment variables** của Netlify
2. Click **"Add a variable"**
3. Điền:
   - **Key**: `VITE_API_BASE`
   - **Value**: `https://fastfood-backend-xxxx.onrender.com/api` (thay `xxxx` bằng URL thật của bạn)
4. Click **"Save"**

### Bước 4: Redeploy Netlify

**QUAN TRỌNG:** Sau khi thay đổi environment variables, **PHẢI redeploy**:

1. Vào **Deploys** tab trên Netlify
2. Click **"Trigger deploy"** → **"Clear cache and deploy site"**
3. Đợi deploy xong (1-2 phút)

### Bước 5: Kiểm tra Backend có chạy không

1. Mở trình duyệt
2. Truy cập: `https://fastfood-backend-xxxx.onrender.com/api/` (URL backend + `/api/`)
3. **Phải thấy response JSON** (ví dụ: `{"orders":"...", "merchant":"..."}`)
4. Nếu lỗi 404 hoặc không load được → Backend chưa chạy, cần kiểm tra lại

### Bước 6: Kiểm tra CORS trên Render

1. Vào Render dashboard
2. Chọn service **`fastfood-backend`**
3. Vào **Environment** tab
4. Kiểm tra biến `CORS_ORIGINS`:
   - **Phải có giá trị**: `https://fastfooddatdoan.netlify.app` (URL frontend Netlify)
   - **KHÔNG có dấu `/` ở cuối** (ví dụ: `https://fastfooddatdoan.netlify.app` ✅, không phải `https://fastfooddatdoan.netlify.app/` ❌)

### Bước 7: Test lại

1. Mở trình duyệt ở **chế độ incognito** (để tránh cache)
2. Truy cập: `https://fastfooddatdoan.netlify.app/register`
3. Thử đăng ký lại
4. Nếu vẫn lỗi, mở **Developer Tools** (F12) → **Console** tab để xem lỗi chi tiết

## 🔍 Debug trong Browser Console

Nếu vẫn lỗi, mở **Console** (F12) và kiểm tra:

1. **Network tab**: Xem request đến backend có fail không
   - URL request phải là: `https://fastfood-backend-xxxx.onrender.com/api/accounts/register/request-otp/`
   - Nếu là `/api/accounts/register/request-otp/` (relative) → `VITE_API_BASE` chưa được set đúng

2. **Console tab**: Xem có lỗi CORS không
   - Nếu thấy `CORS policy: No 'Access-Control-Allow-Origin'` → CORS chưa được cấu hình đúng

## ✅ Checklist

Trước khi test lại, đảm bảo:

- [ ] Backend URL trên Render: `https://fastfood-backend-xxxx.onrender.com`
- [ ] `VITE_API_BASE` trên Netlify: `https://fastfood-backend-xxxx.onrender.com/api` (KHÔNG có `/` cuối)
- [ ] `CORS_ORIGINS` trên Render: `https://fastfooddatdoan.netlify.app` (KHÔNG có `/` cuối)
- [ ] Đã redeploy Netlify sau khi thay đổi env vars
- [ ] Backend đang chạy (test URL backend trong browser)

## 🆘 Nếu vẫn lỗi

Gửi cho tôi:
1. Screenshot của **Console** tab (F12)
2. Screenshot của **Network** tab (F12) khi click đăng ký
3. Giá trị của `VITE_API_BASE` trên Netlify
4. URL backend trên Render

