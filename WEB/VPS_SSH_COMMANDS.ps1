# ========================================
# CÀI ĐẶT SSH SERVER CHO WINDOWS VPS
# Chạy trong PowerShell (Admin) trên VPS
# ========================================

Write-Host "=== Installing OpenSSH Server ===" -ForegroundColor Green

# 1. Kiểm tra OpenSSH
Write-Host "`n1. Checking OpenSSH availability..." -ForegroundColor Yellow
Get-WindowsCapability -Online | Where-Object Name -like 'OpenSSH*'

# 2. Cài OpenSSH Server
Write-Host "`n2. Installing OpenSSH Server..." -ForegroundColor Yellow
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

# 3. Cài OpenSSH Client
Write-Host "`n3. Installing OpenSSH Client..." -ForegroundColor Yellow
Add-WindowsCapability -Online -Name OpenSSH.Client~~~~0.0.1.0

# 4. Start SSH Service
Write-Host "`n4. Starting SSH service..." -ForegroundColor Yellow
Start-Service sshd

# 5. Set SSH Auto-start
Write-Host "`n5. Setting SSH to auto-start..." -ForegroundColor Yellow
Set-Service -Name sshd -StartupType 'Automatic'

# 6. Verify SSH Service
Write-Host "`n6. Verifying SSH service..." -ForegroundColor Yellow
Get-Service sshd | Format-Table -AutoSize

# 7. Mở Firewall Port 22
Write-Host "`n7. Opening Firewall Port 22..." -ForegroundColor Yellow
New-NetFirewallRule -Name "SSH-Inbound" -DisplayName "SSH (Port 22)" -Direction Inbound -LocalPort 22 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue

# 8. Mở thêm các port khác
Write-Host "`n8. Opening additional ports..." -ForegroundColor Yellow

# Port 3389 (Remote Desktop)
New-NetFirewallRule -DisplayName "RDP-3389" -Direction Inbound -LocalPort 3389 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue

# Allow ICMP (Ping)
New-NetFirewallRule -DisplayName "Allow-Ping" -Protocol ICMPv4 -IcmpType 8 -Action Allow -Enabled True -ErrorAction SilentlyContinue

# Port 80 (HTTP)
New-NetFirewallRule -DisplayName "HTTP-80" -Direction Inbound -LocalPort 80 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue

# Port 5000 (Backend API)
New-NetFirewallRule -DisplayName "Backend-5000" -Direction Inbound -LocalPort 5000 -Protocol TCP -Action Allow -ErrorAction SilentlyContinue

# 9. Enable Remote Desktop
Write-Host "`n9. Enabling Remote Desktop..." -ForegroundColor Yellow
Set-ItemProperty -Path 'HKLM:\System\CurrentControlSet\Control\Terminal Server' -name "fDenyTSConnections" -Value 0
Enable-NetFirewallRule -DisplayGroup "Remote Desktop"

# 10. Hiển thị thông tin
Write-Host "`n=== Installation Complete! ===" -ForegroundColor Green
Write-Host ""
Write-Host "SSH Server Status:" -ForegroundColor Cyan
Get-Service sshd | Select-Object Name, Status, StartType | Format-Table -AutoSize

Write-Host "Firewall Rules:" -ForegroundColor Cyan
Get-NetFirewallRule | Where-Object {$_.DisplayName -like "*SSH*" -or $_.DisplayName -like "*RDP*" -or $_.DisplayName -like "*HTTP*" -or $_.DisplayName -like "*Backend*" -or $_.DisplayName -like "*Ping*"} | Select-Object DisplayName, Enabled | Format-Table -AutoSize

Write-Host ""
Write-Host "You can now connect via SSH:" -ForegroundColor Green
Write-Host "  ssh Administrator@103.75.182.180" -ForegroundColor White
Write-Host ""
Write-Host "Or via Remote Desktop:" -ForegroundColor Green
Write-Host "  mstsc /v:103.75.182.180" -ForegroundColor White
Write-Host ""

# 11. Test SSH Port
Write-Host "Testing SSH Port 22..." -ForegroundColor Yellow
$testPort = Test-NetConnection -ComputerName localhost -Port 22 -InformationLevel Quiet
if ($testPort) {
    Write-Host "✓ SSH Port 22 is OPEN" -ForegroundColor Green
} else {
    Write-Host "✗ SSH Port 22 is CLOSED" -ForegroundColor Red
}

Write-Host "`n=== Setup Complete! ===" -ForegroundColor Green

