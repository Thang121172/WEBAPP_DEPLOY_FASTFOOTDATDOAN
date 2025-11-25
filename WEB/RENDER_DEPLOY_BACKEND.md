# 🚀 HƯỚNG DẪN DEPLOY BACKEND LÊN RENDER

## ✅ **TẠI SAO CHỌN RENDER?**

- ✅ Đã có sẵn file `backend/render.yaml` trong project
- ✅ Hỗ trợ Django, PostgreSQL, Redis
- ✅ Có free tier
- ✅ Auto-deploy từ Git
- ✅ SSL tự động

---

## 📋 **BƯỚC 1: ĐĂNG KÝ RENDER**

1. Truy cập: https://render.com
2. Đăng ký bằng GitHub account (khuyên dùng)
3. Authorize Render truy cập repositories

---

## 📋 **BƯỚC 2: DEPLOY BẰNG BLUEPRINT (KHUYÊN DÙNG)**

### **Cách 1: Deploy từ render.yaml (Tự động)**

1. Vào Dashboard → **"New +"** → **"Blueprint"**
2. Chọn repository: `Thang121172/TEST_WEB_DEPLOY` (hoặc repo của bạn)
3. Click **"Apply"**
4. Render sẽ tự động đọc file `backend/render.yaml` và tạo:
   - PostgreSQL Database
   - Web Service (Django backend)
   - Migration Job (nếu có)

5. Kiểm tra và điều chỉnh:
   - **Database name:** `fastfood-db`
   - **Service name:** `fastfood-backend`
   - **Environment variables:** Render sẽ tự tạo một số biến

6. Click **"Apply"** để deploy

---

### **Cách 2: Deploy thủ công (Nếu không có render.yaml)**

1. **Tạo PostgreSQL Database:**
   - Vào **"New +"** → **"PostgreSQL"**
   - **Name:** `fastfood-db`
   - **Database:** `fastfood_db`
   - **User:** `fastfood_user`
   - **Plan:** Free
   - Click **"Create Database"**

2. **Lưu lại thông tin:**
   - **Internal Database URL:** (sẽ dùng sau)
   - **External Database URL:** (cho local testing)

3. **Tạo Web Service:**
   - Vào **"New +"** → **"Web Service"**
   - Connect repository của bạn
   - Cấu hình:
     - **Name:** `fastfood-backend`
     - **Environment:** `Python 3`
     - **Build Command:** 
       ```bash
       cd backend && pip install -r requirements.txt && python manage.py collectstatic --noinput
       ```
     - **Start Command:**
       ```bash
       cd backend && gunicorn core.wsgi:application --bind 0.0.0.0:$PORT
       ```
     - **Plan:** Free

4. **Thêm Environment Variables:**
   - `DATABASE_URL` → Lấy từ Database service (Internal Database URL)
   - `SECRET_KEY` → Generate random string (hoặc để Render tự generate)
   - `ALLOWED_HOSTS` → `your-service-name.onrender.com`
   - `DEBUG` → `False`
   - `DJANGO_SETTINGS_MODULE` → `core.settings.prod`

5. Click **"Create Web Service"**

---

## 📋 **BƯỚC 3: CHẠY MIGRATIONS**

Sau khi deploy xong:

1. Vào Web Service → **"Shell"** tab
2. Chạy lệnh:
   ```bash
   cd backend && python manage.py migrate
   ```
3. Tạo superuser (nếu cần):
   ```bash
   cd backend && python manage.py createsuperuser
   ```

---

## 📋 **BƯỚC 4: CẤU HÌNH CORS**

Sửa file `backend/core/settings/prod.py` hoặc settings của bạn:

```python
CORS_ALLOWED_ORIGINS = [
    "https://your-netlify-site.netlify.app",
    "https://your-custom-domain.com",  # nếu có
]
```

Sau đó commit và push:
```powershell
git add backend/core/settings/prod.py
git commit -m "Add Netlify domain to CORS"
git push
```

Render sẽ tự động redeploy.

---

## 📋 **BƯỚC 5: CẬP NHẬT VITE_API_BASE TRÊN NETLIFY**

1. Vào Netlify → Site settings → Environment variables
2. Sửa `VITE_API_BASE` thành:
   ```
   https://your-service-name.onrender.com/api
   ```
   (Thay `your-service-name` bằng tên service trên Render)

3. Redeploy Netlify site

---

## 🔧 **TROUBLESHOOTING**

### ❌ Lỗi: Database connection failed
- ✅ Kiểm tra `DATABASE_URL` đã được set chưa
- ✅ Đảm bảo dùng Internal Database URL (không phải External)

### ❌ Lỗi: Static files 404
- ✅ Đảm bảo đã chạy `collectstatic` trong build command
- ✅ Kiểm tra `STATIC_ROOT` và `STATIC_URL` trong settings

### ❌ Lỗi: CORS block
- ✅ Cập nhật `CORS_ALLOWED_ORIGINS` với domain Netlify

### ❌ Lỗi: ALLOWED_HOSTS
- ✅ Thêm domain Render vào `ALLOWED_HOSTS`:
  ```python
  ALLOWED_HOSTS = ['your-service-name.onrender.com']
  ```

---

## 📝 **CẤU HÌNH REDIS VÀ CELERY (Tùy chọn)**

Nếu cần Celery:

1. Tạo Redis service trên Render:
   - **"New +"** → **"Redis"**
   - **Name:** `fastfood-redis`
   - **Plan:** Free

2. Thêm environment variable:
   - `CELERY_BROKER_URL` → Redis URL từ Render

3. Tạo Celery Worker service (separate service):
   - **"New +"** → **"Background Worker"**
   - **Start Command:** `cd backend && celery -A core worker -l info`

---

## ✅ **SAU KHI DEPLOY THÀNH CÔNG:**

- 🌐 Backend URL: `https://your-service-name.onrender.com`
- 🔗 API Base: `https://your-service-name.onrender.com/api`
- ✅ SSL tự động
- ✅ Auto-deploy khi push code

---

## 💰 **GIỚI HẠN FREE TIER:**

- Web Service: **Sleep sau 15 phút không dùng** (wake up khi có request)
- Database: **90 ngày** (sau đó phải upgrade)
- Bandwidth: **100GB/tháng**

---

**Chúc bạn deploy thành công! 🎉**

