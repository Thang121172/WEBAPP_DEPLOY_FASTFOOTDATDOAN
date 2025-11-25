# 🔧 Sửa lỗi Build trên Vercel

## ❌ Lỗi:
```
Error: Command "cd frontend && npm install" exited with 1
```

## 🔍 Nguyên nhân:

Vercel đã được set **Root Directory** = `frontend` trong UI, nên khi chạy command có `cd frontend`, nó sẽ cố cd vào `frontend/frontend` → **Lỗi!**

## ✅ Giải pháp:

### Cách 1: Sửa vercel.json (Đã sửa)

File `vercel.json` đã được cập nhật:
- ❌ Cũ: `"buildCommand": "cd frontend && npm install && npm run build"`
- ✅ Mới: `"buildCommand": "npm install && npm run build"`

**Lý do:** Vì Root Directory đã là `frontend`, không cần `cd` nữa.

### Cách 2: Xóa vercel.json và để Vercel tự detect

1. Xóa file `vercel.json`
2. Trong Vercel UI, set:
   - **Root Directory:** `frontend`
   - **Build Command:** `npm install && npm run build`
   - **Output Directory:** `dist`

## 📝 Cấu hình Vercel đúng:

### Trong Vercel UI Settings:
- **Root Directory:** `frontend` ✅
- **Build Command:** `npm install && npm run build` ✅
- **Output Directory:** `dist` ✅

### Trong vercel.json (nếu dùng):
```json
{
  "buildCommand": "npm install && npm run build",
  "outputDirectory": "dist",
  "framework": "vite"
}
```

**Lưu ý:** KHÔNG có `cd frontend` vì Root Directory đã là `frontend`!

## 🔄 Sau khi sửa:

1. **Commit và push lại:**
   ```bash
   git add vercel.json
   git commit -m "Fix Vercel build command - remove cd frontend"
   git push origin main
   ```

2. **Vercel sẽ tự động deploy lại**

3. **Kiểm tra logs:** Xem build có thành công không

## 🆘 Nếu vẫn lỗi:

### Kiểm tra:
1. **Root Directory có đúng không?**
   - Phải là `frontend` (không có dấu `/`)

2. **package.json có tồn tại không?**
   - Phải ở: `frontend/package.json`

3. **Node version:**
   - Vercel tự detect, nhưng có thể set trong `package.json`:
   ```json
   "engines": {
     "node": ">=18.0.0"
   }
   ```

### Xem logs chi tiết trên Vercel:
1. Vào Vercel Dashboard
2. Chọn deployment
3. Xem **Build Logs** để biết lỗi cụ thể

