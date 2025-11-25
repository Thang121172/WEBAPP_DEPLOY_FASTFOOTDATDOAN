# 🔍 KIỂM TRA KẾT NỐI VPS

## 1. Kiểm tra Ping (cơ bản nhất)
```powershell
ping -n 4 103.75.182.180
```
✅ **Kết quả OK**: Thấy `Reply from 103.75.182.180: bytes=32 time=Xms`

---

## 2. Kiểm tra SSH Connection
```powershell
ssh vps "echo 'Connected successfully!'"
```
✅ **Kết quả OK**: Thấy dòng `Connected successfully!`

**Hoặc đơn giản hơn:**
```powershell
ssh vps "hostname"
```
✅ **Kết quả OK**: Thấy tên máy chủ VPS (ví dụ: `WIN-JG1E0O7FSBS`)

---

## 3. Kiểm tra Port (22 - SSH)
```powershell
Test-NetConnection -ComputerName 103.75.182.180 -Port 22
```
✅ **Kết quả OK**: `TcpTestSucceeded : True`

---

## 4. Kiểm tra các Service đang chạy
```powershell
ssh vps "powershell -Command 'Get-Process | Where-Object {`$_.ProcessName -like \"*python*\" -or `$_.ProcessName -like \"*node*\"} | Select-Object ProcessName, Id'"
```

---

## 5. Kiểm tra Backend API
```powershell
curl http://103.75.182.180:5000
```
✅ **Kết quả OK**: Nhận được response từ Django API

**Hoặc dùng PowerShell:**
```powershell
Invoke-WebRequest -Uri http://103.75.182.180:5000 -UseBasicParsing
```

---

## 6. Kiểm tra Frontend
```powershell
curl http://103.75.182.180:3000
```
✅ **Kết quả OK**: Nhận được HTML từ frontend

---

## 7. Kiểm tra Node.js, Python, Git trên VPS
```powershell
ssh vps "powershell -Command 'node -v; python --version; git --version'"
```

---

## 8. Kiểm tra Firewall Rules
```powershell
ssh vps "powershell -Command 'Get-NetFirewallRule | Where-Object DisplayName -like \"*Frontend*\" -or DisplayName -like \"*Backend*\" | Select-Object DisplayName, Enabled, Direction'"
```

---

## 🚀 LỆNH NHANH NHẤT - Test tất cả:
```powershell
Write-Host "=== TEST VPS CONNECTION ===" -ForegroundColor Cyan; Write-Host "`n1. Ping test:" -ForegroundColor Yellow; ping -n 2 103.75.182.180 | Select-Object -Last 1; Write-Host "`n2. SSH test:" -ForegroundColor Yellow; ssh vps "hostname"; Write-Host "`n3. Backend API test:" -ForegroundColor Yellow; try { $r = Invoke-WebRequest -Uri http://103.75.182.180:5000 -UseBasicParsing -TimeoutSec 5; Write-Host "✅ Backend OK - Status: $($r.StatusCode)" -ForegroundColor Green } catch { Write-Host "❌ Backend not responding" -ForegroundColor Red }; Write-Host "`n4. Frontend test:" -ForegroundColor Yellow; try { $r = Invoke-WebRequest -Uri http://103.75.182.180:3000 -UseBasicParsing -TimeoutSec 5; Write-Host "✅ Frontend OK - Status: $($r.StatusCode)" -ForegroundColor Green } catch { Write-Host "❌ Frontend not responding" -ForegroundColor Red }
```

