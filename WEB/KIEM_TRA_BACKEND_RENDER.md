# 🔍 Hướng dẫn kiểm tra Backend trên Render

## Bạn đang ở: Render Dashboard → fastfood-backend service

### Bước 1: Lấy Backend URL

1. Ở trang hiện tại (Render dashboard), tìm phần **"Overview"** hoặc **"Settings"**
2. Tìm dòng **"URL"** hoặc **"Live URL"**
3. Copy URL này (ví dụ: `https://fastfood-backend-xxxx.onrender.com`)
4. **Lưu lại URL này** - bạn sẽ cần nó cho bước tiếp theo!

### Bước 2: Kiểm tra Environment Variables

1. Trên trang Render dashboard hiện tại, click vào tab **"Environment"** (ở menu trên cùng)
2. Kiểm tra các biến sau **PHẢI CÓ**:

#### ✅ Các biến BẮT BUỘC:

```
ALLOWED_HOSTS = fastfood-backend-xxxx.onrender.com
```
- Thay `xxxx` bằng tên service của bạn
- Nếu không có, thêm mới

```
CORS_ORIGINS = https://fastfooddatdoan.netlify.app
```
- **KHÔNG có dấu `/` ở cuối**
- Đây là URL frontend của bạn trên Netlify

```
DJANGO_SETTINGS_MODULE = core.settings.prod
```

```
RENDER_EXTERNAL_HOSTNAME = fastfood-backend-xxxx.onrender.com
```
- Thay `xxxx` bằng URL service của bạn

```
SECRET_KEY = (tự động generate)
```

```
DATABASE_URL = (từ PostgreSQL database)
```
- Phải có kết nối database

```
CELERY_BROKER_URL = 
```
- Có thể để trống nếu chưa dùng Celery

### Bước 3: Kiểm tra Logs

1. Click vào tab **"Logs"** trên Render dashboard
2. Xem logs gần đây:
   - Nếu thấy `Application failed to respond` → Backend chưa chạy đúng
   - Nếu thấy `Application is live` → Backend đang chạy OK
   - Nếu thấy lỗi `DisallowedHost` → `ALLOWED_HOSTS` chưa đúng

### Bước 4: Test Backend URL

1. Copy URL backend (từ Bước 1)
2. Thêm `/api/` vào cuối: `https://fastfood-backend-xxxx.onrender.com/api/`
3. Mở browser mới, paste URL này
4. **Phải thấy JSON response** (ví dụ: `{"orders":"...", "merchant":"..."}`)
5. Nếu lỗi 404 hoặc không load → Backend chưa chạy đúng

### Bước 5: Ghi lại thông tin

Sau khi kiểm tra, ghi lại:

```
Backend URL: https://_____________________.onrender.com

CORS_ORIGINS có giá trị: _______________________

ALLOWED_HOSTS có giá trị: _______________________

Backend test URL (/api/) có chạy không: ✅ / ❌
```

## ⚠️ Lưu ý quan trọng

- **URL backend** sẽ dùng để set `VITE_API_BASE` trên Netlify
- **CORS_ORIGINS** phải trùng với URL frontend trên Netlify
- **KHÔNG có dấu `/` ở cuối** trong CORS_ORIGINS và ALLOWED_HOSTS

