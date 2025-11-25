# 🔧 SỬA LỖI: KHÔNG XÓA ĐƯỢC "frontend" TRONG PUBLISH DIRECTORY

## ⚠️ **VẤN ĐỀ:**

Trên Netlify UI, bạn không thể xóa `frontend/` trong **Publish directory**. Có thể do UI tự động thêm prefix.

---

## ✅ **GIẢI PHÁP: ĐỂ TRỐNG VÀ DÙNG FILE NETLIFY.TOML**

### **Cách làm:**

1. **Trong Netlify UI - Build settings:**
   - **Base directory:** `frontend` ✅ (GIỮ NGUYÊN)
   - **Build command:** `npm run build` ✅ (GIỮ NGUYÊN)
   - **Publish directory:** ⬅️ **ĐỂ TRỐNG HOÀN TOÀN** (xóa hết nội dung, không gõ gì cả)

2. **Netlify sẽ tự động đọc từ file `frontend/netlify.toml`:**
   - File này đã có sẵn trong project
   - Đã cấu hình `publish = "dist"` ✅
   - Netlify sẽ ưu tiên đọc file này hơn UI settings

3. **Click "Save"**

4. **Trigger deploy mới:**
   - Vào tab **"Deploys"**
   - Click **"Trigger deploy"** → **"Clear cache and deploy site"**

---

## 🔍 **KIỂM TRA:**

Sau khi deploy, vào build logs và xem:

```
✅ Publishing to directory: dist
```

Nếu thấy điều này → **ĐÚNG!**

Nếu thấy:
```
❌ Directory frontend/dist does not exist
```
→ Cần kiểm tra lại cấu hình

---

## 📝 **TẠI SAO CÁCH NÀY HOẠT ĐỘNG?**

1. Khi **Base directory = `frontend`**, Netlify sẽ:
   - `cd` vào folder `frontend/`
   - Tìm file `netlify.toml` trong đó
   - Đọc cấu hình `publish = "dist"` từ file
   - Build và publish từ `frontend/dist/`

2. File `netlify.toml` có **ưu tiên cao hơn** UI settings, nên nếu UI không cho sửa, file config sẽ override.

---

## 🚨 **NẾU VẪN KHÔNG ĐƯỢC:**

### **Giải pháp dự phòng:**

1. **Xóa Base directory tạm thời:**
   - Trong UI, xóa `frontend` trong **Base directory** (để trống)
   - **Build command** đổi thành: `cd frontend && npm run build`
   - **Publish directory** đổi thành: `frontend/dist`
   - Click **"Save"**
   - Test deploy

2. **Hoặc di chuyển netlify.toml lên root:**
   - Tạo file `netlify.toml` ở root project (bên cạnh `docker-compose.yml`)
   - Cấu hình:
   ```toml
   [build]
     base = "frontend"
     command = "npm run build"
     publish = "frontend/dist"
   ```
   - Trong UI, để trống tất cả các field

---

## ✅ **TÓM TẮT CẤU HÌNH ĐÚNG:**

| Cách | Base directory | Build command | Publish directory |
|------|---------------|---------------|-------------------|
| **Cách 1 (Khuyên dùng)** | `frontend` | `npm run build` | **ĐỂ TRỐNG** (dùng netlify.toml) |
| **Cách 2 (Dự phòng)** | *(trống)* | `cd frontend && npm run build` | `frontend/dist` |

---

**Thử cách 1 trước (để trống Publish directory). Nếu không được, dùng cách 2!** 🚀
