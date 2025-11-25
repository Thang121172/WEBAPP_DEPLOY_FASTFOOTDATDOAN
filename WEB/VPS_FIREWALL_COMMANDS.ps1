# ====================================
# VPS FIREWALL CONFIGURATION
# Chạy các lệnh này trong PowerShell (Admin) trên VPS
# ====================================

Write-Host "=== Opening Firewall Ports ===" -ForegroundColor Green

# 1. Enable Remote Desktop
Write-Host "1. Enabling Remote Desktop..." -ForegroundColor Yellow
Set-ItemProperty -Path 'HKLM:\System\CurrentControlSet\Control\Terminal Server' -name "fDenyTSConnections" -Value 0
Enable-NetFirewallRule -DisplayGroup "Remote Desktop"

# 2. Open Port 3389 (Remote Desktop)
Write-Host "2. Opening Port 3389 (Remote Desktop)..." -ForegroundColor Yellow
New-NetFirewallRule -DisplayName "Remote Desktop - TCP 3389" -Direction Inbound -LocalPort 3389 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue

# 3. Allow ICMP (Ping)
Write-Host "3. Allowing ICMP (Ping)..." -ForegroundColor Yellow
New-NetFirewallRule -DisplayName "Allow ICMPv4-In" -Protocol ICMPv4 -IcmpType 8 -Action Allow -Enabled True -ErrorAction SilentlyContinue

# 4. Open Port 80 (HTTP)
Write-Host "4. Opening Port 80 (HTTP)..." -ForegroundColor Yellow
New-NetFirewallRule -DisplayName "HTTP Port 80" -Direction Inbound -LocalPort 80 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue

# 5. Open Port 5000 (Backend API)
Write-Host "5. Opening Port 5000 (Backend API)..." -ForegroundColor Yellow
New-NetFirewallRule -DisplayName "Backend API Port 5000" -Direction Inbound -LocalPort 5000 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue

# 6. Open Port 3000 (Frontend Dev)
Write-Host "6. Opening Port 3000 (Frontend)..." -ForegroundColor Yellow
New-NetFirewallRule -DisplayName "Frontend Port 3000" -Direction Inbound -LocalPort 3000 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue

Write-Host "=== Firewall Configuration Complete! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Opened Ports:" -ForegroundColor Cyan
Write-Host "  - 3389 (Remote Desktop)" -ForegroundColor White
Write-Host "  - 80   (HTTP/Frontend)" -ForegroundColor White
Write-Host "  - 3000 (Frontend Dev)" -ForegroundColor White
Write-Host "  - 5000 (Backend API)" -ForegroundColor White
Write-Host "  - ICMP (Ping)" -ForegroundColor White
Write-Host ""
Write-Host "You can now access VPS remotely!" -ForegroundColor Green

# Display current firewall rules
Write-Host "=== Current Firewall Rules ===" -ForegroundColor Cyan
Get-NetFirewallRule | Where-Object {$_.DisplayName -like "*Remote*" -or $_.DisplayName -like "*HTTP*" -or $_.DisplayName -like "*Backend*" -or $_.DisplayName -like "*Frontend*" -or $_.DisplayName -like "*ICMP*"} | Select-Object DisplayName, Enabled, Direction | Format-Table -AutoSize

