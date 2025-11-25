# 🚀 HƯỚNG DẪN CHI TIẾT: DEPLOY BACKEND LÊN RENDER

## 📋 **TỔNG QUAN**

Bạn sẽ deploy Django backend lên Render.com. Project đã có sẵn file `backend/render.yaml` để tự động cấu hình!

---

## ✅ **BƯỚC 1: CHUẨN BỊ**

### **1.1. Kiểm tra file cấu hình:**

✅ File `backend/render.yaml` đã có sẵn trong project  
✅ File `backend/core/settings/prod.py` đã được cấu hình sẵn

### **1.2. Push code lên GitHub (nếu chưa có):**

```powershell
git add .
git commit -m "Prepare for Render deployment"
git push origin main
```

---

## 🌐 **BƯỚC 2: ĐĂNG KÝ RENDER**

1. **Truy cập:** https://render.com
2. **Click:** "Get Started for Free"
3. **Đăng ký bằng GitHub:**
   - Click "Sign up with GitHub"
   - Authorize Render truy cập repositories
   - Chọn repositories bạn muốn deploy (hoặc "All repositories")

✅ **Sau khi đăng ký xong, bạn sẽ vào Dashboard**

---

## 🚀 **BƯỚC 3: DEPLOY BẰNG BLUEPRINT (CÁCH DỄ NHẤT)**

### **3.1. Tạo Blueprint:**

1. Trên Dashboard Render, click **"New +"** (góc trên bên phải)
2. Chọn **"Blueprint"** từ dropdown menu
3. Chọn **"Public Git repository"** hoặc connect GitHub nếu chưa connect

### **3.2. Chọn Repository:**

1. Tìm và chọn repository của bạn: `Thang121172/TEST_WEB_DEPLOY` (hoặc tên repo của bạn)
2. Click **"Connect"** hoặc **"Apply"**

### **3.3. Render sẽ tự động đọc file `backend/render.yaml`:**

Render sẽ hiển thị preview các services sẽ được tạo:

```
✅ fastfood-db (PostgreSQL Database)
✅ fastfood-backend (Web Service)
✅ fastfood-migrate (Job - chạy migrations)
```

### **3.4. Xem lại và Apply:**

1. **Kiểm tra cấu hình:**
   - Database name: `fastfood-db`
   - Web service name: `fastfood-backend`
   - Build command: `cd backend && pip install -r requirements.txt && python manage.py collectstatic --noinput`
   - Start command: `cd backend && gunicorn core.wsgi:application --bind 0.0.0.0:$PORT`

2. **Click "Apply"** để bắt đầu deploy

3. **Chờ deploy xong:** (khoảng 5-10 phút)
   - Render sẽ tự động:
     - Tạo PostgreSQL database
     - Build và deploy web service
     - Chạy migrations

---

## ⚙️ **BƯỚC 4: CẤU HÌNH THÊM (SAU KHI DEPLOY)**

### **4.1. Lấy URL Backend:**

1. Vào Dashboard → Click vào service **"fastfood-backend"**
2. Copy **URL** (ví dụ: `https://fastfood-backend-xxxx.onrender.com`)
3. **Lưu lại URL này!** (sẽ dùng để cập nhật Netlify)

### **4.2. Thêm Environment Variables cho CORS:**

1. Vào service **"fastfood-backend"** → Tab **"Environment"**
2. Click **"Add Environment Variable"**
3. Thêm biến:

   ```
   Key: CORS_ORIGINS
   Value: https://your-netlify-site.netlify.app,https://your-custom-domain.com
   ```
   
   ⚠️ **Thay `your-netlify-site` bằng URL Netlify thực tế của bạn!**

4. Click **"Save Changes"**

5. Render sẽ tự động **redeploy** với cấu hình mới

---

## 🔧 **BƯỚC 5: CHẠY MIGRATIONS (NẾU CẦN)**

### **Cách 1: Dùng Job (Tự động)**
- File `render.yaml` đã có job `fastfood-migrate` sẽ tự chạy

### **Cách 2: Chạy thủ công qua Shell:**

1. Vào service **"fastfood-backend"** → Tab **"Shell"**
2. Click **"Connect"** để mở terminal
3. Chạy lệnh:
   ```bash
   cd backend
   python manage.py migrate
   ```

### **Tạo Superuser (nếu cần):**

```bash
cd backend
python manage.py createsuperuser
```
- Nhập username, email, password khi được hỏi

---

## 🔗 **BƯỚC 6: CẬP NHẬT NETLIFY (KẾT NỐI VỚI BACKEND)**

1. **Vào Netlify** → Site của bạn → **"Site settings"**

2. **Vào "Environment variables"**

3. **Sửa biến `VITE_API_BASE`:**
   ```
   Key: VITE_API_BASE
   Value: https://fastfood-backend-xxxx.onrender.com/api
   ```
   ⚠️ **Thay `fastfood-backend-xxxx` bằng URL thực tế từ Render!**

4. **Click "Save"**

