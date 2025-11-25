# Hướng dẫn chạy Backend cho APP và WEB

## 📋 Tổng quan

Project có 2 backend:
- **APP Backend**: Node.js/Express (port 8001)
- **WEB Backend**: Django/Python (port 8000)

---

## 🚀 APP Backend (Node.js/Express)

### Vị trí: `APP/backend/`

### 1. Chạy với Docker (Khuyến nghị)

```powershell
# Từ thư mục APP
cd APP
docker compose up -d

# Xem logs
docker compose logs -f backend

# Dừng
docker compose down
```

**Cổng:** http://localhost:8001

### 2. Chạy trực tiếp (Development)

```powershell
# Di chuyển vào thư mục backend
cd APP\backend

# Cài đặt dependencies (lần đầu)
npm install

# Tạo file .env nếu chưa có
# Copy từ .env.example và điền thông tin

# Chạy migrations (nếu cần)
npm run migrate

# Chạy server
npm start
# hoặc
node index.js
```

**Các lệnh npm có sẵn:**
- `npm start` - Chạy server
- `npm run migrate` - Chạy database migrations
- `npm run cleanup:tokens` - Dọn dẹp tokens đã hết hạn
- `npm run test:ci` - Chạy tests
- `npm run test:smoke` - Chạy smoke tests

### 3. Cấu hình môi trường (.env)

```env
JWT_SECRET=supersecret
ADMIN_SECRET=adminkey
POSTGRES_DB=fastfood
POSTGRES_USER=app
POSTGRES_PASSWORD=123456
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
REDIS_HOST=localhost
REDIS_PORT=6379
DEBUG_SHOW_OTP=true
ALLOW_SMOKE_SEED=true
DEV_TOKEN=testtoken
```

---

## 🌐 WEB Backend (Django/Python)

### Vị trí: `WEB/backend/`

### 1. Chạy với Docker (Khuyến nghị)

```powershell
# Từ thư mục WEB
cd WEB
docker compose up -d

# Xem logs
docker compose logs -f backend

# Dừng
docker compose down
```

**Cổng:** http://localhost:8000

### 2. Chạy trực tiếp (Development)

#### Bước 1: Tạo và kích hoạt virtual environment

```powershell
# Di chuyển vào thư mục WEB
cd WEB

# Tạo virtual environment (lần đầu)
python -m venv venv

# Kích hoạt virtual environment
# Windows PowerShell:
.\venv\Scripts\Activate.ps1
# Windows CMD:
venv\Scripts\activate.bat
# Linux/Mac:
source venv/bin/activate
```

#### Bước 2: Cài đặt dependencies

```powershell
# Di chuyển vào thư mục backend
cd backend

# Cài đặt packages
pip install -r requirements.txt
```

#### Bước 3: Cấu hình môi trường

Tạo file `.env` trong thư mục `WEB/`:

```env
DJANGO_SECRET_KEY=dev-secret-key-change-in-production
DEBUG=True
ALLOWED_HOSTS=127.0.0.1,localhost,backend
CORS_ORIGINS=http://localhost:5173,http://localhost:5174
POSTGRES_DB=fastfood
POSTGRES_USER=app
POSTGRES_PASSWORD=123456
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
DATABASE_URL=postgresql://app:123456@localhost:5432/fastfood
REDIS_URL=redis://localhost:6379/0
```

#### Bước 4: Chạy migrations

```powershell
# Từ thư mục WEB/backend
python manage.py migrate
```

#### Bước 5: Tạo superuser (nếu cần)

```powershell
python manage.py createsuperuser
```

#### Bước 6: Chạy server

```powershell
# Development server
python manage.py runserver

# Hoặc chỉ định port
python manage.py runserver 8000
```

**URLs:**
- API: http://localhost:8000
- Admin: http://localhost:8000/admin
- Health: http://localhost:8000/api/health/

### 3. Chạy với Gunicorn (Production)

```powershell
# Từ thư mục WEB/backend
gunicorn core.wsgi:application --bind 0.0.0.0:8000 --workers 4
```

