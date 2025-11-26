# Hướng dẫn sửa lỗi Render Docker

## Lỗi hiện tại:
```
Service Root Directory "/opt/render/project/src/backend" is missing.
error: invalid local: resolve : lstat /opt/render/project/src/backend: no such file or directory
```

## Nguyên nhân:
- Service trên Render đang được cấu hình với **Root Directory = "backend"**
- Hoặc service đang dùng **Python buildpack** thay vì **Docker**

## Giải pháp:

### Cách 1: Sửa trên Render Dashboard (Khuyến nghị)

1. **Vào Render Dashboard** → Service `fastfood-backend`
2. **Vào Settings** (bánh răng ⚙️)
3. **Kiểm tra và sửa các cấu hình sau:**

   #### Environment:
   - Phải chọn: **Docker** (KHÔNG phải Python)
   - Nếu đang là Python, click **Change** và chọn **Docker**

   #### Root Directory:
   - **Để TRỐNG** hoặc nhập: `.` 
   - **KHÔNG** được là `backend`

   #### Docker Settings:
   - **Dockerfile Path**: `./Dockerfile`
   - **Docker Context**: `.`

4. **Click "Save Changes"**
5. **Manual Deploy** → "Clear build cache & deploy"

---

### Cách 2: Xóa và tạo lại từ Blueprint

1. **Xóa service hiện tại:**
   - Vào service → Settings → Danger Zone → Delete Service

2. **Tạo lại từ Blueprint:**
   - Vào Dashboard → New → Blueprint
   - Connect repository GitHub
   - Render sẽ tự động detect `render.yaml`
   - Chọn branch và deploy

---

### Cách 3: Kiểm tra render.yaml

Đảm bảo file `render.yaml` ở root có:
```yaml
services:
  - type: web
    name: fastfood-backend
    env: docker  # ← Phải là "docker"
    dockerfilePath: ./Dockerfile
    dockerContext: .  # ← Context là root, không phải backend
```

---

## Sau khi sửa:

1. Render sẽ build Docker image từ `Dockerfile` ở root
2. Build context sẽ là root directory (`.`)
3. Dockerfile sẽ copy `backend/` vào container
4. Service sẽ chạy với `start.sh` script

---

## Kiểm tra:

Sau khi deploy, check logs:
- Phải thấy: `Building Docker image...`
- KHÔNG được thấy: `Installing Python dependencies...`

