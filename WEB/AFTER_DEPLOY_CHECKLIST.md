# Checklist sau khi Backend đã Live trên Render

## ✅ Backend đã deploy thành công!

---

## 🔍 Kiểm tra Backend

### 1. Test Health Check
Mở trình duyệt và truy cập:
```
https://your-backend.onrender.com/api/health/
```

Kết quả mong đợi:
```json
{
  "status": "ok",
  "message": "API is operational"
}
```

### 2. Test API Docs
Truy cập Swagger/OpenAPI docs:
```
https://your-backend.onrender.com/api/docs/
```

### 3. Test Root Endpoint
```
https://your-backend.onrender.com/
```

---

## ⚙️ Kiểm tra Environment Variables

Vào Render Dashboard → Service → Environment, đảm bảo có:

### ✅ Bắt buộc:
- [ ] `SECRET_KEY` - Đã được generate
- [ ] `DATABASE_URL` - Internal Database URL từ PostgreSQL service
- [ ] `DJANGO_SETTINGS_MODULE` = `core.settings.prod`
- [ ] `ENVIRONMENT` = `Production`
- [ ] `RENDER_EXTERNAL_HOSTNAME` - Tự động set

### ⚠️ Quan trọng:
- [ ] `CORS_ORIGINS` = `https://test-web-deploy-eight.vercel.app`
  - Nếu chưa có, thêm ngay để frontend có thể gọi API

---

## 🔗 Kết nối Frontend với Backend

### 1. Lấy URL Backend
- URL Backend: `https://your-backend.onrender.com`
- Ví dụ: `https://fastfood-backend.onrender.com`

### 2. Cập nhật Frontend
Nếu frontend cần biết URL backend, thêm vào Vercel Environment Variables:
- `VITE_API_URL` = `https://your-backend.onrender.com`

### 3. Cập nhật CORS trên Backend
- Vào Render → Service → Environment
- Thêm hoặc cập nhật: `CORS_ORIGINS` = `https://test-web-deploy-eight.vercel.app`
- Service sẽ tự động redeploy

---

## 🗄️ Kiểm tra Database

### 1. Chạy Migrations (nếu cần)
Migrations đã chạy tự động trong `start.sh`, nhưng nếu cần chạy thủ công:

1. Vào Render Dashboard → Service → Shell
2. Chạy:
```bash
cd /app/backend
python manage.py migrate
```

### 2. Tạo Superuser (nếu cần)
```bash
python manage.py createsuperuser
```

---

## 🧪 Test API Endpoints

### 1. Test Authentication
```bash
# Register
POST https://your-backend.onrender.com/api/accounts/register/

# Login
POST https://your-backend.onrender.com/api/accounts/login/
```

### 2. Test Orders API
```bash
GET https://your-backend.onrender.com/api/orders/
```

---

## 📊 Kiểm tra Logs

Vào Render Dashboard → Service → Logs để xem:
- ✅ Gunicorn đã khởi động
- ✅ Migrations đã chạy
- ✅ Static files đã được collect
- ❌ Không có lỗi

---

## 🚨 Xử lý lỗi thường gặp

### Lỗi CORS
- **Triệu chứng**: Frontend không gọi được API
- **Giải pháp**: Thêm `CORS_ORIGINS` với URL Vercel

### Lỗi Database Connection
- **Triệu chứng**: 500 error khi gọi API
- **Giải pháp**: Kiểm tra `DATABASE_URL` có đúng Internal URL không

### Lỗi Static Files
- **Triệu chứng**: CSS/JS không load
- **Giải pháp**: Kiểm tra `collectstatic` đã chạy trong logs

---

## 🎯 Bước tiếp theo

1. ✅ Backend đã live
2. ⏳ Test các API endpoints
3. ⏳ Cập nhật CORS_ORIGINS với URL Vercel
4. ⏳ Test kết nối Frontend ↔ Backend
5. ⏳ Tạo superuser nếu cần
6. ⏳ Seed dữ liệu demo (nếu có)

---

## 📝 Ghi chú

- Render free tier sẽ **sleep sau 15 phút** không có traffic
- Lần đầu truy cập sau khi sleep có thể mất 30-60 giây để wake up
- Để tránh sleep, có thể dùng service như **UptimeRobot** để ping định kỳ

---

## 🎉 Chúc mừng!

Backend đã được deploy thành công trên Render với Docker!

