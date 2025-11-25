# Các Lệnh Deploy Trên VPS 103.75.182.180

## 📋 Chạy Các Lệnh Này Trên VPS (Sau Khi Kết Nối)

---

## 1️⃣ KIỂM TRA MÔI TRƯỜNG

```powershell
# Kiểm tra Node.js
node -v

# Kiểm tra npm
npm -v

# Kiểm tra Git
git --version

# Kiểm tra MySQL
mysql --version
```

**Nếu chưa có → Cài đặt theo hướng dẫn dưới**

---

## 2️⃣ DOWNLOAD & CÀI ĐẶT PHẦN MỀM

### Node.js
- Link: https://nodejs.org/en/download/
- Chọn: Windows Installer (.msi) - 64-bit - LTS
- Chạy installer → Next → Next → Install

### Git
- Link: https://git-scm.com/download/win
- Chạy installer → Next → Next → Install

### MySQL
- Link: https://dev.mysql.com/downloads/installer/
- Chọn: mysql-installer-community-8.x.x.msi
- Chạy installer:
  - Setup Type: Developer Default
  - Root Password: **GHI NHỚ PASSWORD NÀY!**

---

## 3️⃣ CLONE CODE

```powershell
# Tạo thư mục
cd C:\
mkdir Projects
cd Projects

# Clone code
git clone https://github.com/Thang121172/TEST_WEB_DEPLOY.git

# Vào thư mục
cd TEST_WEB_DEPLOY
dir
```

---

## 4️⃣ SETUP DATABASE

### Tạo database:

```powershell
# Mở MySQL Command Line hoặc MySQL Workbench
mysql -u root -p
```

Sau đó chạy SQL:

```sql
CREATE DATABASE fastfood_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SHOW DATABASES;
EXIT;
```

---

## 5️⃣ SETUP BACKEND

### Tạo file .env:

```powershell
cd C:\Projects\TEST_WEB_DEPLOY\backend
notepad .env
```

**Nội dung file .env:**

```env
PORT=5000
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=your_mysql_root_password
DB_NAME=fastfood_db
JWT_SECRET=fastfood_secret_key_2024
NODE_ENV=production
```

**Lưu và đóng Notepad**

### Cài dependencies:

```powershell
npm install
```

### Test backend:

```powershell
node server.js
```

**Mở trình duyệt trên VPS:** `http://localhost:5000`

Nếu chạy OK → Nhấn `Ctrl + C` để stop

### Cài PM2 và chạy backend:

```powershell
npm install -g pm2
npm install -g pm2-windows-service

# Cài PM2 service
pm2-service-install

# Start backend
cd C:\Projects\TEST_WEB_DEPLOY\backend
pm2 start server.js --name backend
pm2 save
pm2 list
```

---

## 6️⃣ BUILD FRONTEND

### Cấu hình API URL:

Trước khi build, cần đổi API URL trong frontend:

```powershell
cd C:\Projects\TEST_WEB_DEPLOY\frontend
```

**Tìm file config (có thể là `src/config.js` hoặc `src/api/config.js`):**

Đổi API URL thành:
```javascript
const API_URL = 'http://103.75.182.180:5000/api';
```

### Build frontend:

```powershell
npm install
npm run build
```

**Sau khi build xong, sẽ có thư mục `dist`**

---

## 7️⃣ SERVE FRONTEND

### Cài serve và chạy:

```powershell
npm install -g serve

cd C:\Projects\TEST_WEB_DEPLOY\frontend
pm2 start "serve -s dist -l 80" --name frontend
pm2 save
pm2 list
```

---

## 8️⃣ MỞ FIREWALL

```powershell
# Mở port 80 (Frontend)
New-NetFirewallRule -DisplayName "HTTP-80" -Direction Inbound -LocalPort 80 -Protocol TCP -Action Allow

# Mở port 5000 (Backend API)
New-NetFirewallRule -DisplayName "Backend-5000" -Direction Inbound -LocalPort 5000 -Protocol TCP -Action Allow
```

---

## 9️⃣ TRUY CẬP WEBSITE

### Từ máy tính của bạn:

- **Frontend:** http://103.75.182.180
- **Backend API:** http://103.75.182.180:5000/api

---

## 🔟 QUẢN LÝ PM2

```powershell
# Xem danh sách processes
pm2 list

# Xem logs
pm2 logs backend
pm2 logs frontend

# Restart
pm2 restart backend
pm2 restart frontend

# Stop
pm2 stop backend
pm2 stop frontend

# Xem thông tin chi tiết
pm2 show backend
```

---

## ✅ CHECKLIST

Đánh dấu khi hoàn thành:

- [ ] Kết nối VPS qua Remote Desktop (103.75.182.180)
- [ ] Cài Node.js (node -v để kiểm tra)
- [ ] Cài Git (git --version để kiểm tra)
- [ ] Cài MySQL (mysql --version để kiểm tra)
- [ ] Clone code từ GitHub
- [ ] Tạo database `fastfood_db`
- [ ] Tạo file `.env` trong backend
- [ ] Cài dependencies backend (npm install)
- [ ] Cài PM2
- [ ] Start backend với PM2
- [ ] Build frontend (npm run build)
- [ ] Serve frontend với PM2
- [ ] Mở firewall ports (80, 5000)
- [ ] Truy cập http://103.75.182.180 thành công

---

## 🆘 NẾU GẶP LỖI

### Backend không start:
```powershell
cd C:\Projects\TEST_WEB_DEPLOY\backend
pm2 logs backend
```

### Frontend không hiển thị:
```powershell
cd C:\Projects\TEST_WEB_DEPLOY\frontend
dir dist
pm2 logs frontend
```

### Không truy cập được từ internet:
- Kiểm tra Windows Firewall
- Kiểm tra firewall của nhà cung cấp VPS (hosting panel)