5. **Trigger deploy mới:**
   - Vào tab **"Deploys"**
   - Click **"Trigger deploy"** → **"Clear cache and deploy site"**

---

## ✅ **BƯỚC 7: KIỂM TRA**

### **7.1. Test Backend API:**

1. Mở browser, truy cập: `https://your-backend-url.onrender.com/api/`
2. Hoặc test endpoint: `https://your-backend-url.onrender.com/api/accounts/`
3. Nếu thấy response (JSON hoặc HTML) → ✅ **Backend hoạt động!**

### **7.2. Test từ Frontend:**

1. Mở website Netlify của bạn
2. Thử đăng nhập hoặc load dữ liệu
3. Mở **Developer Tools** (F12) → Tab **Network**
4. Kiểm tra API calls có thành công không

### **7.3. Kiểm tra Logs (nếu có lỗi):**

1. Vào Render → Service **"fastfood-backend"** → Tab **"Logs"**
2. Xem logs để debug nếu có lỗi

---

## 🔧 **TROUBLESHOOTING**

### ❌ **Lỗi: Build failed**

**Nguyên nhân:** Có thể thiếu dependencies hoặc lỗi trong code

**Giải pháp:**
1. Xem logs trong Render để tìm lỗi cụ thể
2. Test build local trước:
   ```powershell
   cd backend
   pip install -r requirements.txt
   python manage.py collectstatic --noinput
   ```

### ❌ **Lỗi: Database connection failed**

**Nguyên nhân:** `DATABASE_URL` chưa được set đúng

**Giải pháp:**
1. Vào service → Tab **"Environment"**
2. Kiểm tra `DATABASE_URL` có được tự động tạo từ database service chưa
3. Nếu chưa có, thêm manual:
   - Vào database service → Copy "Internal Database URL"
   - Thêm vào environment variables của web service

### ❌ **Lỗi: CORS block từ frontend**

**Nguyên nhân:** Domain Netlify chưa được thêm vào `CORS_ORIGINS`

**Giải pháp:**
1. Vào Render → Service → Tab **"Environment"**
2. Thêm hoặc sửa `CORS_ORIGINS`:
   ```
   CORS_ORIGINS=https://your-netlify-site.netlify.app
   ```
3. Save và đợi redeploy

### ❌ **Lỗi: ALLOWED_HOSTS**

**Nguyên nhân:** Domain Render chưa được thêm vào ALLOWED_HOSTS

**Giải pháp:**
- File `prod.py` đã tự động lấy từ `RENDER_EXTERNAL_HOSTNAME`
- Kiểm tra environment variable `RENDER_EXTERNAL_HOSTNAME` đã có chưa
- Nếu chưa, thêm:
  ```
  ALLOWED_HOSTS=your-service-name.onrender.com
  ```

### ❌ **Lỗi: Static files 404**

**Nguyên nhân:** `collectstatic` chưa chạy hoặc WhiteNoise chưa cấu hình

**Giải pháp:**
- Build command đã có `collectstatic` → Kiểm tra lại logs
- Đảm bảo `whitenoise` trong `requirements.txt`

---

## 📝 **TÓM TẮT CÁC URL QUAN TRỌNG**

Sau khi deploy xong, bạn sẽ có:

| Service | URL | Mục đích |
|---------|-----|----------|
| **Backend API** | `https://fastfood-backend-xxxx.onrender.com` | API endpoint |
| **Frontend** | `https://your-site.netlify.app` | Website người dùng |
| **Admin Panel** | `https://fastfood-backend-xxxx.onrender.com/admin/` | Django admin |

---

## 💰 **LƯU Ý VỀ FREE TIER**

### **Web Service:**
- ⚠️ **Sleep sau 15 phút không có traffic**
- ✅ **Tự động wake up** khi có request (có thể mất 30-60 giây)
- 💡 **Tip:** Dùng service như UptimeRobot để ping định kỳ

### **Database:**
- ⚠️ **Chỉ tồn tại 90 ngày** (sau đó phải upgrade)
- ✅ **100GB bandwidth/tháng**

### **Giải pháp nếu cần 24/7:**
- Upgrade lên paid plan ($7/tháng cho Web Service)
- Hoặc deploy lên VPS (đã có hướng dẫn trong `VPS_DEPLOY_STEPS.md`)

---

## ✅ **SAU KHI HOÀN TẤT:**

1. ✅ Backend đã chạy trên Render
2. ✅ Database đã được tạo và migrations đã chạy
3. ✅ Frontend đã kết nối với backend
4. ✅ CORS đã được cấu hình
5. ✅ SSL tự động cho cả frontend và backend

---

## 🎉 **CHÚC MỪNG!**

Bạn đã deploy thành công full-stack application:
- **Frontend:** Netlify ✅
- **Backend:** Render ✅
- **Database:** Render PostgreSQL ✅

**Website của bạn đã live và hoạt động!** 🚀

---

**Nếu gặp vấn đề gì, xem logs trong Render hoặc hỏi mình nhé!** 😊

