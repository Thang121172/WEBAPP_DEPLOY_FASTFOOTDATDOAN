# 🔧 SỬA LỖI: Invalid HTTP_HOST header - ALLOWED_HOSTS

## ⚠️ **LỖI:**

```
Invalid HTTP_HOST header: 'fastfood-backend-t8jz.onrender.com'. 
You may need to add 'fastfood-backend-t8jz.onrender.com' to ALLOWED_HOSTS.
```

## ✅ **NGUYÊN NHÂN:**

Django đang chặn request vì domain không có trong `ALLOWED_HOSTS`. Mặc dù đã có logic tự động lấy từ `RENDER_EXTERNAL_HOSTNAME`, nhưng có thể biến này chưa được set đúng.

## ✅ **GIẢI PHÁP:**

### **Cách 1: Thêm ALLOWED_HOSTS thủ công (NHANH NHẤT)**

1. Vào Render → Service `fastfood-backend` → Tab **"Environment"**
2. Click **"Add Environment Variable"**
3. Thêm:
   ```
   Key: ALLOWED_HOSTS
   Value: fastfood-backend-t8jz.onrender.com
   ```
   ⚠️ **Thay `fastfood-backend-t8jz` bằng URL thực tế của bạn!**

4. Click **"Save Changes"**
5. Render sẽ tự động redeploy

---

### **Cách 2: Kiểm tra RENDER_EXTERNAL_HOSTNAME**

1. Vào Render → Service `fastfood-backend` → Tab **"Environment"**
2. Kiểm tra xem có biến `RENDER_EXTERNAL_HOSTNAME` chưa
3. Nếu chưa có, biến này đã được set trong `render.yaml` nhưng có thể chưa được tạo
4. Thêm thủ công nếu cần:
   ```
   Key: RENDER_EXTERNAL_HOSTNAME
   Value: fastfood-backend-t8jz.onrender.com
   ```

---

## 🔍 **KIỂM TRA:**

Sau khi thêm biến, kiểm tra logs:
- ✅ Không còn lỗi `Invalid HTTP_HOST header`
- ✅ Service trả về response 200 khi truy cập URL

---

**Sau khi thêm ALLOWED_HOSTS, Render sẽ tự động redeploy!** 🚀

