# Kết Nối VPS - 103.75.182.180

## 🔑 Thông Tin Kết Nối

- **IP:** 103.75.182.180
- **Username:** Administrator (hoặc username nhà cung cấp đã cho)
- **Password:** (Cần lấy từ email/panel nhà cung cấp VPS)

---

## 🖥️ Cách Kết Nối

### Option 1: Remote Desktop (Khuyến nghị)

1. **Nhấn:** `Windows + R`
2. **Gõ:** `mstsc`
3. **Nhập:**
   - Computer: `103.75.182.180`
   - Username: `Administrator`
4. **Click Connect** → Nhập password

### Option 2: Từ PowerShell

```powershell
mstsc /v:103.75.182.180
```

---

## ✅ Sau Khi Kết Nối Thành Công

Chạy các lệnh sau trong PowerShell trên VPS:

```powershell
# Kiểm tra hệ điều hành
systeminfo | findstr /B /C:"OS Name" /C:"OS Version"

# Kiểm tra RAM
systeminfo | findstr /C:"Total Physical Memory"

# Kiểm tra disk
Get-PSDrive C
```

