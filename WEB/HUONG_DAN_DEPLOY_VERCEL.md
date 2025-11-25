# 🚀 Hướng dẫn Deploy Frontend lên Vercel (Chi tiết)

## 📋 Kiến trúc Deployment

- **Frontend (React/Vite):** Vercel ✅
- **Backend (Django):** Render (giữ nguyên) ✅
- **Database (PostgreSQL):** Render (giữ nguyên) ✅

## ✅ File đã được tạo/cập nhật

1. ✅ `vercel.json` - Cấu hình Vercel (đã update)
2. ✅ `DEPLOY_VERCEL_FRONTEND.md` - Hướng dẫn chi tiết

## 🔧 Bước 1: Commit và Push vercel.json

```bash
git add vercel.json
git commit -m "Add Vercel configuration"
git push origin main
```

## 🔧 Bước 2: Đăng ký Vercel

1. Vào https://vercel.com/
2. Click **"Sign Up"**
3. Chọn **"Continue with GitHub"**
4. Authorize Vercel để truy cập GitHub repositories

## 🔧 Bước 3: Deploy Project

### 3.1 Import Project

1. Sau khi đăng nhập, click **"Add New..."** → **"Project"**
2. Tìm repo **`TEST_WEB_DEPLOY`** của bạn
3. Click **"Import"**

### 3.2 Cấu hình Build Settings

Vercel sẽ tự động detect `vercel.json`, nhưng bạn vẫn nên kiểm tra:

1. **Framework Preset:** Vite (tự động)

2. **Root Directory:** 
   - Để trống (root của repo)

3. **Build and Output Settings:**
   - Vercel sẽ đọc từ `vercel.json`:
     ```
     Build Command: cd frontend && npm install && npm run build
     Output Directory: frontend/dist
     ```
   - **KHÔNG CẦN SỬA** nếu đã có `vercel.json`

4. **Environment Variables:**
   - Click **"Environment Variables"**
   - Thêm biến:
     ```
     Name: VITE_API_BASE
     Value: https://fastfood-backend-t8jz.onrender.com/api
     ```
   - ⚠️ **KHÔNG có dấu `/` ở cuối!**
   - Chọn tất cả environments: **Production**, **Preview**, **Development**
   - Click **"Add"**

5. **Optional - Mapbox Token (nếu có):**
   ```
   Name: VITE_MAPBOX_TOKEN
   Value: (token của bạn nếu có)
   ```

### 3.3 Deploy

1. Click **"Deploy"** ở góc dưới bên phải
2. Đợi 1-2 phút
3. Khi deploy xong, bạn sẽ thấy URL: `https://your-project-name.vercel.app`

## 🔧 Bước 4: Update CORS trên Render

Frontend đã chuyển sang Vercel, cần update CORS:

1. Vào https://dashboard.render.com/
2. Chọn service **`fastfood-backend-t8jz`**
3. Vào tab **"Environment"**
4. Tìm biến `CORS_ORIGINS`:
   - **Nếu đã có:** Thêm URL Vercel vào (phân cách bằng dấu phẩy)
   - **Nếu chưa có:** Thêm mới

5. Set giá trị:
   ```
   CORS_ORIGINS = https://your-project-name.vercel.app,https://fastfooddatdoan.netlify.app
   ```
   - Thay `your-project-name.vercel.app` bằng URL Vercel thật của bạn
   - Giữ lại Netlify URL nếu muốn (hoặc xóa nếu không dùng nữa)

6. Click **"Save Changes"**

## 🔧 Bước 5: Test

### 5.1 Test Frontend URL

1. Truy cập URL Vercel của bạn
2. Phải thấy trang web load bình thường

### 5.2 Test Environment Variable

1. Mở **Developer Tools** (F12) → **Console**
2. Chạy lệnh:
   ```javascript
   console.log('API_BASE:', import.meta.env.VITE_API_BASE);
   ```
   - Phải hiển thị: `https://fastfood-backend-t8jz.onrender.com/api`

### 5.3 Test Đăng ký

1. Vào trang đăng ký: `/register`
2. Mở **Network** tab (F12)
3. Thử đăng ký với email/password
4. Xem request:
   - **URL phải là:** `https://fastfood-backend-t8jz.onrender.com/api/accounts/register/request-otp/`
   - **Status:** 200 hoặc 400 (tùy vào dữ liệu input)

### 5.4 Test CORS

Nếu thấy lỗi CORS trong Console:
- Kiểm tra lại `CORS_ORIGINS` trên Render
- Đảm bảo URL Vercel được thêm vào
- Redeploy Render nếu cần

## 🔄 Bước 6: Auto-Deploy Setup

Vercel tự động deploy khi push code:

1. Push code lên GitHub:
   ```bash
   git add .
   git commit -m "Update code"
   git push origin main
   ```

2. Vercel sẽ tự động:
   - Detect commit mới
   - Build lại project
   - Deploy lên production
   - Cập nhật URL

3. Xem deploy status:
   - Vào Vercel Dashboard
   - Tab **"Deployments"**
   - Xem trạng thái deploy

## 📝 Bước 7: Custom Domain (Optional)

Nếu muốn dùng domain tùy chỉnh:

1. Vào Vercel Dashboard → Project → **Settings** → **Domains**
2. Click **"Add Domain"**
3. Nhập domain của bạn (ví dụ: `fastfood.com`)
4. Làm theo hướng dẫn để config DNS records

## ✅ Checklist cuối cùng

Sau khi deploy, đảm bảo:

- [ ] `vercel.json` đã được commit và push lên GitHub
- [ ] Project đã được import vào Vercel
- [ ] Environment variable `VITE_API_BASE` đã được set
- [ ] Deploy thành công trên Vercel
- [ ] URL Vercel: `https://your-project-name.vercel.app`
- [ ] `CORS_ORIGINS` trên Render có URL Vercel
- [ ] Test đăng ký thành công (không còn lỗi "Lỗi kết nối máy chủ")
- [ ] Auto-deploy từ GitHub hoạt động

## 🆘 Troubleshooting

### Lỗi: Build failed - "Cannot find module"

**Nguyên nhân:** Build command không đúng  
**Giải pháp:** Kiểm tra `vercel.json`, đảm bảo có `cd frontend` trong build command

### Lỗi: 404 khi navigate

**Nguyên nhân:** Thiếu rewrite rules  
**Giải pháp:** Kiểm tra `vercel.json` có phần `rewrites` chưa

### Lỗi: Environment variable không có hiệu lực

**Nguyên nhân:** Chưa redeploy sau khi thêm env var  
**Giải pháp:** 
1. Vào Vercel Dashboard → Deployments
2. Click "..." ở deployment mới nhất
3. Click "Redeploy"

### Lỗi: CORS error

**Nguyên nhân:** CORS_ORIGINS chưa có URL Vercel  
**Giải pháp:** 
1. Thêm URL Vercel vào `CORS_ORIGINS` trên Render
2. Save Changes
3. Restart service nếu cần

## 🎯 Tóm tắt

1. ✅ **Commit vercel.json** lên GitHub
2. ✅ **Import project** vào Vercel
3. ✅ **Set environment variable** `VITE_API_BASE`
4. ✅ **Deploy** project
5. ✅ **Update CORS** trên Render
6. ✅ **Test** đăng ký

**Backend Django giữ nguyên trên Render vì:**
- ✅ Render phù hợp với Django hơn Vercel
- ✅ Backend đã chạy tốt
- ✅ Database PostgreSQL đã được setup
- ✅ Không cần thay đổi gì

