# Debug Health Endpoint - Cannot GET /api/health/

## 🔍 Các bước kiểm tra:

### 1. Test các endpoint khác

Thử các URL sau để xem endpoint nào hoạt động:

```
https://your-backend.onrender.com/
https://your-backend.onrender.com/health/
https://your-backend.onrender.com/api/
https://your-backend.onrender.com/admin/
```

### 2. Kiểm tra Logs trên Render

Vào Render Dashboard → Service → **Logs** và xem:
- ✅ Gunicorn đã khởi động thành công?
- ✅ Migrations đã chạy?
- ❌ Có lỗi nào không?

### 3. Kiểm tra Environment Variables

Đảm bảo có:
- `DJANGO_SETTINGS_MODULE` = `core.settings.prod`
- `DATABASE_URL` đã được set

### 4. Test trong Shell

Vào Render Dashboard → Service → **Shell** và chạy:

```bash
cd /app/backend
python manage.py check
python manage.py show_urls | grep health
```

---

## 🔧 Các nguyên nhân có thể:

### 1. Django chưa khởi động đúng
- **Triệu chứng**: Tất cả endpoint đều lỗi
- **Giải pháp**: Kiểm tra logs, có thể do lỗi import hoặc database connection

### 2. URL routing chưa được load
- **Triệu chứng**: Chỉ một số endpoint lỗi
- **Giải pháp**: Kiểm tra ROOT_URLCONF trong settings

### 3. Static files chưa được collect
- **Triệu chứng**: CSS/JS không load (không ảnh hưởng API)
- **Giải pháp**: Đã có trong start.sh

### 4. Database connection lỗi
- **Triệu chứng**: 500 error
- **Giải pháp**: Kiểm tra DATABASE_URL

---

## ✅ Giải pháp nhanh:

### Thử endpoint root trước:
```
https://your-backend.onrender.com/
```

Nếu endpoint này hoạt động (trả về JSON với status: ok), thì routing đúng, chỉ có thể là vấn đề với path `/api/health/`.

### Nếu tất cả đều lỗi:

1. **Kiểm tra logs** trên Render
2. **Kiểm tra Shell** để xem Django có chạy được không
3. **Kiểm tra DATABASE_URL** có đúng không

---

## 📝 Ghi chú:

Endpoint `/api/health/` được định nghĩa trong `backend/core/urls.py`:
```python
path("api/health/", healthcheck, name="api_healthcheck"),
```

Và `ROOT_URLCONF = "core.urls"` trong settings, nên endpoint phải hoạt động.

Nếu vẫn lỗi, hãy gửi logs từ Render để debug tiếp.

