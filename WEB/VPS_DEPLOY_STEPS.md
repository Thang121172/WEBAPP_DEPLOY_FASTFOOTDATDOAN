# Deploy Full-Stack Lên VPS - Từng Bước

## 📋 CHẠY CÁC LỆNH SAU TRÊN VPS (Remote Desktop)

Mở **PowerShell** (không cần Admin) trên VPS và chạy từng lệnh:

---

## BƯỚC 1: KIỂM TRA PHẦN MỀM

```powershell
node -v
git --version
mysql --version
```

**Nếu lệnh nào báo lỗi** → Cần cài phần mềm đó.

---

## BƯỚC 2: CÀI NODE.JS (Nếu chưa có)

### Download và cài:
1. Mở trình duyệt trên VPS
2. Truy cập: https://nodejs.org/en/download/
3. Download: **Windows Installer (.msi) 64-bit LTS**
4. Chạy file .msi → Next → Next → Install
5. Sau khi cài xong, **đóng và mở lại PowerShell**
6. Test: `node -v`

---

## BƯỚC 3: CÀI GIT (Nếu chưa có)

### Download và cài:
1. Truy cập: https://git-scm.com/download/win
2. Download Git for Windows
3. Chạy installer → Next → Next → Install
4. Sau khi cài xong, **đóng và mở lại PowerShell**
5. Test: `git --version`

---

## BƯỚC 4: CÀI MYSQL (Nếu chưa có)

### Download và cài:
1. Truy cập: https://dev.mysql.com/downloads/installer/
2. Download: **mysql-installer-community** (Windows)
3. Chạy installer:
   - Setup Type: **Developer Default**
   - Root Password: **Thang2004** (hoặc password bạn chọn - GHI NHỚ!)
4. Finish installation
5. Test: `mysql --version`

---

## BƯỚC 5: CẤU HÌNH GIT (Username/Token)

```powershell
# Cấu hình Git
git config --global user.name "Thang121172"
git config --global user.email "your_email@example.com"
git config --global credential.helper wincred
```

---

## BƯỚC 6: CLONE CODE TỪ GITHUB

```powershell
# Tạo thư mục Projects
cd C:\
mkdir Projects
cd Projects

# Clone code
git clone https://github.com/Thang121172/TEST_WEB_DEPLOY.git

# Vào thư mục
cd TEST_WEB_DEPLOY
dir
```

**Khi được hỏi username/password:**
- Username: `Thang121172`
- Password: `YOUR_GITHUB_TOKEN_HERE` (sử dụng Personal Access Token)

---

## BƯỚC 7: SETUP DATABASE

```powershell
# Mở MySQL Command Line hoặc MySQL Workbench
mysql -u root -p
```

**Nhập password MySQL** (đã đặt khi cài - ví dụ: `Thang2004`)

### Trong MySQL, chạy:

```sql
CREATE DATABASE fastfood_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SHOW DATABASES;
EXIT;
```

---

## BƯỚC 8: CẤU HÌNH BACKEND

```powershell
cd C:\Projects\TEST_WEB_DEPLOY\backend

# Tạo file .env
notepad .env
```

### Nội dung file .env:

```env
PORT=5000
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=Thang2004
DB_NAME=fastfood_db
JWT_SECRET=fastfood_secret_key_2024
NODE_ENV=production
```

**Lưu (Ctrl+S) và đóng Notepad**

### Cài dependencies:

```powershell
npm install
```

### Test backend:

```powershell
node server.js
```

**Mở trình duyệt trên VPS:** `http://localhost:5000`

Nếu OK → Nhấn `Ctrl + C` để stop

---

## BƯỚC 9: CÀI PM2 (Process Manager)

```powershell
npm install -g pm2
npm install -g pm2-windows-service

# Cài PM2 service
pm2-service-install
# Khi được hỏi, chọn default options (nhấn Enter)

# Start backend
cd C:\Projects\TEST_WEB_DEPLOY\backend
pm2 start server.js --name backend
pm2 save
pm2 list
```

---

## BƯỚC 10: CẤU HÌNH & BUILD FRONTEND

### Cấu hình API URL:

```powershell
cd C:\Projects\TEST_WEB_DEPLOY\frontend
notepad src\config.js
```

(Hoặc file config khác tùy project)

**Đổi API URL thành:**

```javascript
export const API_URL = 'http://103.75.182.180:5000/api';
```

Lưu và đóng.

### Build frontend:

```powershell
npm install
npm run build
```

---

## BƯỚC 11: SERVE FRONTEND

```powershell
npm install -g serve

cd C:\Projects\TEST_WEB_DEPLOY\frontend
pm2 start "serve -s dist -l 80" --name frontend
pm2 save
pm2 list
```

---

## BƯỚC 12: TRUY CẬP WEBSITE

### Từ máy tính của bạn (hoặc bất kỳ đâu):

- **Frontend:** http://103.75.182.180
- **Backend API:** http://103.75.182.180:5000/api

---

## 📊 QUẢN LÝ PM2

```powershell
# Xem danh sách
pm2 list

# Xem logs
pm2 logs backend
pm2 logs frontend

# Restart
pm2 restart backend
pm2 restart frontend

# Stop
pm2 stop backend
```

---

## ✅ HOÀN THÀNH!

Website của bạn đã online tại: **http://103.75.182.180**

