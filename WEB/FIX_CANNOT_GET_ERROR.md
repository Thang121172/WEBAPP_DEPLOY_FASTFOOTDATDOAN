# Fix "Cannot GET /" Error

## 🔴 Vấn đề:
Tất cả endpoints đều trả về "Cannot GET /" - Django/Gunicorn không khởi động được.

## ✅ Đã sửa:

### 1. **wsgi.py** - Sửa settings module
- Không hardcode `core.settings.dev`
- Sử dụng biến môi trường `DJANGO_SETTINGS_MODULE`

### 2. **start.sh** - Cải thiện error handling
- Thêm debug logging
- Không exit ngay khi migrations/collectstatic fail
- Thêm `--capture-output` cho Gunicorn

---

## 🚀 Các bước tiếp theo:

### 1. Commit và Push code
```bash
git add .
git commit -m "Fix wsgi.py settings and improve start.sh error handling"
git push
```

### 2. Render sẽ tự động redeploy
- Hoặc vào Render Dashboard → Manual Deploy

### 3. Kiểm tra Logs
Vào Render Dashboard → Service → **Logs** và xem:
- ✅ Có thấy "Starting Gunicorn..."?
- ✅ Có thấy "Booting worker with pid"?
- ❌ Có lỗi gì không?

---

## 🔍 Debug nếu vẫn lỗi:

### Kiểm tra trong Shell:
Vào Render Dashboard → Service → **Shell**:

```bash
cd /app/backend
python manage.py check
python manage.py check --deploy
```

### Kiểm tra Environment Variables:
Đảm bảo có:
- `DJANGO_SETTINGS_MODULE` = `core.settings.prod`
- `DATABASE_URL` = Internal Database URL
- `SECRET_KEY` = đã được set

### Test Gunicorn trực tiếp:
```bash
cd /app/backend
gunicorn core.wsgi:application --bind 0.0.0.0:8000 --log-level debug
```

---

## 📋 Các nguyên nhân thường gặp:

### 1. Database Connection Error
- **Triệu chứng**: Lỗi trong logs về database
- **Giải pháp**: Kiểm tra `DATABASE_URL` có đúng Internal URL không

### 2. Import Error
- **Triệu chứng**: Lỗi import module trong logs
- **Giải pháp**: Kiểm tra PYTHONPATH và cấu trúc thư mục

### 3. Settings Error
- **Triệu chứng**: Lỗi khi load settings
- **Giải pháp**: Kiểm tra `DJANGO_SETTINGS_MODULE`

### 4. Port Binding Error
- **Triệu chứng**: Gunicorn không bind được port
- **Giải pháp**: Kiểm tra PORT env var

---

## ⚠️ Lưu ý:

Sau khi push code, đợi Render build và deploy xong (có thể mất 2-5 phút), sau đó:
1. Kiểm tra logs
2. Test lại endpoint `/`
3. Nếu vẫn lỗi, gửi logs để debug tiếp

