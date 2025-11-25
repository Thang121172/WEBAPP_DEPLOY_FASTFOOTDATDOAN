# 🚀 HƯỚNG DẪN DEPLOY LÊN NETLIFY

## 🎯 **MỤC TIÊU:**
- ✅ Deploy frontend React/Vite lên Netlify
- ✅ Cấu hình biến môi trường (API backend URL)
- ✅ Cấu hình routing cho Single Page Application (SPA)
- ✅ Tự động deploy khi push code lên Git

---

## 📋 **BƯỚC 1: CHUẨN BỊ PROJECT**

### **1.1. Đảm bảo build command hoạt động:**

```powershell
cd frontend
npm run build
```

✅ Kiểm tra folder `frontend/dist` đã được tạo thành công.

---

## 📋 **BƯỚC 2: TẠO FILE CẤU HÌNH NETLIFY**

### **2.1. Tạo file `netlify.toml` trong thư mục `frontend/`**

File này đã được tạo sẵn với các cấu hình:
- Build command: `npm run build`
- Publish directory: `dist`
- Redirect rules cho SPA routing

---

## 📋 **BƯỚC 3: ĐĂNG KÝ VÀ TẠO SITE TRÊN NETLIFY**

### **3.1. Truy cập Netlify:**
- 🌐 Website: https://www.netlify.com
- Đăng nhập bằng GitHub/GitLab/Bitbucket account

### **3.2. Bạn đang ở trang Projects - Chọn một trong 2 cách:**

#### **🔄 CÁCH A: Sử dụng project hiện có (nếu đã tạo)**

1. **Click vào project** (ví dụ: "whimsical-licorice-884129") để vào trang chi tiết
2. Vào **"Site settings"** (icon bánh răng ⚙️ ở menu trên)
3. Vào **"Build & deploy"** → **"Build settings"**
4. **Cấu hình ĐÚNG:**
   - **Base directory:** `frontend`
   - **Build command:** `npm run build`
   - **Publish directory:** `dist` ⚠️ **CHỈ CÓ "dist", KHÔNG CÓ "frontend/"!**
   
   **Nếu không xóa được "frontend/" trong Publish directory:**
   - Chọn toàn bộ text trong ô "Publish directory" (Ctrl+A)
   - Xóa hết (Delete hoặc Backspace)
   - Gõ lại: `dist` (chỉ 4 ký tự)
   - Hoặc xem hướng dẫn chi tiết trong file `NETLIFY_FIX_PUBLISH_DIRECTORY.md`
5. Click **"Save"**

#### **✨ CÁCH B: Tạo project mới (KHUYÊN DÙNG)**

1. Click nút **"Add new project"** (màu xanh lá, góc trên bên phải)
2. Chọn **"Import an existing project"**
3. Chọn Git provider (GitHub/GitLab/Bitbucket) và authorize nếu cần
4. Chọn repository của bạn từ danh sách
5. **Cấu hình Build settings:**
   - **Base directory:** `frontend` ⚠️ **QUAN TRỌNG!**
   - **Build command:** `npm run build`
   - **Publish directory:** `dist`
6. Click **"Deploy site"**

#### **📦 CÁCH C: Deploy thủ công (Manual) - Nhanh để test**

1. **Trước tiên, build project trên máy local:**
   ```powershell
   cd frontend
   npm run build
   ```
2. Trên Netlify, kéo thả folder `frontend/dist` vào vùng **"Drag and drop your project folder here"**
3. Site sẽ được deploy ngay (nhưng không tự động update khi push code)

⚠️ **LƯU Ý:** Cách C chỉ để test nhanh. Nên dùng **Cách B** để có auto-deploy từ Git!

---

## 📋 **BƯỚC 4: CẤU HÌNH BIẾN MÔI TRƯỜNG**

### **4.1. Vào Site settings:**
- Vào site của bạn trên Netlify
- Click **"Site settings"** (hoặc **"Site configuration"**)

### **4.2. Thêm Environment Variables:**
- Vào **"Environment variables"**
- Click **"Add a variable"**
- Thêm biến: `VITE_API_BASE`

**Ví dụ:**
```
Variable name: VITE_API_BASE
Value: https://your-backend-api.com/api
```

**Lưu ý:**
- ✅ Nếu backend chạy trên VPS: `https://103.75.182.180:8000/api` hoặc domain của bạn
- ✅ Nếu backend chạy trên Render/Heroku: URL của backend service
- ✅ Không có `/` ở cuối URL (trừ khi cần)

### **4.3. Redeploy sau khi thêm biến môi trường:**
- Vào **"Deploys"**
- Click **"Trigger deploy"** → **"Clear cache and deploy site"**

