# 🔧 SỬA LỖI: Deploy directory 'dist' does not exist

## ✅ **TIN TỐT: Build đã thành công!**

Build đã chạy thành công:
```
✓ built in 1.84s
dist/index.html
dist/assets/index-DaG3_VF5.css
dist/assets/index-32lNDftX.js
```

## ❌ **VẤN ĐỀ:**

Netlify đang tìm publish directory sai:
- ✅ Build output ở: `frontend/dist/`
- ❌ Netlify tìm ở: `/opt/build/repo/dist` (SAI!)

**Lỗi:**
```
Deploy directory 'dist' does not exist
publish: /opt/build/repo/dist
```

---

## ✅ **GIẢI PHÁP:**

Vấn đề là **Publish directory trong UI** không match với Base directory. Có 3 cách sửa:

---

### **CÁCH 1: Để trống Publish directory (KHUYÊN DÙNG)**

1. Vào **"Site settings"** → **"Build & deploy"** → **"Build settings"**
2. **Publish directory:** ⬅️ **XÓA HẾT, ĐỂ TRỐNG** (không gõ gì)
3. File `frontend/netlify.toml` đã có `publish = "dist"` ✅
4. Netlify sẽ tự đọc từ file config
5. Click **"Save"**
6. Trigger deploy mới

---

### **CÁCH 2: Sửa Publish directory = `dist` (relative)**

1. Vào **"Site settings"** → **"Build & deploy"** → **"Build settings"**
2. **Publish directory:** Gõ chỉ `dist` (4 ký tự, không có `frontend/`)
   - Nếu UI tự thêm `frontend/`, thử xóa và gõ lại
3. Click **"Save"**
4. Trigger deploy mới

---

### **CÁCH 3: Sửa Base directory về root (Nếu 2 cách trên không được)**

1. **Base directory:** XÓA `frontend` (để trống)
2. **Build command:** Đổi thành: `cd frontend && npm install && npm run build`
3. **Publish directory:** Đổi thành: `frontend/dist`
4. Click **"Save"**
5. Trigger deploy mới

---

## 🔍 **GIẢI THÍCH:**

Khi **Base directory = `frontend`**:
- Netlify `cd` vào `frontend/`
- Build command chạy trong `frontend/`
- Output tạo ở `frontend/dist/`
- Publish directory phải là `dist` (relative to `frontend/`)

Nhưng UI có thể đang hiểu Publish directory là relative to root, không phải base directory.

---

## ✅ **KIỂM TRA SAU KHI SỬA:**

Sau khi deploy, trong build logs phải thấy:
```
Publishing to directory: frontend/dist
```

Hoặc nếu base directory = `frontend`:
```
Publishing to directory: dist (relative to frontend/)
```

---

## 📝 **CẤU HÌNH ĐÚNG:**

| Cấu hình | Base directory | Build command | Publish directory |
|----------|---------------|---------------|-------------------|
| **Cách 1** | `frontend` | `cd frontend && npm install && npm run build` | *(trống - dùng netlify.toml)* |
| **Cách 2** | `frontend` | `cd frontend && npm install && npm run build` | `dist` |
| **Cách 3** | *(trống)* | `cd frontend && npm install && npm run build` | `frontend/dist` |

---

**Thử Cách 1 trước (để trống Publish directory). Nếu không được, thử Cách 2 hoặc Cách 3!** 🚀

