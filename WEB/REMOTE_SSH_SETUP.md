# 🚀 HƯỚNG DẪN REMOTE-SSH CHO WINDOWS VPS

## ✅ File SSH Config đã được cấu hình tại:
`C:\Users\ASUS\.ssh\config`

---

## 📋 CÁCH SỬ DỤNG REMOTE-SSH TRONG CURSOR/VS CODE

### **1. Cài đặt Remote-SSH Extension:**
1. Mở Cursor/VS Code
2. Vào **Extensions** (Ctrl+Shift+X)
3. Tìm: **Remote - SSH**
4. Cài đặt extension từ **Microsoft**

---

### **2. Kết nối vào VPS:**

**Cách 1: Qua Command Palette**
1. Nhấn `Ctrl+Shift+P`
2. Gõ: `Remote-SSH: Connect to Host`
3. Chọn: `vps`
4. Nhập password khi được hỏi

**Cách 2: Qua Status Bar**
1. Nhìn góc dưới bên trái của Cursor/VS Code
2. Click vào biểu tượng `><` (Remote)
3. Chọn: `Connect to Host...`
4. Chọn: `vps`

---

### **3. Mở Folder trên VPS:**
Sau khi kết nối thành công:
1. `Ctrl+Shift+P`
2. Gõ: `Remote-SSH: Open Folder`
3. Chọn thư mục: `/c/Projects/TEST_WEB_DEPLOY` hoặc `C:\Projects\TEST_WEB_DEPLOY`

---

## ⚠️ LƯU Ý VỚI WINDOWS VPS:

Remote-SSH hoạt động tốt nhất với **Linux servers**. Với Windows VPS, bạn có thể gặp một số hạn chế:

### **Alternative Options:**

**Option 1: Remote Desktop (RDP)** - Được khuyên dùng cho Windows
- Đã được cấu hình sẵn
- Sử dụng: `mstsc /v:103.75.182.180 /u:Administrator`

**Option 2: WSL (Windows Subsystem for Linux) trên VPS**
- Cài đặt WSL trên VPS
- Sau đó dùng Remote-SSH vào WSL

**Option 3: Git + Local Development**
- Làm việc trên máy local
- Push code lên GitHub
- Pull code trên VPS khi cần

---

## 🔧 TEST KẾT NỐI:

**Trước khi dùng Remote-SSH, test bằng terminal:**
```powershell
ssh vps "hostname"
```

Nếu lệnh này chạy được, Remote-SSH cũng sẽ hoạt động!

---

## 📝 LỆNH NHANH:

**Kết nối SSH từ terminal:**
```powershell
ssh vps
```

**Chạy lệnh trên VPS:**
```powershell
ssh vps "cd C:\Projects\TEST_WEB_DEPLOY\backend && python manage.py runserver 0.0.0.0:5000"
```

---

## 🎯 BẠN MUỐN LÀM GÌ?

1. **Chỉnh sửa code trên VPS?** → Dùng Remote Desktop hoặc Git
2. **Chạy lệnh trên VPS?** → Dùng SSH từ terminal
3. **Full development trên VPS?** → Cài WSL trên VPS rồi dùng Remote-SSH