---

## 📋 **BƯỚC 5: CẤU HÌNH CUSTOM DOMAIN (TÙY CHỌN)**

### **5.1. Thêm domain:**
1. Vào **"Domain settings"**
2. Click **"Add custom domain"**
3. Nhập domain của bạn (ví dụ: `fastfood.yourdomain.com`)
4. Làm theo hướng dẫn để cấu hình DNS:
   - Thêm CNAME record: `fastfood.yourdomain.com` → `your-site-name.netlify.app`

### **5.2. SSL tự động:**
✅ Netlify sẽ tự động cấp SSL certificate (HTTPS) cho domain của bạn.

---

## 📋 **BƯỚC 6: KIỂM TRA DEPLOY**

### **6.1. Kiểm tra build logs:**
- Vào **"Deploys"** tab
- Click vào deploy mới nhất
- Xem build logs để đảm bảo không có lỗi

### **6.2. Test trên trình duyệt:**
- Mở URL site: `https://your-site-name.netlify.app`
- Kiểm tra:
  - ✅ Trang chủ load được
  - ✅ Routing hoạt động (thử navigate giữa các trang)
  - ✅ API calls hoạt động (kiểm tra Network tab trong DevTools)

---

## 📋 **BƯỚC 7: CẤU HÌNH CORS TRÊN BACKEND**

### **⚠️ QUAN TRỌNG:** Backend cần cho phép CORS từ domain Netlify!

### **7.1. Cấu hình CORS trên Django backend:**

Tìm file settings của Django backend và thêm:

```python
CORS_ALLOWED_ORIGINS = [
    "https://your-site-name.netlify.app",
    "https://your-custom-domain.com",  # nếu có
]

# Hoặc cho phép tất cả (CHỈ DÙNG CHO DEVELOPMENT):
# CORS_ALLOW_ALL_ORIGINS = True
```

### **7.2. Nếu dùng middleware CORS:**

```python
MIDDLEWARE = [
    # ... các middleware khác
    'corsheaders.middleware.CorsMiddleware',
    'django.middleware.common.CommonMiddleware',
    # ...
]
```

---

## 🔧 **TROUBLESHOOTING**

### **❌ Lỗi: "Page not found" khi refresh trang:**
✅ **Giải pháp:** Đã cấu hình redirect rules trong `netlify.toml`

### **❌ Lỗi: API calls bị CORS block:**
✅ **Giải pháp:** Cấu hình CORS trên backend (xem Bước 7)

### **❌ Lỗi: Build failed - "Cannot find module"**
✅ **Giải pháp:** 
- Kiểm tra `package.json` có đầy đủ dependencies
- Chạy `npm install` trước khi deploy
- Đảm bảo Base directory là `frontend`

### **❌ Lỗi: Environment variables không hoạt động:**
✅ **Giải pháp:**
- Biến môi trường phải bắt đầu với `VITE_` để Vite nhận diện
- Redeploy sau khi thêm biến môi trường
- Kiểm tra tên biến trong code: `import.meta.env.VITE_API_BASE`

### **❌ Lỗi: "404 Not Found" cho assets (CSS/JS)**
✅ **Giải pháp:**
- Kiểm tra `vite.config.ts` có cấu hình `base` path không
- Nếu deploy ở subdirectory, thêm:
```typescript
export default defineConfig({
  base: '/your-subdirectory/',
  // ...
})
```

---

## 📝 **TÓM TẮT CÁC BƯỚC:**

1. ✅ Tạo file `netlify.toml` trong `frontend/` (đã có sẵn)
2. ✅ Push code lên Git repository
3. ✅ Đăng nhập Netlify và tạo site mới từ Git
4. ✅ Cấu hình Base directory: `frontend`
5. ✅ Thêm Environment variable: `VITE_API_BASE`
6. ✅ Cấu hình CORS trên backend
7. ✅ Deploy và test!

---

## 🔗 **LIÊN KẾT HỮU ÍCH:**

- 📖 [Netlify Documentation](https://docs.netlify.com/)
- 📖 [Vite Environment Variables](https://vitejs.dev/guide/env-and-mode.html)
- 📖 [Netlify Redirect Rules](https://docs.netlify.com/routing/redirects/)

---

## ✅ **SAU KHI DEPLOY THÀNH CÔNG:**

- 🌐 Frontend URL: `https://your-site-name.netlify.app`
- 🔧 Backend API: `https://your-backend-url.com/api`
- 🚀 Mỗi khi push code lên Git, Netlify sẽ tự động deploy!

---

**Chúc bạn deploy thành công! 🎉**

