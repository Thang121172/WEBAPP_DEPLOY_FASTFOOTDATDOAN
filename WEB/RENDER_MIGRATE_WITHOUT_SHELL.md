# 🔧 CHẠY MIGRATIONS TRÊN RENDER (KHÔNG CẦN SHELL)

## ⚠️ **VẤN ĐỀ:**

Render Free tier **KHÔNG có Shell access**. Bạn không thể dùng Shell để chạy migrations.

## ✅ **GIẢI PHÁP: Chạy migrations trong Build Command**

Có 2 cách để chạy migrations mà không cần Shell:

---

## 🔹 **CÁCH 1: Sửa Build Command tạm thời (KHUYÊN DÙNG)**

### **Bước 1: Sửa Build Command:**

1. Vào Render → Service `fastfood-backend` → Tab **"Settings"**
2. Scroll xuống phần **"Build Command"**
3. Sửa build command thành:
   ```bash
   cd backend && pip install -r requirements.txt && python manage.py migrate --noinput && python manage.py collectstatic --noinput
   ```
   - `--noinput` = không hỏi xác nhận (auto-yes)

4. Click **"Save Changes"**
5. Render sẽ tự động redeploy và chạy migrations

### **Bước 2: Sau khi migrations chạy xong:**

1. Sửa lại build command về như cũ:
   ```bash
   cd backend && pip install -r requirements.txt && python manage.py collectstatic --noinput
   ```
2. Click **"Save Changes"** (migrations chỉ cần chạy 1 lần)

---

## 🔹 **CÁCH 2: Chạy migrations mỗi lần deploy (Nếu cần)**

Nếu bạn muốn migrations chạy tự động mỗi lần deploy:

1. Vào Render → Service `fastfood-backend` → Tab **"Settings"**
2. Sửa **"Build Command"** thành:
   ```bash
   cd backend && pip install -r requirements.txt && python manage.py migrate --noinput && python manage.py collectstatic --noinput
   ```
3. Click **"Save Changes"**

⚠️ **Lưu ý:** Cách này sẽ chạy migrations mỗi lần deploy (không sao, Django migrations là idempotent).

---

## 🔹 **CÁCH 3: Chạy migrations trong Start Command (KHÔNG KHUYÊN DÙNG)**

Chạy migrations trước khi start server:

1. Vào Render → Service `fastfood-backend` → Tab **"Settings"**
2. Sửa **"Start Command"** thành:
   ```bash
   cd backend && python manage.py migrate --noinput && gunicorn core.wsgi:application --bind 0.0.0.0:$PORT
   ```

⚠️ **Nhược điểm:** Chậm hơn vì phải chờ migrations mỗi lần restart.

---

## ✅ **KHUYÊN DÙNG: CÁCH 1**

- ✅ Chạy migrations 1 lần khi deploy
- ✅ Sau đó sửa lại build command về như cũ
- ✅ Nhanh và đơn giản

---

## 📝 **TẠO SUPERUSER (KHÔNG CẦN SHELL)**

Nếu cần tạo superuser, có thể:

### **Cách 1: Tạo bằng Django admin script**

Tạo một script Python để tạo superuser tự động:

1. Tạo file `backend/create_superuser.py`:
```python
import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'core.settings.prod')
django.setup()

from django.contrib.auth import get_user_model

User = get_user_model()

if not User.objects.filter(username='admin').exists():
    User.objects.create_superuser(
        username='admin',
        email='admin@example.com',
        password='your-secure-password-here'  # Thay bằng password mạnh!
    )
    print("Superuser created!")
else:
    print("Superuser already exists!")
```

2. Thêm vào build command:
```bash
cd backend && pip install -r requirements.txt && python manage.py migrate --noinput && python create_superuser.py && python manage.py collectstatic --noinput
```

⚠️ **Lưu ý:** Không commit password vào Git! Dùng environment variable.

---

## 🔍 **KIỂM TRA MIGRATIONS ĐÃ CHẠY:**

1. Xem build logs trong Render:
   - Vào service → Tab **"Logs"**
   - Tìm dòng: `Operations to perform:` và `Running migrations:`
   - Nếu thấy các migrations được apply → ✅ **OK!**

2. Test API:
   - Mở URL backend: `https://fastfood-backend-t8jz.onrender.com/api/`
   - Nếu API hoạt động → ✅ **Database OK!**

---

## ⚠️ **LƯU Ý:**

- **DATABASE_URL phải được set trước** khi chạy migrations
- Đảm bảo database đã được tạo
- Migrations chỉ cần chạy 1 lần (trừ khi có migrations mới)

---

**Dùng Cách 1 để chạy migrations! Sau đó sửa lại build command về như cũ.** 🚀

