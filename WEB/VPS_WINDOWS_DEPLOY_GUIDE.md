# Hướng Dẫn Deploy Full-Stack Lên Windows VPS

## 📋 Tổng Quan

Deploy React + Node.js + MySQL lên Windows Server 2019

---

## 1️⃣ KẾT NỐI VPS

### Windows (Remote Desktop):

1. **Nhấn Windows + R**
2. **Gõ:** `mstsc`
3. **Nhập:**
   - Computer: `IP_VPS_của_bạn`
   - Username: `Administrator`
   - Password: `password_đã_nhận`

### Hoặc qua PowerShell:

```powershell
mstsc /v:IP_VPS_của_bạn
```

---

## 2️⃣ CÀI ĐẶT MÔI TRƯỜNG (Trên VPS)

### A. Cài Node.js

1. **Download Node.js:**
   - Truy cập: https://nodejs.org/en/download/
   - Chọn: **Windows Installer (.msi)** - LTS version
   - Download và chạy installer

2. **Verify:**
   ```powershell
   node -v
   npm -v
   ```

### B. Cài Git

1. **Download Git:**
   - Truy cập: https://git-scm.com/download/win
   - Download và cài đặt

2. **Verify:**
   ```powershell
   git --version
   ```

### C. Cài MySQL

1. **Download MySQL:**
   - Truy cập: https://dev.mysql.com/downloads/installer/
   - Chọn: **MySQL Installer for Windows**
   - Download và chạy installer

2. **Chọn setup type:** `Developer Default`

3. **Đặt root password:** (Ghi nhớ password này!)

4. **Verify:**
   ```powershell
   mysql --version
   ```

### D. Cài PM2 (Process Manager)

```powershell
npm install -g pm2
npm install -g pm2-windows-service
pm2-service-install
```

---

## 3️⃣ CLONE CODE TỪ GITHUB

### Tạo thư mục project:

```powershell
cd C:\
mkdir Projects
cd Projects
```

### Clone repository:

```powershell
git clone https://github.com/Thang121172/TEST_WEB_DEPLOY.git
cd TEST_WEB_DEPLOY
```

---

## 4️⃣ SETUP DATABASE

### A. Tạo Database

1. **Mở MySQL Workbench** hoặc MySQL Command Line

2. **Tạo database:**
   ```sql
   CREATE DATABASE fastfood_db;
   ```

3. **Tạo user (optional):**
   ```sql
   CREATE USER 'fastfood_user'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON fastfood_db.* TO 'fastfood_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

### B. Import Database Schema

Nếu có file `.sql`:

```powershell
mysql -u root -p fastfood_db < path\to\database.sql
```

---

## 5️⃣ SETUP BACKEND

### A. Cấu hình Environment

1. **Tạo file `.env` trong thư mục `backend`:**

```powershell
cd C:\Projects\TEST_WEB_DEPLOY\backend
```

2. **Tạo file `.env`:**

```env
PORT=5000
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=your_mysql_password
DB_NAME=fastfood_db
JWT_SECRET=your_secret_key_here
NODE_ENV=production
```

### B. Cài Dependencies & Start

```powershell
npm install
```

### C. Test Backend:

```powershell
node server.js
```

Mở trình duyệt: `http://localhost:5000` - xem có chạy không.

### D. Chạy Backend với PM2 (auto restart):

```powershell
pm2 start server.js --name "backend"
pm2 save
pm2 startup
```

---

## 6️⃣ BUILD FRONTEND

### A. Cấu hình API URL

1. **Mở file `frontend/src/config.js` (hoặc tương tự):**

Đổi API URL thành IP VPS của bạn:

```javascript
export const API_URL = 'http://YOUR_VPS_IP:5000/api';
```

### B. Build Frontend:

```powershell
cd C:\Projects\TEST_WEB_DEPLOY\frontend
npm install
npm run build
```

