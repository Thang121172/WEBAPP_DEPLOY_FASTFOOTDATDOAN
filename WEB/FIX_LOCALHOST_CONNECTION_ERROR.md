# 🔧 Sửa lỗi: Frontend kết nối đến localhost:8000

## ❌ Lỗi hiện tại
```
localhost:8000/api/accounts/register/request-otp/:1 
Failed to load resource: net::ERR_CONNECTION_REFUSED
```

## 🔍 Nguyên nhân
Frontend đang dùng fallback `/api` (từ `vite.config.ts` proxy) thay vì backend URL trên Render.

## ✅ Giải pháp: 2 trường hợp

### Trường hợp 1: Bạn đang chạy Frontend LOCAL (npm run dev)

#### Cách 1: Set Environment Variable khi chạy local

1. Tạo file `.env.local` trong folder `frontend/`:
   ```bash
   cd frontend
   ```
   
2. Tạo file `.env.local`:
   ```env
   VITE_API_BASE=https://fastfood-backend-t8jz.onrender.com/api
   ```
   ⚠️ **KHÔNG có dấu `/` ở cuối!**

3. Restart dev server:
   ```bash
   # Dừng server (Ctrl+C)
   npm run dev
   ```

#### Cách 2: Thêm vào `.gitignore` (nếu chưa có)
Đảm bảo `.env.local` không bị commit:
```
frontend/.env.local
```

### Trường hợp 2: Bạn đã deploy lên Vercel

#### Bước 1: Kiểm tra Environment Variable trên Vercel

1. Vào https://vercel.com/
2. Chọn project của bạn
3. Vào **Settings** → **Environment Variables**
4. Kiểm tra có biến `VITE_API_BASE` chưa:
   - **Nếu chưa có:** Thêm mới
   - **Nếu đã có:** Kiểm tra giá trị

#### Bước 2: Set Environment Variable

1. Click **"Add New"**
2. Điền:
   ```
   Name: VITE_API_BASE
   Value: https://fastfood-backend-t8jz.onrender.com/api
   ```
   ⚠️ **KHÔNG có dấu `/` ở cuối!**

3. Chọn environments: **Production**, **Preview**, **Development**
4. Click **"Save"**

#### Bước 3: Redeploy Vercel

1. Vào tab **"Deployments"**
2. Click **"..."** ở deployment mới nhất
3. Click **"Redeploy"**
4. Đợi deploy xong (1-2 phút)

#### Bước 4: Test lại

1. Mở URL Vercel của bạn
2. Mở Console (F12)
3. Chạy lệnh:
   ```javascript
   console.log('API_BASE:', import.meta.env.VITE_API_BASE);
   ```
   - Phải hiển thị: `https://fastfood-backend-t8jz.onrender.com/api`
   - Nếu `undefined` → Env var chưa có hiệu lực, cần redeploy

---

## 🔍 Kiểm tra: Bạn đang ở đâu?

### Nếu URL là `localhost:5173` hoặc `127.0.0.1:5173`
→ Bạn đang chạy **LOCAL**
→ Cần set `.env.local` (Trường hợp 1)

### Nếu URL là `*.vercel.app` hoặc `*.netlify.app`
→ Bạn đã **DEPLOY**
→ Cần set env var trên platform (Trường hợp 2)

---

## 🧪 Test sau khi sửa

1. Mở browser ở **chế độ incognito**
2. Truy cập frontend URL
3. Mở **Console** (F12)
4. Chạy:
   ```javascript
   console.log('API_BASE:', import.meta.env.VITE_API_BASE);
   ```
5. Vào trang đăng ký: `/register`
6. Mở **Network** tab (F12)
7. Thử đăng ký
8. Xem request URL:
   - ✅ **Phải là:** `https://fastfood-backend-t8jz.onrender.com/api/accounts/register/request-otp/`
   - ❌ **KHÔNG được là:** `localhost:8000/api/...` hoặc `/api/...`

---

## 🆘 Nếu vẫn lỗi

### Lỗi: Environment variable vẫn là `undefined`

**Giải pháp:**
1. Đảm bảo biến bắt đầu bằng `VITE_` (Vercel/Vite requirement)
2. Redeploy lại project
3. Hard refresh browser (Ctrl + Shift + R)

### Lỗi: CORS error

**Giải pháp:**
1. Kiểm tra `CORS_ORIGINS` trên Render
2. Đảm bảo có URL frontend (Vercel hoặc Netlify)
3. Save và restart service trên Render

