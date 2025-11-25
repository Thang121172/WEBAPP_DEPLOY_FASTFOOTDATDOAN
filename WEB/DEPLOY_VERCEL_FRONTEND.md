# 🚀 Hướng dẫn Deploy Frontend lên Vercel

## 📋 Tổng quan

- **Frontend:** Deploy lên Vercel ✅
- **Backend:** Giữ trên Render (đã chạy tốt) ✅
- **Kết nối:** Frontend (Vercel) → Backend (Render)

## 🔧 Bước 1: Chuẩn bị Project

### 1.1 Kiểm tra cấu trúc project
```
WEB/
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── src/
│   └── dist/ (sau khi build)
└── backend/ (giữ nguyên, không cần thay đổi)
```

### 1.2 Tạo file `vercel.json` (nếu cần)

File này KHÔNG BẮT BUỘC vì Vercel tự detect Vite. Nhưng nếu muốn customize:

```json
{
  "buildCommand": "cd frontend && npm install && npm run build",
  "outputDirectory": "frontend/dist",
  "devCommand": "cd frontend && npm run dev",
  "installCommand": "cd frontend && npm install",
  "framework": "vite",
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

## 🔧 Bước 2: Deploy lên Vercel

### 2.1 Đăng ký Vercel

1. Vào https://vercel.com/
2. Click **"Sign Up"**
3. Chọn **"Continue with GitHub"**
4. Authorize Vercel để truy cập GitHub

### 2.2 Import Project

1. Sau khi đăng nhập, click **"Add New..."** → **"Project"**
2. Tìm và chọn repo GitHub của bạn (`TEST_WEB_DEPLOY`)
3. Click **"Import"**

### 2.3 Cấu hình Build Settings

Vercel sẽ tự động detect Vite, nhưng bạn cần chỉnh lại:

1. **Framework Preset:** Vite (tự động detect)

2. **Root Directory:** 
   - Click **"Edit"** ở phần "Root Directory"
   - Không cần set (để trống) vì Vercel sẽ build từ root

3. **Build and Output Settings:**
   ```
   Build Command: cd frontend && npm install && npm run build
   Output Directory: frontend/dist
   Install Command: cd frontend && npm install
   ```

4. **Environment Variables:**
   - Click **"Environment Variables"**
   - Thêm biến:
     ```
     Name: VITE_API_BASE
     Value: https://fastfood-backend-t8jz.onrender.com/api
     ```
   - ⚠️ **KHÔNG có dấu `/` ở cuối!**
   - Chọn: **Production, Preview, Development** (tất cả environments)
   - Click **"Save"**

5. **Optional - Custom Domain:**
   - Nếu muốn dùng domain tùy chỉnh, có thể set sau

### 2.4 Deploy

1. Click **"Deploy"** ở góc dưới bên phải
2. Đợi 1-2 phút để Vercel:
   - Clone repo
   - Install dependencies
   - Build project
   - Deploy lên CDN

3. Khi deploy xong, bạn sẽ thấy:
   - ✅ **Status:** "Ready"
   - 🌐 **URL:** `https://your-project.vercel.app`

## 🔧 Bước 3: Kiểm tra CORS trên Render

Frontend đã chuyển sang Vercel, cần update CORS:

1. Vào https://dashboard.render.com/
2. Chọn service **`fastfood-backend-t8jz`**
3. Vào tab **"Environment"**
4. Tìm biến `CORS_ORIGINS`:
   - **Nếu có:** Sửa để thêm URL Vercel
   - **Nếu chưa có:** Thêm mới

5. Set giá trị:
   ```
   CORS_ORIGINS = https://your-project.vercel.app,https://fastfooddatdoan.netlify.app
   ```
   - Thay `your-project.vercel.app` bằng URL Vercel thật của bạn
   - Có thể thêm nhiều origins (phân cách bằng dấu phẩy)

6. Click **"Save Changes"**

## 🔧 Bước 4: Test

### 4.1 Test Frontend

1. Truy cập URL Vercel: `https://your-project.vercel.app`
2. Mở **Developer Tools** (F12) → **Console**
3. Chạy lệnh:
   ```javascript
   console.log('API_BASE:', import.meta.env.VITE_API_BASE);
   ```
   - Phải hiển thị: `https://fastfood-backend-t8jz.onrender.com/api`

### 4.2 Test Đăng ký

1. Vào trang đăng ký: `https://your-project.vercel.app/register`
2. Thử đăng ký
3. Mở **Network** tab (F12) khi click đăng ký:
   - Request URL phải là: `https://fastfood-backend-t8jz.onrender.com/api/accounts/register/request-otp/`
   - Nếu thành công → ✅ Hoàn tất!

## 🔄 Bước 5: Auto-Deploy

Vercel sẽ tự động deploy khi bạn push code lên GitHub:

1. Push code mới lên GitHub:
   ```bash
   git add .
   git commit -m "Update frontend"
   git push origin main
   ```

2. Vercel tự động:
   - Detect commit mới
   - Build lại project
   - Deploy lên production

3. Xem deploy status trên Vercel Dashboard

## 📝 Bước 6: Custom Domain (Optional)

Nếu muốn dùng domain tùy chỉnh:

1. Vào Vercel Dashboard → Project → **Settings** → **Domains**
2. Click **"Add Domain"**
3. Nhập domain của bạn
4. Làm theo hướng dẫn để config DNS

## ✅ Checklist

Sau khi deploy, đảm bảo:

- [ ] Frontend deploy thành công trên Vercel
- [ ] URL Vercel: `https://your-project.vercel.app`
- [ ] `VITE_API_BASE` env var = `https://fastfood-backend-t8jz.onrender.com/api` (không có `/` cuối)
- [ ] `CORS_ORIGINS` trên Render có URL Vercel
- [ ] Test đăng ký thành công
- [ ] Auto-deploy từ GitHub hoạt động

## 🆘 Troubleshooting

### Lỗi: Build failed
- **Nguyên nhân:** Build command sai
- **Giải pháp:** Kiểm tra lại `Build Command` và `Output Directory`

### Lỗi: Environment variable không có hiệu lực
- **Nguyên nhân:** Chưa redeploy sau khi thêm env var
- **Giải pháp:** Vào Deployments → Click "..." → "Redeploy"

### Lỗi: CORS error
- **Nguyên nhân:** CORS_ORIGINS chưa có URL Vercel
- **Giải pháp:** Thêm URL Vercel vào CORS_ORIGINS trên Render

### Lỗi: 404 khi navigate
- **Nguyên nhân:** Thiếu rewrite rules
- **Giải pháp:** Vercel tự động handle SPA routing, nhưng có thể cần thêm `vercel.json`

## 🎯 Kết luận

Sau khi hoàn tất:
- ✅ Frontend chạy trên Vercel
- ✅ Backend chạy trên Render
- ✅ Kết nối Frontend ↔ Backend hoạt động
- ✅ Auto-deploy từ GitHub

**Backend Django giữ nguyên trên Render vì:**
- Render phù hợp với Django hơn
- Backend đã chạy tốt
- Database PostgreSQL đã được setup
- Không cần thay đổi

