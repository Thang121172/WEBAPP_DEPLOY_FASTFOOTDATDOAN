# 🔧 Tạo file .env.local cho Frontend (Local Development)

## 📝 Hướng dẫn tạo file `.env.local`

### Bước 1: Vào folder frontend
```bash
cd frontend
```

### Bước 2: Tạo file `.env.local`

**Windows (PowerShell):**
```powershell
@"
VITE_API_BASE=https://fastfood-backend-t8jz.onrender.com/api
"@ | Out-File -FilePath .env.local -Encoding utf8
```

**Hoặc tạo file thủ công:**
1. Mở Notepad hoặc VS Code
2. Gõ nội dung:
   ```
   VITE_API_BASE=https://fastfood-backend-t8jz.onrender.com/api
   ```
3. Save as: `.env.local` (có dấu chấm ở đầu!)
4. Lưu vào folder `frontend/`

### Bước 3: Kiểm tra file đã tạo

File phải ở: `frontend/.env.local`

Nội dung:
```
VITE_API_BASE=https://fastfood-backend-t8jz.onrender.com/api
```

⚠️ **Lưu ý:**
- KHÔNG có dấu `/` ở cuối URL
- KHÔNG có khoảng trắng thừa
- File bắt đầu bằng dấu chấm (`.env.local`)

### Bước 4: Restart dev server

1. Dừng server hiện tại (Ctrl + C)
2. Chạy lại:
   ```bash
   npm run dev
   ```

### Bước 5: Test

1. Mở browser: `http://localhost:5173`
2. Mở Console (F12)
3. Chạy lệnh:
   ```javascript
   console.log('API_BASE:', import.meta.env.VITE_API_BASE);
   ```
4. Phải hiển thị: `https://fastfood-backend-t8jz.onrender.com/api`

---

## ✅ Đảm bảo .env.local không bị commit

Kiểm tra file `frontend/.gitignore` có dòng này chưa:
```
.env.local
.env*.local
```

Nếu chưa có, thêm vào!

