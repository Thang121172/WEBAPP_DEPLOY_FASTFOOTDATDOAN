# 🔧 SỬA LỖI BUILD: Cannot find package 'vite'

## ❌ **LỖI:**

```
Error [ERR_MODULE_NOT_FOUND]: Cannot find package 'vite' imported from /opt/build/repo/frontend/vite.config.ts
```

## 🔍 **NGUYÊN NHÂN:**

Netlify chạy build command `cd frontend && npm run build` nhưng:
- ❌ Không chạy `npm install` trong folder `frontend/`
- ❌ Dependencies chưa được cài đặt
- ❌ Package `vite` không tồn tại khi build

## ✅ **GIẢI PHÁP:**

### **Cách 1: Sửa Build Command trong UI (KHUYÊN DÙNG)**

1. Vào **"Site settings"** → **"Build & deploy"** → **"Build settings"**
2. Sửa **Build command** thành:
   ```
   cd frontend && npm install && npm run build
   ```
3. Click **"Save"**
4. Trigger deploy mới

---

### **Cách 2: Sửa file netlify.toml (ĐÃ CẬP NHẬT)**

File `frontend/netlify.toml` đã được cập nhật:
```toml
[build]
  command = "npm install && npm run build"
```

1. Commit và push file này lên Git
2. Netlify sẽ tự động đọc cấu hình mới

---

## 📝 **GIẢI THÍCH:**

**Build command cũ (SAI):**
```bash
cd frontend && npm run build
```
→ Chỉ chạy build, không install dependencies

**Build command mới (ĐÚNG):**
```bash
cd frontend && npm install && npm run build
```
→ Install dependencies trước, sau đó mới build

---

## ✅ **SAU KHI SỬA:**

1. **Nếu dùng Cách 1 (UI):**
   - Sửa Build command trong UI
   - Click "Save"
   - Vào "Deploys" → "Trigger deploy"

2. **Nếu dùng Cách 2 (file config):**
   - Commit và push `frontend/netlify.toml`
   - Netlify sẽ tự động deploy

---

## 🔍 **KIỂM TRA:**

Sau khi deploy, trong build logs phải thấy:

```
1. Installing dependencies
2. Building site...
3. ✅ Build completed successfully
```

---

**Sửa xong và trigger deploy lại nhé!** 🚀

