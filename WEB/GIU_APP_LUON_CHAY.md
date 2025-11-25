# 🔧 Cách giữ Frontend luôn chạy trên Netlify

## ❌ Hiểu lầm thường gặp
- **KHÔNG CẦN** mở Netlify dashboard để app chạy
- **KHÔNG CẦN** giữ tab trình duyệt mở
- App đã được deploy và luôn accessible qua URL của bạn

## ✅ Sự thật
- **App LUÔN CHẠY** trên server Netlify
- Bất kỳ ai cũng có thể truy cập bất cứ lúc nào
- Netlify host app trên CDN, không cần bạn làm gì thêm

## ⚠️ Vấn đề "Sleep" trên Free Tier
- Netlify Free Tier có thể "sleep" sau **30 phút** không có traffic
- Lần đầu sau khi sleep sẽ load chậm hơn (~2-3 giây) - gọi là "cold start"
- Sau khi "wake up", app chạy bình thường

## 🔧 Giải pháp: Setup UptimeRobot (Miễn phí)

### Cách 1: Sử dụng UptimeRobot (Đơn giản nhất)

**Bước 1:** Truy cập https://uptimerobot.com/
- Đăng ký miễn phí (không cần thẻ tín dụng)

**Bước 2:** Tạo Monitor
1. Click **"+ Add New Monitor"**
2. Điền thông tin:
   ```
   Monitor Type: HTTP(s)
   Friendly Name: FastFood Frontend
   URL: https://fastfooddatdoan.netlify.app/
   Monitoring Interval: Every 5 minutes
   ```
3. Click **"Create Monitor"**

**Bước 3:** Xong!
- UptimeRobot sẽ tự động ping app mỗi 5 phút
- App sẽ không bị sleep nữa
- Bạn nhận email nếu app down

### Cách 2: Sử dụng Cron-Job (Nếu thích tự động hơn)

**Bước 1:** Truy cập https://cron-job.org/
- Đăng ký tài khoản miễn phí

**Bước 2:** Tạo Cron Job
1. Click **"Create cronjob"**
2. Điền thông tin:
   ```
   Title: Keep Netlify Alive
   Address: https://fastfooddatdoan.netlify.app/
   Schedule: */5 * * * * (mỗi 5 phút)
   ```
3. Click **"Create cronjob"**

### Cách 3: Sử dụng Browser Extension (Cho máy tính của bạn)

1. Cài extension **"UptimeRobot"** hoặc **"Website Monitor"**
2. Add URL: `https://fastfooddatdoan.netlify.app/`
3. Set interval: 5 phút

## 📝 Lưu ý quan trọng

1. **App LUÔN CHẠY** - không cần làm gì thêm sau khi deploy
2. **"Sleep" không phải lỗi** - chỉ là tính năng của Free Tier
3. **UptimeRobot không bắt buộc** - nhưng giúp app luôn "warm"
4. **Người dùng vẫn truy cập được** - dù app có "sleep" hay không

## 🆘 Nếu gặp lỗi

### Lỗi: "Site not found" hoặc "404"
- ✅ Kiểm tra URL có đúng không
- ✅ Kiểm tra Netlify deploy status (phải là "Published")

### Lỗi: "Connection timeout"
- ✅ Kiểm tra backend có đang chạy không (Render)
- ✅ Kiểm tra `VITE_API_BASE` trong Netlify environment variables

### App load chậm lần đầu
- ✅ Bình thường nếu app đã "sleep"
- ✅ Setup UptimeRobot để tránh sleep

## 🎯 Kết luận
- **App đã chạy rồi** - bạn không cần làm gì thêm
- **Setup UptimeRobot** (5 phút) để app luôn "warm"
- **Không cần mở Netlify dashboard** - app vẫn chạy