**Thư mục `dist` sẽ chứa frontend đã build.**

---

## 7️⃣ SERVE FRONTEND

### Option 1: Dùng serve (đơn giản)

```powershell
npm install -g serve
cd C:\Projects\TEST_WEB_DEPLOY\frontend
pm2 start "serve -s dist -l 3000" --name "frontend"
pm2 save
```

### Option 2: Dùng IIS (chuyên nghiệp hơn)

1. **Cài IIS:**
   - Server Manager → Add Roles → Web Server (IIS)

2. **Cấu hình IIS:**
   - Tạo website mới
   - Point đến thư mục `C:\Projects\TEST_WEB_DEPLOY\frontend\dist`
   - Bind port 80

---

## 8️⃣ CẤU HÌNH FIREWALL

### Mở Ports:

```powershell
# Port 80 (HTTP)
New-NetFirewallRule -DisplayName "HTTP" -Direction Inbound -LocalPort 80 -Protocol TCP -Action Allow

# Port 3000 (Frontend)
New-NetFirewallRule -DisplayName "Frontend" -Direction Inbound -LocalPort 3000 -Protocol TCP -Action Allow

# Port 5000 (Backend)
New-NetFirewallRule -DisplayName "Backend" -Direction Inbound -LocalPort 5000 -Protocol TCP -Action Allow

# Port 3306 (MySQL) - CHỈ nếu cần remote access
New-NetFirewallRule -DisplayName "MySQL" -Direction Inbound -LocalPort 3306 -Protocol TCP -Action Allow
```

---

## 9️⃣ TRUY CẬP WEBSITE

### Truy cập từ internet:

- **Frontend:** `http://YOUR_VPS_IP:3000`
- **Backend API:** `http://YOUR_VPS_IP:5000/api`

---

## 🔟 CÀI ĐẶT DOMAIN (Nếu có)

### A. Point Domain về VPS

1. Vào DNS provider (tên miền)
2. Tạo **A Record:**
   - Host: `@` (hoặc `www`)
   - Value: `YOUR_VPS_IP`
   - TTL: 3600

### B. Đổi Port về 80

- Frontend chạy port 80 thay vì 3000
- Backend có thể giữ nguyên 5000

---

## 📊 QUẢN LÝ PM2

### Xem processes:
```powershell
pm2 list
```

### Xem logs:
```powershell
pm2 logs backend
pm2 logs frontend
```

### Restart:
```powershell
pm2 restart backend
pm2 restart frontend
```

### Stop:
```powershell
pm2 stop backend
pm2 stop frontend
```

---

## ✅ CHECKLIST

- [ ] Kết nối VPS qua Remote Desktop
- [ ] Cài Node.js
- [ ] Cài Git
- [ ] Cài MySQL
- [ ] Cài PM2
- [ ] Clone code từ GitHub
- [ ] Tạo database MySQL
- [ ] Cấu hình `.env` cho backend
- [ ] Start backend với PM2
- [ ] Build frontend
- [ ] Serve frontend với PM2 hoặc IIS
- [ ] Mở firewall ports
- [ ] Truy cập website qua IP VPS

---

## 🆘 TROUBLESHOOTING

### Backend không chạy:
```powershell
pm2 logs backend
```

### Frontend không hiển thị:
- Kiểm tra `dist` folder có file không
- Kiểm tra port 3000 có mở không

### Database connection error:
- Kiểm tra MySQL service có chạy không
- Kiểm tra credentials trong `.env`

### Không truy cập được từ internet:
- Kiểm tra firewall VPS
- Kiểm tra security group/firewall của nhà cung cấp VPS

---

## 📝 GHI CHÚ

- **Windows VPS khác Linux:** Dùng backslash `\` thay vì `/`
- **PM2 trên Windows:** Cần cài `pm2-windows-service`
- **MySQL:** Đặt password mạnh cho production
- **Security:** Không expose MySQL port ra internet

