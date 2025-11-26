# Environment Variables cho Render Web Service

## 🔴 BẮT BUỘC (Phải có)

### 1. **SECRET_KEY**
- **Giá trị**: Render sẽ tự động generate (có nút "Generate")
- **Hoặc**: Bạn có thể tự tạo một key ngẫu nhiên
- **Lưu ý**: Đây là key bảo mật quan trọng, không được để trống

### 2. **DATABASE_URL**
- **Giá trị**: Internal Database URL từ PostgreSQL service
- **Cách lấy**: 
  1. Vào PostgreSQL service (`fastfood-db`)
  2. Copy "Internal Database URL" 
  3. Paste vào đây
- **Ví dụ**: `postgresql://fastfood_user:password@dpg-xxxxx-a/fastfood_db`

### 3. **DJANGO_SETTINGS_MODULE**
- **Giá trị**: `core.settings.prod`
- **Lưu ý**: Đã được set trong render.yaml, nhưng nên kiểm tra lại

### 4. **RENDER_EXTERNAL_HOSTNAME**
- **Giá trị**: Render sẽ tự động set (từ render.yaml)
- **Hoặc**: URL của service, ví dụ: `fastfood-backend.onrender.com`

---

## 🟡 QUAN TRỌNG (Nên có ngay)

### 5. **CORS_ORIGINS**
- **Giá trị**: URL của frontend Vercel (sau khi deploy frontend)
- **Format**: Có thể nhiều URL, cách nhau bởi dấu phẩy
- **Ví dụ**: 
  - Nếu chỉ có 1 frontend: `https://your-app.vercel.app`
  - Nếu có nhiều: `https://your-app.vercel.app,https://www.your-app.vercel.app`
- **Lưu ý**: Nếu chưa deploy frontend, có thể để trống tạm thời, nhưng nhớ thêm sau

### 6. **ENVIRONMENT**
- **Giá trị**: `Production`
- **Lưu ý**: Đã được set trong render.yaml

---

## 🟢 TÙY CHỌN (Nếu cần)

### 7. **CELERY_BROKER_URL** (Nếu dùng Celery)
- **Giá trị**: URL Redis nếu bạn có Redis service
- **Ví dụ**: `redis://redis-host:6379/1`
- **Hiện tại**: Có thể để trống nếu chưa dùng

### 8. **ALLOWED_HOSTS** (Nếu cần tùy chỉnh)
- **Giá trị**: Danh sách host được phép, cách nhau bởi dấu phẩy
- **Ví dụ**: `fastfood-backend.onrender.com,your-custom-domain.com`
- **Lưu ý**: Thường không cần set vì `RENDER_EXTERNAL_HOSTNAME` đã tự động set

### 9. **EMAIL Settings** (Nếu cần gửi email OTP)
- `EMAIL_HOST`: `smtp.gmail.com`
- `EMAIL_PORT`: `587`
- `EMAIL_HOST_USER`: Email của bạn
- `EMAIL_HOST_PASSWORD`: App password của Gmail
- `EMAIL_USE_TLS`: `True`
- `DEFAULT_FROM_EMAIL`: Email gửi đi

---

## 📋 Checklist khi tạo Web Service mới

- [ ] **SECRET_KEY** - Click "Generate" hoặc tự tạo
- [ ] **DATABASE_URL** - Copy từ PostgreSQL service
- [ ] **DJANGO_SETTINGS_MODULE** = `core.settings.prod`
- [ ] **ENVIRONMENT** = `Production`
- [ ] **CORS_ORIGINS** - Thêm sau khi có URL frontend Vercel
- [ ] **RENDER_EXTERNAL_HOSTNAME** - Render tự động set (kiểm tra lại)

---

## 🚀 Thứ tự thực hiện

1. **Tạo PostgreSQL Database trước** (nếu chưa có)
2. **Tạo Web Service** với các env vars bắt buộc
3. **Deploy và test** backend
4. **Deploy frontend** trên Vercel
5. **Quay lại Render** và thêm `CORS_ORIGINS` với URL Vercel

---

## ⚠️ Lưu ý quan trọng

- **DATABASE_URL**: Phải dùng **Internal Database URL**, không phải External URL
- **CORS_ORIGINS**: Phải có `https://` ở đầu, không có dấu `/` ở cuối
- **SECRET_KEY**: Không được share hoặc commit lên Git
- Sau khi thêm/sửa env vars, service sẽ tự động redeploy

