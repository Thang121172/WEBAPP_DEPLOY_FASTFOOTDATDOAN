# Hướng dẫn Upload Project lên VPS

## Thông tin VPS
- **IP:** 103.75.182.180
- **Archive đã tạo:** `T:\FASTFOOD_project_20251126_021844.tar.gz`
- **Kích thước:** ~58.81 MB

## Các bước thực hiện

### Cách 1: Sử dụng script tự động (Khuyến nghị)

Chạy script upload:
```powershell
cd T:\FASTFOOD
.\upload_to_vps.ps1
```

Script sẽ hỏi:
- SSH username (mặc định: root)
- Đường dẫn đích trên VPS (mặc định: /root/)

### Cách 2: Upload thủ công bằng SCP

1. **Copy file lên VPS:**
```powershell
scp "T:\FASTFOOD_project_20251126_021844.tar.gz" root@103.75.182.180:/root/
```

Hoặc nếu dùng username khác:
```powershell
scp "T:\FASTFOOD_project_20251126_021844.tar.gz" your_username@103.75.182.180:/home/your_username/
```

2. **SSH vào VPS:**
```bash
ssh root@103.75.182.180
```

3. **Giải nén project:**
```bash
cd /root
tar -xzf FASTFOOD_project_20251126_021844.tar.gz
```

Project sẽ được giải nén thành thư mục `FASTFOOD/`

### Cách 3: Tạo lại archive và upload

Nếu cần tạo lại archive:
```powershell
cd T:\FASTFOOD
.\compress_and_upload.ps1
```

## Lưu ý

- Archive đã loại trừ: `node_modules`, `build`, `*.log`, `uploads`, `.next`, `dist`, `__pycache__`
- Sau khi upload, bạn cần cài đặt lại dependencies:
  - Backend: `cd FASTFOOD/APP/backend && npm install`
  - Web: `cd FASTFOOD/WEB && npm install` (nếu có)

## Kiểm tra sau khi upload

```bash
ssh root@103.75.182.180
cd /root/FASTFOOD
ls -la
```

