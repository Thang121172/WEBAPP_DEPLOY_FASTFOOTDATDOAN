# 👤 HƯỚNG DẪN TẠO USER "THANG" VÀ THÊM SSH KEY

## 📋 **CÁC BƯỚC:**

### **BƯỚC 1: Tạo user "thang" trên VPS**

**Cách 1: Chạy script trên VPS (qua Remote Desktop)**
1. Kết nối VPS qua Remote Desktop: `mstsc /v:103.75.182.180`
2. Mở PowerShell với quyền Administrator
3. Copy script `create_user_thang.ps1` và chạy:
   ```powershell
   .\create_user_thang.ps1
   ```

**Cách 2: Chạy trực tiếp lệnh trên VPS**
```powershell
$securePassword = ConvertTo-SecureString "Thang2004" -AsPlainText -Force
New-LocalUser -Name "thang" -Password $securePassword -FullName "Thang" -Description "User for SSH access"
Add-LocalGroupMember -Group "Remote Desktop Users" -Member "thang"
```

---

### **BƯỚC 2: Thêm SSH key vào user "thang"**

**Chạy script trên máy local của bạn:**
```powershell
.\add_ssh_key_to_user_thang.ps1
```

Script này sẽ:
- ✅ Đọc SSH key từ máy local: `C:\Users\ASUS\.ssh\id_ed25519.pub`
- ✅ Thêm key vào file `authorized_keys` của user "thang" trên VPS
- ✅ Đặt quyền truy cập đúng

---

### **BƯỚC 3: Test kết nối SSH với user "thang"**

**Sau khi hoàn thành Bước 1 & 2, test kết nối:**
```powershell
ssh vps-thang
```

**Hoặc:**
```powershell
ssh thang@103.75.182.180
```

---

## ✅ **SSH CONFIG ĐÃ ĐƯỢC CẤU HÌNH:**

File `C:\Users\ASUS\.ssh\config` đã có 2 entry:

1. **`vps`** - Kết nối với user Administrator
2. **`vps-thang`** - Kết nối với user thang (mới thêm)

---

## 📝 **THÔNG TIN USER:**

- **Tên user:** `thang`
- **Password:** `Thang2004`
- **SSH key:** Đã được cấu hình

---

## 🚀 **SAU KHI HOÀN TẤT:**

Bạn có thể:
- ✅ SSH vào VPS bằng user "thang" mà không cần password
- ✅ Sử dụng Remote Desktop với user "thang" (nếu cần)

---

## ⚠️ **LƯU Ý:**

1. **Chạy Bước 1 trên VPS** (qua Remote Desktop hoặc SSH với Administrator)
2. **Chạy Bước 2 trên máy local** của bạn
3. **SSH config đã được cập nhật**, bạn có thể dùng `ssh vps-thang` ngay sau khi hoàn thành

