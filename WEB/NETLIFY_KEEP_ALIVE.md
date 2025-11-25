# Hướng dẫn giữ Frontend luôn chạy trên Netlify

## Vấn đề
Netlify Free Tier có thể bị "sleep" sau 30 phút không có traffic, khiến lần đầu load sẽ chậm hơn.

## Giải pháp: Sử dụng UptimeRobot (Miễn phí)

### Bước 1: Đăng ký UptimeRobot
1. Truy cập: https://uptimerobot.com/
2. Đăng ký tài khoản miễn phí (không cần credit card)
3. Free tier cho phép monitor **50 URLs** và check mỗi **5 phút**

### Bước 2: Tạo Monitor
1. Sau khi đăng nhập, click **"Add New Monitor"**
2. Cấu hình như sau:
   - **Monitor Type**: Chọn `HTTP(s)`
   - **Friendly Name**: `FastFood Frontend`
   - **URL**: `https://fastfooddatdoan.netlify.app/` (URL Netlify của bạn)
   - **Monitoring Interval**: `Every 5 minutes` (5 phút một lần)
   - **Alert Contacts**: Chọn email của bạn (để nhận thông báo nếu down)
3. Click **"Create Monitor"**

### Bước 3: Xác nhận
- UptimeRobot sẽ tự động ping URL của bạn mỗi 5 phút
- Điều này giữ app "awake" và tránh sleep
- Bạn có thể xem status trên dashboard của UptimeRobot

## Lưu ý
- **Không cần mở Netlify dashboard** - App vẫn chạy bình thường
- UptimeRobot chỉ ping để giữ app "awake", không ảnh hưởng đến người dùng
- App vẫn sẽ "sleep" nếu không có traffic, nhưng sẽ được "wake up" nhanh hơn nhờ UptimeRobot

## Các dịch vụ tương tự (Miễn phí)
1. **UptimeRobot**: https://uptimerobot.com/ (Khuyên dùng)
2. **Cron-Job.org**: https://cron-job.org/ (Có thể setup cron job)
3. **Better Uptime**: https://betteruptime.com/ (Free tier có giới hạn)

## Giải thích kỹ thuật
- Netlify host app của bạn trên CDN, app sẽ luôn accessible
- "Sleep" chỉ ảnh hưởng đến cold start time (từ ~2-3s lần đầu)
- UptimeRobot ping mỗi 5 phút giúp giữ app "warm"
- Không cần phải mở Netlify dashboard hay làm gì đặc biệt

