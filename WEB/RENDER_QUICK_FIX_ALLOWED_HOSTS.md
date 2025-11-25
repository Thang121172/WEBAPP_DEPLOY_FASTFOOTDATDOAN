# 🔧 SỬA NHANH: Lỗi ALLOWED_HOSTS

## ⚠️ **LỖI:**
```
Invalid HTTP_HOST header: 'fastfood-backend-t8jz.onrender.com'. 
You may need to add 'fastfood-backend-t8jz.onrender.com' to ALLOWED_HOSTS.
```

## ✅ **SỬA NHANH (2 PHÚT):**

1. **Vào Render Dashboard** → Service **"fastfood-backend"**

2. **Vào tab "Environment"**

3. **Click "Add Environment Variable"**

4. **Thêm biến:**
   ```
   Key: ALLOWED_HOSTS
   Value: fastfood-backend-t8jz.onrender.com
   ```
   ⚠️ **Lưu ý:** Thay `fastfood-backend-t8jz` bằng URL thực tế của bạn!

5. **Click "Save Changes"**

6. **Render sẽ tự động redeploy** (chờ 2-3 phút)

7. **Kiểm tra lại:** Mở URL `https://fastfood-backend-t8jz.onrender.com` - không còn lỗi!

---

## ✅ **SAU KHI SỬA:**

✅ Service sẽ chấp nhận requests từ domain Render  
✅ Không còn lỗi `Invalid HTTP_HOST header`  
✅ API sẽ hoạt động bình thường

---

**Làm ngay bước 4 trên! Render sẽ tự động redeploy và sửa lỗi!** 🚀

