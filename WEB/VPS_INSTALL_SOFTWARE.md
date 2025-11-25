# Cài Đặt Phần Mềm Trên VPS Windows

## ⚡ CÁCH 1: Dùng Chocolatey (Nhanh Nhất) - KHUYẾN NGHỊ

### Bước 1: Cài Chocolatey

Mở **PowerShell (Admin)** trên VPS và chạy:

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

Đợi cài đặt xong (khoảng 1-2 phút).

### Bước 2: Cài Node.js, Git, MySQL

```powershell
# Cài Node.js
choco install nodejs-lts -y

# Cài Git
choco install git -y

# Cài MySQL
choco install mysql -y
```

### Bước 3: Đóng và Mở Lại PowerShell

Sau khi cài xong, **đóng PowerShell và mở lại** (để load PATH mới).

### Bước 4: Verify

```powershell
node -v
npm -v
git --version
mysql --version
```

---

## 🖱️ CÁCH 2: Download & Cài Thủ Công

Nếu Chocolatey không hoạt động, tải và cài thủ công:

### A. Node.js

1. **Mở trình duyệt trên VPS**
2. **Truy cập:** https://nodejs.org/en/download/
3. **Download:** Windows Installer (.msi) 64-bit - LTS
4. **Chạy file .msi** → Next → Next → Install
5. **Restart PowerShell**
6. **Test:** `node -v`

### B. Git

1. **Truy cập:** https://git-scm.com/download/win
2. **Download:** Git for Windows (64-bit)
3. **Chạy installer** → Next → Next → Install
4. **Restart PowerShell**
5. **Test:** `git --version`

### C. MySQL

1. **Truy cập:** https://dev.mysql.com/downloads/installer/
2. **Download:** mysql-installer-community (Windows)
3. **Chọn:** "No thanks, just start my download"
4. **Chạy installer:**
   - Setup Type: **Developer Default**
   - Root Password: **Thang2004** (GHI NHỚ PASSWORD NÀY!)
   - Finish installation
5. **Restart PowerShell**
6. **Test:** `mysql --version`

---

## ✅ SAU KHI CÀI XONG

Đóng và mở lại PowerShell, rồi chạy:

```powershell
node -v
npm -v
git --version
mysql --version
```

Tất cả đều phải hiển thị version!

---

## 🚀 TIẾP THEO

Sau khi cài xong phần mềm, tiếp tục với:
- Clone code từ GitHub
- Setup database
- Deploy backend & frontend

