# 🔧 SỬA LỖI CUỐI CÙNG: Publish Directory

## ✅ **TIN TỐT: Build đã thành công!**

Build logs cho thấy:
```
✓ built in 1.84s
dist/index.html
dist/assets/index-DaG3_VF5.css
dist/assets/index-32lNDftX.js
```

## ❌ **VẤN ĐỀ CUỐI CÙNG:**

Netlify không tìm được publish directory:
```
Deploy directory 'dist' does not exist
publish: /opt/build/repo/dist
```

**Nguyên nhân:** Publish directory trong UI đang là relative to root, không phải base directory.

---

## ✅ **GIẢI PHÁP: Tạo netlify.toml ở ROOT**

Đã tạo file `netlify.toml` ở **root project** (bên cạnh `docker-compose.yml`) với cấu hình đúng.

### **Cách làm:**

1. **File đã được tạo:** `netlify.toml` ở root
   - Cấu hình `base = "frontend"`
   - Cấu hình `publish = "frontend/dist"` (absolute path từ root)

2. **Trong Netlify UI:**
   - Vào **"Site settings"** → **"Build & deploy"** → **"Build settings"**
   - **XÓA HẾT** tất cả các field:
     - Base directory: *(để trống)*
     - Build command: *(để trống)*
     - Publish directory: *(để trống)*
   - Click **"Save"**
   - Netlify sẽ tự đọc từ file `netlify.toml` ở root

3. **Commit và push:**
   ```powershell
   git add netlify.toml
   git commit -m "Add netlify.toml at root for proper build config"
   git push
   ```

4. **Hoặc trigger deploy thủ công:**
   - Vào **"Deploys"** → **"Trigger deploy"** → **"Clear cache and deploy site"**

---

## 📝 **GIẢI THÍCH:**

**File `netlify.toml` ở root sẽ:**
- ✅ Được Netlify đọc đầu tiên
- ✅ Override tất cả UI settings
- ✅ Cấu hình đúng paths từ root:
  - `base = "frontend"` → Netlify cd vào frontend/
  - `publish = "frontend/dist"` → Netlify tìm ở frontend/dist từ root

---

## 🔍 **KIỂM TRA:**

Sau khi deploy, build logs phải hiển thị:
```
Publishing to directory: frontend/dist
✓ Deploy succeeded!
```

---

## ✅ **CẤU HÌNH CUỐI CÙNG:**

| Cấu hình | Giá trị |
|----------|---------|
| **Base directory (UI)** | *(trống - đọc từ netlify.toml)* |
| **Build command (UI)** | *(trống - đọc từ netlify.toml)* |
| **Publish directory (UI)** | *(trống - đọc từ netlify.toml)* |
| **netlify.toml (root)** | `base = "frontend"`<br>`publish = "frontend/dist"` |

---

**Commit và push file `netlify.toml` lên Git, hoặc để trống tất cả trong UI!** 🚀

