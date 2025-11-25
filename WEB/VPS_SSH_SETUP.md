# Cài Đặt SSH Trên Windows VPS

## 📋 Tổng Quan

Cài đặt OpenSSH Server trên Windows Server 2019 để kết nối qua SSH thay vì Remote Desktop.

---

## 1️⃣ CÀI ĐẶT SSH SERVER (Trên VPS)

### Bước 1: Truy Cập VPS Qua VNC Console

1. Đăng nhập **Control Panel** của nhà cung cấp VPS
2. Tìm VPS → Click **"Console"** hoặc **"VNC Console"**
3. Login:
   - User: `Administrator`
   - Password: `Thang2004`

### Bước 2: Mở PowerShell (Admin)

- Nhấn `Windows + X`
- Chọn **"Windows PowerShell (Admin)"**

### Bước 3: Cài OpenSSH Server

Copy và paste các lệnh sau:

```powershell
# Kiểm tra OpenSSH có sẵn chưa
Get-WindowsCapability -Online | Where-Object Name -like 'OpenSSH*'

# Cài OpenSSH Server
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

# Cài OpenSSH Client (nếu chưa có)
Add-WindowsCapability -Online -Name OpenSSH.Client~~~~0.0.1.0
```

### Bước 4: Start SSH Service

```powershell
# Start SSH service
Start-Service sshd

# Set SSH tự động chạy khi khởi động
Set-Service -Name sshd -StartupType 'Automatic'

# Verify
Get-Service sshd
```

### Bước 5: Mở Firewall Port 22

```powershell
# Mở port 22 cho SSH
New-NetFirewallRule -Name "SSH-Inbound" -DisplayName "SSH (Port 22)" -Direction Inbound -LocalPort 22 -Protocol TCP -Action Allow

# Verify
Get-NetFirewallRule -Name "SSH-Inbound"
```

### Bước 6: Cấu Hình SSH (Optional)

```powershell
# Cho phép password authentication
notepad C:\ProgramData\ssh\sshd_config
```

Trong file `sshd_config`, đảm bảo các dòng sau:

```
PasswordAuthentication yes
PubkeyAuthentication yes
PermitRootLogin yes
```

Lưu và đóng Notepad.

### Bước 7: Restart SSH Service

```powershell
Restart-Service sshd
```

---

## 2️⃣ KẾT NỐI SSH TỪ MÁY LOCAL

### Từ Windows PowerShell:

```powershell
ssh Administrator@103.75.182.180
```

Khi được hỏi password, nhập: `Thang2004`

### Lần đầu kết nối:

Sẽ có thông báo:
```
The authenticity of host '103.75.182.180' can't be established.
Are you sure you want to continue connecting (yes/no)?
```

Gõ: `yes` và nhấn Enter.

---

## 3️⃣ SETUP SSH KEY (Không Cần Password)

### Trên Máy Local:

#### Kiểm tra SSH key có sẵn:

```powershell
Test-Path ~/.ssh/id_rsa.pub
```

#### Nếu chưa có, tạo mới:

```powershell
ssh-keygen -t rsa -b 4096 -C "vps-key"
```

Nhấn Enter cho tất cả prompts (không đặt passphrase).

#### Copy public key:

```powershell
Get-Content ~/.ssh/id_rsa.pub
```

### Trên VPS (Qua SSH):

```powershell
# Tạo thư mục .ssh
mkdir C:\Users\Administrator\.ssh

# Tạo file authorized_keys
notepad C:\Users\Administrator\.ssh\authorized_keys
```

**Paste public key** (từ máy local) vào file này, lưu và đóng.

#### Set permissions:

```powershell
icacls C:\Users\Administrator\.ssh\authorized_keys /inheritance:r
icacls C:\Users\Administrator\.ssh\authorized_keys /grant "SYSTEM:(F)"
icacls C:\Users\Administrator\.ssh\authorized_keys /grant "BUILTIN\Administrators:(F)"
```

#### Restart SSH:

```powershell
Restart-Service sshd
```

### Test SSH Key:

Từ máy local:

```powershell
ssh Administrator@103.75.182.180
```

**Không cần nhập password nữa!**

---

## 4️⃣ LỆNH SSH THƯỜNG DÙNG

### Kết nối VPS:
```powershell
ssh Administrator@103.75.182.180
```

### Copy file lên VPS:
```powershell
scp local_file.txt Administrator@103.75.182.180:C:\destination\
```

### Copy file từ VPS về:
```powershell
scp Administrator@103.75.182.180:C:\source\file.txt ./
```

### Chạy lệnh từ xa:
```powershell
ssh Administrator@103.75.182.180 "powershell -Command Get-Process"
```

---

## ✅ CHECKLIST

- [ ] Truy cập VPS qua VNC Console
- [ ] Cài OpenSSH Server
- [ ] Start SSH service
- [ ] Set SSH auto-start
- [ ] Mở firewall port 22
- [ ] Test kết nối SSH từ máy local
- [ ] (Optional) Setup SSH key

---

## 🆘 TROUBLESHOOTING

### SSH không kết nối được:

```powershell
# Kiểm tra SSH service trên VPS
Get-Service sshd

# Kiểm tra port 22
netstat -an | findstr ":22"

# Kiểm tra firewall
Get-NetFirewallRule -Name "SSH-Inbound"
```

### Permission denied:

- Kiểm tra password đúng chưa
- Kiểm tra `PasswordAuthentication yes` trong `sshd_config`

### Connection timeout:

- Kiểm tra firewall của nhà cung cấp VPS (security group)
- Kiểm tra port 22 có được mở không

---

## 📝 GHI CHÚ

- **Port SSH:** 22 (mặc định)
- **Port RDP:** 3389
- **SSH an toàn hơn và nhẹ hơn Remote Desktop**
- **Nên dùng SSH key thay vì password**

