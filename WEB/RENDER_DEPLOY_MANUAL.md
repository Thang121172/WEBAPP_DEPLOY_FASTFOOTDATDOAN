# 🚀 HƯỚNG DẪN DEPLOY BACKEND LÊN RENDER (KHÔNG DÙNG BLUEPRINT)

## ⚠️ **VẤN ĐỀ:**

Render Blueprint không hỗ trợ tạo PostgreSQL database tự động. Cần tạo **THỦ CÔNG**.

## ✅ **GIẢI PHÁP: Deploy thủ công từng bước**

---

## 📋 **BƯỚC 1: TẠO POSTGRESQL DATABASE**

1. Vào Render Dashboard → **"New +"** → **"PostgreSQL"**

2. Cấu hình:
   - **Name:** `fastfood-db`
   - **Database:** `fastfood_db`
   - **User:** `fastfood_user`
   - **Region:** Chọn gần nhất (Oregon, Frankfurt, Singapore...)
   - **PostgreSQL Version:** `15` (hoặc mới nhất)
   - **Plan:** **Free** (hoặc Starter nếu cần)

3. Click **"Create Database"**

4. **Lưu lại:**
   - Vào database vừa tạo → Tab **"Connections"**
   - Copy **"Internal Database URL"** (dùng cho Render services)
   - Copy **"External Database URL"** (dùng cho local testing)

---

## 📋 **BƯỚC 2: TẠO WEB SERVICE (Django Backend)**

### **2.1. Tạo Service:**

1. Vào Dashboard → **"New +"** → **"Web Service"**

2. **Connect Repository:**
   - Chọn **GitHub** → Chọn repository: `Thang121172/TEST_WEB_DEPLOY`
   - Click **"Connect"**

### **2.2. Cấu hình Basic:**

- **Name:** `fastfood-backend`
- **Region:** Cùng region với database (Oregon recommended)
- **Branch:** `main`
- **Root Directory:** *(để trống - Render tự tìm)*
- **Environment:** `Python 3`
- **Build Command:**
  ```bash
  cd backend && pip install -r requirements.txt && python manage.py collectstatic --noinput
  ```
- **Start Command:**
  ```bash
  cd backend && gunicorn core.wsgi:application --bind 0.0.0.0:$PORT
  ```
- **Plan:** **Free** (hoặc Starter)

### **2.3. Thêm Environment Variables:**

Click **"Advanced"** → **"Add Environment Variable"**, thêm:

1. **DATABASE_URL:**
   - Key: `DATABASE_URL`
   - Value: *(Paste Internal Database URL từ database service)*
   - Format: `postgresql://user:password@host:port/dbname`

2. **SECRET_KEY:**
   - Key: `SECRET_KEY`
   - Value: *(Generate random string hoặc để Render tự tạo)*
   - Hoặc dùng: `python -c "from django.core.management.utils import get_random_secret_key; print(get_random_secret_key())"`

3. **DJANGO_SETTINGS_MODULE:**
   - Key: `DJANGO_SETTINGS_MODULE`
   - Value: `core.settings.prod`

4. **ENVIRONMENT:**
   - Key: `ENVIRONMENT`
   - Value: `Production`

5. **CORS_ORIGINS:**
   - Key: `CORS_ORIGINS`
   - Value: `https://your-netlify-site.netlify.app`
   - ⚠️ Thay `your-netlify-site` bằng URL Netlify thực tế!

6. **CELERY_BROKER_URL:**
   - Key: `CELERY_BROKER_URL`
   - Value: *(Để trống nếu không dùng Celery)*

### **2.4. Deploy:**

1. Click **"Create Web Service"**
2. Render sẽ bắt đầu build và deploy
3. Chờ khoảng 5-10 phút

---

## 📋 **BƯỚC 3: CHẠY MIGRATIONS**

### **Cách 1: Qua Shell (KHUYÊN DÙNG)**

1. Vào service **"fastfood-backend"** → Tab **"Shell"**
2. Click **"Connect"** để mở terminal
3. Chạy:
   ```bash
   cd backend
   python manage.py migrate
   ```

### **Cách 2: Qua Manual Deploy**

1. Vào service → Tab **"Events"**
2. Click **"Manual Deploy"** → **"Deploy latest commit"**
3. Trong build command, tạm thời thêm migrate:
   ```bash
   cd backend && pip install -r requirements.txt && python manage.py migrate && python manage.py collectstatic --noinput
   ```
4. Sau khi migrate xong, sửa lại build command về như cũ

### **Tạo Superuser (nếu cần):**

Trong Shell:
```bash
cd backend
python manage.py createsuperuser
```

---

## 📋 **BƯỚC 4: KIỂM TRA**

1. **Kiểm tra Backend:**
   - Mở URL: `https://fastfood-backend-xxxx.onrender.com`
   - Hoặc: `https://fastfood-backend-xxxx.onrender.com/api/`
   - Nếu thấy response → ✅ **OK!**

2. **Kiểm tra Logs:**
   - Vào service → Tab **"Logs"**
   - Xem có lỗi gì không

3. **Kiểm tra Database Connection:**
   - Vào Shell → Chạy:
     ```bash
     cd backend
     python manage.py dbshell
     ```
   - Nếu vào được database → ✅ **Connection OK!**

---

## 📋 **BƯỚC 5: CẬP NHẬT NETLIFY**

1. Vào Netlify → Site settings → Environment variables
2. Sửa `VITE_API_BASE`:
   ```
   https://fastfood-backend-xxxx.onrender.com/api
   ```
3. Redeploy Netlify

---

## 🔧 **TROUBLESHOOTING**

### ❌ **Lỗi: Database connection failed**

- ✅ Kiểm tra `DATABASE_URL` đã đúng chưa (Internal URL)
- ✅ Kiểm tra database đã được tạo và running chưa
- ✅ Kiểm tra region của database và service có cùng không

### ❌ **Lỗi: ALLOWED_HOSTS**

- ✅ Thêm domain Render vào environment variable:
  ```
  ALLOWED_HOSTS=fastfood-backend-xxxx.onrender.com
  ```

### ❌ **Lỗi: Static files 404**

- ✅ Kiểm tra build command có `collectstatic` chưa
- ✅ Kiểm tra `whitenoise` trong requirements.txt

### ❌ **Lỗi: CORS block**

- ✅ Thêm `CORS_ORIGINS` với domain Netlify
- ✅ Redeploy service sau khi thêm

---

## ✅ **SAU KHI HOÀN TẤT:**

- ✅ Database: `fastfood-db` (PostgreSQL)
- ✅ Backend: `https://fastfood-backend-xxxx.onrender.com`
- ✅ Frontend: `https://your-site.netlify.app`
- ✅ Migrations: Đã chạy
- ✅ CORS: Đã cấu hình

---

**Chúc bạn deploy thành công!** 🎉

