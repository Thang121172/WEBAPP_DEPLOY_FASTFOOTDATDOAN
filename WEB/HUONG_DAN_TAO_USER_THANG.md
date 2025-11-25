# 👤 HƯỚNG DẪN TẠO USER "THANG" VÀ SSH TỰ ĐỘNG (KHÔNG CẦN PASSWORD)

## 🎯 **MỤC TIÊU:**
- ✅ Tạo user "thang" với password "Thang2004"
- ✅ SSH vào VPS với user "thang" **KHÔNG CẦN NHẬP PASSWORD** (dùng SSH key tự động)

---

## 📋 **CÁCH 1: CHẠY SCRIPT TỰ ĐỘNG (KHUYÊN DÙNG)**

### **Trên máy local của bạn:**
```powershell
.\auto_setup_user_thang.ps1
```
**Khi được hỏi:** Nhập password của Administrator trên VPS

✅ Script sẽ tự động:
1. Tạo user "thang" với password "Thang2004"
2. Thêm SSH key vào user "thang"
3. Đặt quyền đúng

---

## 📋 **CÁCH 2: CHẠY TRỰC TIẾP TRÊN VPS**

### **Bước 1: Kết nối VPS qua Remote Desktop**
```powershell
mstsc /v:103.75.182.180 /u:Administrator
```

### **Bước 2: Mở PowerShell với quyền Administrator trên VPS**

### **Bước 3: Chạy các lệnh sau:**

```powershell
# 1. Tạo user thang
$securePassword = ConvertTo-SecureString "Thang2004" -AsPlainText -Force
New-LocalUser -Name "thang" -Password $securePassword -FullName "Thang" -Description "User for SSH access"
Add-LocalGroupMember -Group "Remote Desktop Users" -Member "thang"

# 2. Tạo thư mục .ssh
New-Item -ItemType Directory -Path "C:\Users\thang\.ssh" -Force

# 3. Thêm SSH public key vào authorized_keys
$publicKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIF7xdnL7PsInD8i8LRUnXbCDEzV0sWKACq/cZmXgrpkG github-ssh-key"
Set-Content -Path "C:\Users\thang\.ssh\authorized_keys" -Value $publicKey

# 4. Đặt quyền cho authorized_keys
$computerName = $env:COMPUTERNAME
icacls "C:\Users\thang\.ssh\authorized_keys" /inheritance:r /grant:r "${computerName}\thang`:F" /grant:r "Administrators:F"
```

---

## ✅ **SAU KHI HOÀN TẤT:**

### **Test kết nối SSH (không cần password):**
```powershell
ssh vps-thang
```

**Hoặc:**
```powershell
ssh thang@103.75.182.180
```

✅ **Lần đầu tiên SSH**, bạn sẽ thấy:
```
The authenticity of host '103.75.182.180' can't be established.
Are you sure you want to continue connecting (yes/no/[fingerprint])? 
```
→ Gõ `yes` và Enter

✅ **Sau đó, SSH sẽ tự động kết nối KHÔNG CẦN NHẬP PASSWORD!**

---

## 📝 **THÔNG TIN USER:**

- **Tên user:** `thang`
- **Password:** `Thang2004`
- **SSH key:** Đã được thêm ✅
- **SSH config:** Đã cấu hình (`vps-thang`)

---

## 🔧 **SSH CONFIG (đã có sẵn):**

File: `C:\Users\ASUS\.ssh\config`

```ssh-config
Host vps-thang
    HostName 103.75.182.180
    User thang
    Port 22
    IdentityFile ~/.ssh/id_ed25519
    ServerAliveInterval 60
    ServerAliveCountMax 3
```

---

## ⚠️ **LƯU Ý:**

1. ✅ SSH key đã được thêm vào user "thang"
2. ✅ SSH sẽ tự động dùng key, **KHÔNG CẦN NHẬP PASSWORD**
3. ✅ Nếu vẫn bị hỏi password, kiểm tra lại:
   - SSH key đã được thêm vào `authorized_keys` chưa?
   - Quyền của file `authorized_keys` có đúng không?

---

## 🚀 **BẠN MUỐN CHẠY CÁCH NÀO?**

**Chọn 1 trong 2 cách:**
1. Chạy script tự động: `.\auto_setup_user_thang.ps1`
2. Chạy thủ công trên VPS qua Remote Desktop

