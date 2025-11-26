# Hướng dẫn Deploy với Docker

## Tổng quan
- **Backend**: Deploy trên Render sử dụng Docker
- **Frontend**: Deploy trên Vercel

## Backend - Render với Docker

### 1. Chuẩn bị
- Đảm bảo có file `Dockerfile` và `start.sh` ở root directory
- File `render.yaml` đã được cấu hình để sử dụng Docker

### 2. Tạo PostgreSQL Database trên Render
1. Vào Render Dashboard
2. Tạo PostgreSQL Database mới (Free tier)
3. Lưu lại Internal Database URL

### 3. Deploy Backend Service
1. Kết nối repository GitHub với Render
2. Render sẽ tự động detect `render.yaml`
3. Thêm các Environment Variables:
   - `DATABASE_URL`: Internal Database URL từ PostgreSQL service
   - `SECRET_KEY`: Render sẽ tự động generate
   - `DJANGO_SETTINGS_MODULE`: `core.settings.prod` (đã set trong yaml)
   - `CORS_ORIGINS`: URL của frontend Vercel (sau khi deploy frontend)
     - Ví dụ: `https://your-app.vercel.app`

### 4. Chạy Migrations
Migrations sẽ chạy tự động trong `start.sh` khi container khởi động.

Nếu cần chạy thủ công:
1. Vào Render Dashboard → Service → Shell
2. Chạy: `python manage.py migrate`

## Frontend - Vercel

### 1. Chuẩn bị
- File `vercel.json` đã được cấu hình với `rootDirectory: "frontend"`

### 2. Deploy trên Vercel
1. Kết nối repository GitHub với Vercel
2. Vercel sẽ tự động detect cấu hình từ `vercel.json`
3. Thêm Environment Variables (nếu cần):
   - `VITE_API_URL`: URL của backend Render
     - Ví dụ: `https://fastfood-backend.onrender.com`

### 3. Cập nhật CORS trên Backend
Sau khi có URL Vercel, cập nhật `CORS_ORIGINS` trong Render:
- Vào Backend Service → Environment
- Thêm hoặc cập nhật: `CORS_ORIGINS=https://your-app.vercel.app`

## Kiểm tra

### Backend
- Health check: `https://your-backend.onrender.com/api/health/`
- API docs: `https://your-backend.onrender.com/api/docs/`

### Frontend
- Truy cập URL Vercel và kiểm tra kết nối với backend

## Lưu ý
- Render free tier sẽ sleep sau 15 phút không có traffic
- Để tránh sleep, có thể dùng service như UptimeRobot để ping định kỳ
- Database migrations chạy tự động trong `start.sh`
- Static files được collect tự động trong `start.sh`

