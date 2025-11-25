# 🔧 Set Environment Variables trên Vercel

## ✅ Bạn đang dùng: Vercel (Frontend) + Render (Backend)

## 📝 Các bước set Environment Variable trên Vercel

### Bước 1: Vào Vercel Dashboard

1. Truy cập: https://vercel.com/
2. Đăng nhập vào tài khoản của bạn
3. Chọn **project** của bạn (từ danh sách projects)

### Bước 2: Vào Settings → Environment Variables

1. Trong project dashboard, click vào tab **"Settings"** (ở menu trên cùng)
2. Tìm và click vào **"Environment Variables"** (ở menu bên trái, trong phần Settings)

### Bước 3: Thêm Environment Variable

1. Bạn sẽ thấy một bảng với các cột: **Key**, **Value**, **Environments**, **Actions**
2. Ở trên cùng, tìm nút **"Add New"** hoặc **"Add Environment Variable"**
3. Click vào đó

### Bước 4: Điền thông tin

**Trong form xuất hiện:**

1. **Key:** Gõ: `VITE_API_BASE`
2. **Value:** Gõ: `https://fastfood-backend-t8jz.onrender.com/api`
   - ⚠️ **KHÔNG có dấu `/` ở cuối!**
   - ⚠️ **KHÔNG có khoảng trắng!**

3. **Environments:** Chọn tất cả:
   - ✅ Production
   - ✅ Preview  
   - ✅ Development

4. Click **"Save"** hoặc **"Add"**

### Bước 5: Kiểm tra

Sau khi thêm, bạn sẽ thấy trong bảng:
- **Key:** `VITE_API_BASE`
- **Value:** `••••••••` (ẩn vì bảo mật)
- **Environments:** Production, Preview, Development

### Bước 6: Redeploy (QUAN TRỌNG!)

Sau khi set env var, **PHẢI redeploy**:

1. Vào tab **"Deployments"** (ở menu trên cùng)
2. Tìm deployment mới nhất
3. Click vào **"..."** (3 chấm) ở bên phải
4. Chọn **"Redeploy"**
5. Chọn **"Use existing Build Cache"** hoặc **"Redeploy"**
6. Đợi deploy xong (1-2 phút)

---

## ✅ Checklist

- [ ] Đã vào Vercel Dashboard (KHÔNG phải Netlify)
- [ ] Đã vào Settings → Environment Variables
- [ ] Đã thêm `VITE_API_BASE` = `https://fastfood-backend-t8jz.onrender.com/api` (KHÔNG có `/` cuối)
- [ ] Đã chọn tất cả environments (Production, Preview, Development)
- [ ] Đã Save
- [ ] Đã Redeploy project

---

## 🧪 Test sau khi deploy

1. Truy cập URL Vercel của bạn (ví dụ: `https://your-project.vercel.app`)
2. Mở **Console** (F12)
3. Chạy:
   ```javascript
   console.log('API_BASE:', import.meta.env.VITE_API_BASE);
   ```
   - ✅ Phải hiển thị: `https://fastfood-backend-t8jz.onrender.com/api`
   - ❌ Nếu `undefined` → Chưa set env var hoặc chưa redeploy

4. Vào trang đăng ký: `/register`
5. Thử đăng ký
6. Mở **Network** tab (F12) khi click đăng ký
7. Request URL phải là: `https://fastfood-backend-t8jz.onrender.com/api/accounts/register/request-otp/`
   - ✅ Đúng → Kết nối thành công!
   - ❌ Nếu vẫn là `localhost:8000` hoặc `/api/...` → Env var chưa có hiệu lực

---

## 🔄 Update CORS trên Render

Sau khi có URL Vercel:

1. Vào https://dashboard.render.com/
2. Chọn service `fastfood-backend-t8jz`
3. Vào tab **"Environment"**
4. Tìm `CORS_ORIGINS`:
   - Nếu chưa có: Thêm mới
   - Nếu đã có: Sửa để thêm URL Vercel
5. Set giá trị:
   ```
   CORS_ORIGINS = https://your-project.vercel.app
   ```
   - Thay `your-project.vercel.app` bằng URL Vercel thật của bạn
   - KHÔNG có dấu `/` ở cuối!
6. Click **"Save Changes"**

---

## 🆘 Nếu không tìm thấy Environment Variables trên Vercel

### Cách 1: Tìm trong Settings
- Vào Settings → Environment Variables

### Cách 2: Tìm trong Project Settings
- Vào Project → Settings → Environment Variables

### Cách 3: Tìm trong Deploy Settings
- Một số version Vercel có thể đặt ở Deploy Settings → Environment Variables

### Cách 4: Tìm bằng cách tìm kiếm
- Dùng Ctrl + F để tìm "Environment Variables" trên trang

---

## 📸 Vị trí Environment Variables trên Vercel

```
Vercel Dashboard
├── Projects
│   └── Your Project
│       ├── Overview
│       ├── Deployments
│       ├── Settings
│       │   ├── General
│       │   ├── **Environment Variables** ← Ở ĐÂY!
│       │   ├── Git
│       │   └── ...
│       └── ...
```

