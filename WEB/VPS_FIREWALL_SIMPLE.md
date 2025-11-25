# Mở Firewall VPS - Các Lệnh Đơn Giản

## 🎯 Chạy Trong PowerShell (Admin) Trên VPS

### Cách 1: Copy từng lệnh

```powershell
# 1. Enable Remote Desktop
Set-ItemProperty -Path 'HKLM:\System\CurrentControlSet\Control\Terminal Server' -name "fDenyTSConnections" -Value 0
Enable-NetFirewallRule -DisplayGroup "Remote Desktop"

# 2. Mở Port 3389 (Remote Desktop)
New-NetFirewallRule -DisplayName "RDP-3389" -Direction Inbound -LocalPort 3389 -Protocol TCP -Action Allow

# 3. Allow ICMP (Ping)
New-NetFirewallRule -DisplayName "Allow-Ping" -Protocol ICMPv4 -IcmpType 8 -Action Allow -Enabled True

# 4. Mở Port 80 (HTTP)
New-NetFirewallRule -DisplayName "HTTP-80" -Direction Inbound -LocalPort 80 -Protocol TCP -Action Allow

# 5. Mở Port 5000 (Backend)
New-NetFirewallRule -DisplayName "Backend-5000" -Direction Inbound -LocalPort 5000 -Protocol TCP -Action Allow
```

### Cách 2: Chạy 1 lệnh duy nhất

Copy toàn bộ và paste vào PowerShell:

```powershell
Set-ItemProperty -Path 'HKLM:\System\CurrentControlSet\Control\Terminal Server' -name "fDenyTSConnections" -Value 0; Enable-NetFirewallRule -DisplayGroup "Remote Desktop"; New-NetFirewallRule -DisplayName "RDP-3389" -Direction Inbound -LocalPort 3389 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue; New-NetFirewallRule -DisplayName "Allow-Ping" -Protocol ICMPv4 -IcmpType 8 -Action Allow -Enabled True -ErrorAction SilentlyContinue; New-NetFirewallRule -DisplayName "HTTP-80" -Direction Inbound -LocalPort 80 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue; New-NetFirewallRule -DisplayName "Backend-5000" -Direction Inbound -LocalPort 5000 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue; Write-Host "Firewall configured successfully!" -ForegroundColor Green
```

---

## 📋 Kiểm Tra Firewall Rules

```powershell
# Xem tất cả rules đã tạo
Get-NetFirewallRule | Where-Object {$_.Enabled -eq $True} | Select-Object DisplayName, Direction | Format-Table

# Xem rules cụ thể
Get-NetFirewallRule -DisplayName "*RDP*"
Get-NetFirewallRule -DisplayName "*Ping*"
Get-NetFirewallRule -DisplayName "*HTTP*"
```

---

## ✅ Sau Khi Chạy Xong

1. Đóng PowerShell
2. Thử ping lại từ máy local:
   ```powershell
   ping 103.75.182.180
   ```
3. Thử Remote Desktop lại:
   ```
   mstsc /v:103.75.182.180
   ```

---

## 🆘 Nếu Vẫn Không Được

### Tắt Windows Firewall hoàn toàn (Tạm thời để test):

```powershell
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled False
```

### Bật lại Firewall:

```powershell
Set-NetFirewallProfile -Profile Domain,Public,Private -Enabled True
```

