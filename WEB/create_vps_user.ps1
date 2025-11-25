# Script để tạo user mới trên Windows VPS
# Sử dụng: .\create_vps_user.ps1 -Username "ten_user" -Password "mat_khau"

param(
    [Parameter(Mandatory=$true)]
    [string]$Username,
    
    [Parameter(Mandatory=$true)]
    [string]$Password,
    
    [Parameter(Mandatory=$false)]
    [string]$VPSHost = "vps"
)

Write-Host "=== CREATE NEW USER ON VPS ===" -ForegroundColor Cyan
Write-Host "User: $Username" -ForegroundColor Yellow
Write-Host "VPS: $VPSHost" -ForegroundColor Yellow
Write-Host ""

# Tạo script PowerShell để chạy trên VPS
$vpsScript = @"
try {
    # Kiểm tra xem user đã tồn tại chưa
    `$existingUser = Get-LocalUser -Name "$Username" -ErrorAction SilentlyContinue
    if (`$existingUser) {
        Write-Host "⚠️  User '$Username' đã tồn tại!"
        exit 1
    }
    
    # Tạo user mới
    `$securePassword = ConvertTo-SecureString "$Password" -AsPlainText -Force
    New-LocalUser -Name "$Username" -Password `$securePassword -FullName "$Username" -Description "User created for SSH access"
    Write-Host "✅ Đã tạo user: $Username"
    
    # Thêm user vào nhóm Remote Desktop Users (nếu cần RDP)
    Add-LocalGroupMember -Group "Remote Desktop Users" -Member "$Username" -ErrorAction SilentlyContinue
    
    # Thêm user vào nhóm Administrators (nếu cần quyền admin)
    # Uncomment dòng dưới nếu muốn user có quyền admin:
    # Add-LocalGroupMember -Group "Administrators" -Member "$Username"
    
    Write-Host "✅ User đã được tạo thành công!"
    Write-Host ""
    Write-Host "Tên user: $Username"
    Write-Host "Password: $Password"
    Write-Host ""
    Write-Host "Bước tiếp theo: Chạy script add_ssh_key_to_vps.ps1 để thêm SSH key"
    exit 0
} catch {
    Write-Host "❌ Lỗi: `$(`$_.Exception.Message)" -ForegroundColor Red
    exit 1
}
"@

# Gửi script lên VPS và chạy
Write-Host "🔄 Đang tạo user trên VPS..." -ForegroundColor Yellow
ssh $VPSHost "powershell -Command '$vpsScript'"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ THÀNH CÔNG!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "❌ Có lỗi xảy ra." -ForegroundColor Red
}