### 4. Chạy Celery (Background tasks)

```powershell
# Terminal 1: Django server
python manage.py runserver

# Terminal 2: Celery worker
celery -A core worker -l info

# Terminal 3: Celery beat (scheduled tasks)
celery -A core beat -l info
```

### 5. Các lệnh Django hữu ích

```powershell
# Migrations
python manage.py makemigrations
python manage.py migrate

# Collect static files
python manage.py collectstatic

# Tạo superuser
python manage.py createsuperuser

# Shell
python manage.py shell

# Kiểm tra
python manage.py check

# Seed data (nếu có)
python manage.py seed_demo
```

---

## 🐳 Chạy cả 2 Backend cùng lúc với Docker

### Từ thư mục gốc (FASTFOOD):

```powershell
# Chạy script tự động
.\start_all_servers.ps1
```

Script này sẽ:
1. Kiểm tra Docker
2. Tạo file .env nếu chưa có
3. Khởi động WEB services (db, redis, backend, celery, frontend)
4. Khởi động APP services (db, redis, backend, adminer)

### Hoặc chạy thủ công:

```powershell
# Terminal 1: WEB Backend
cd WEB
docker compose up -d

# Terminal 2: APP Backend
cd APP
docker compose up -d
```

---

## 📊 Các cổng mặc định

| Service | Port | URL |
|---------|------|-----|
| WEB Backend | 8000 | http://localhost:8000 |
| APP Backend | 8001 | http://localhost:8001 |
| WEB Frontend | 5174 | http://localhost:5174 |
| PostgreSQL (WEB) | 5433 | localhost:5433 |
| PostgreSQL (APP) | 5432 | localhost:5432 |
| Redis (WEB) | 6380 | localhost:6380 |
| Redis (APP) | 6379 | localhost:6379 |
| Adminer (APP) | 8080 | http://localhost:8080 |

---

## 🔍 Kiểm tra trạng thái

### Docker containers:

```powershell
# Xem tất cả containers
docker ps

# Xem logs
docker compose logs -f

# Xem logs của service cụ thể
docker compose logs -f backend
```

### Kiểm tra API:

```powershell
# WEB Backend health check
curl http://localhost:8000/api/health/

# APP Backend (nếu có endpoint health)
curl http://localhost:8001/health
```

---

## 🛠️ Troubleshooting

### Lỗi kết nối database:

1. Kiểm tra PostgreSQL đang chạy:
```powershell
docker ps | Select-String postgres
```

2. Kiểm tra kết nối:
```powershell
# WEB
docker compose exec db psql -U app -d fastfood

# APP
docker compose exec ff_db psql -U app -d fastfood
```

### Lỗi port đã được sử dụng:

```powershell
# Tìm process đang dùng port
netstat -ano | findstr :8000
netstat -ano | findstr :8001

# Kill process (thay PID bằng process ID)
taskkill /PID <PID> /F
```

### Reset database:

```powershell
# WEB
cd WEB
docker compose down -v
docker compose up -d db
# Sau đó chạy migrations lại

# APP
cd APP
docker compose down -v
docker compose up -d ff_db
# Sau đó chạy migrations lại
```

---

## 📝 Scripts có sẵn

### APP:
- `APP/backend/package.json` - Các npm scripts

### WEB:
- `WEB/start-backend.ps1` - Script chạy backend riêng
- `WEB/start.ps1` - Script chạy cả backend và frontend
- `WEB/start-dev.ps1` - Script development mode
- `WEB/backend/start.sh` - Script production (Linux)

### Root:
- `start_all_servers.ps1` - Chạy tất cả services

---

## 💡 Tips

1. **Development**: Dùng `python manage.py runserver` cho Django và `npm start` cho Node.js
2. **Production**: Dùng Docker hoặc Gunicorn cho Django
3. **Hot reload**: Django và Node.js đều tự động reload khi code thay đổi (development mode)
4. **Logs**: Luôn kiểm tra logs khi có lỗi: `docker compose logs -f`

