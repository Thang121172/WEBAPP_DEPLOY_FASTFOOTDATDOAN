# Script để thêm SSH key vào user mới trên Windows VPS
# Sử dụng: .\add_ssh_key_to_vps.ps1 -Username "ten_user" -VPSHost "vps"

param(
    [Parameter(Mandatory=$true)]
    [string]$Username,
    
    [Parameter(Mandatory=$false)]
    [string]$VPSHost = "vps",
    
    [Parameter(Mandatory=$false)]
    [string]$PublicKeyPath = "$env:USERPROFILE\.ssh\id_ed25519.pub"
)

Write-Host "=== ADD SSH KEY TO VPS USER ===" -ForegroundColor Cyan
Write-Host "User: $Username" -ForegroundColor Yellow
Write-Host "VPS: $VPSHost" -ForegroundColor Yellow
Write-Host ""

# Đọc public key từ máy local
if (-not (Test-Path $PublicKeyPath)) {
    Write-Host "❌ Không tìm thấy public key tại: $PublicKeyPath" -ForegroundColor Red
    exit 1
}

$publicKey = Get-Content $PublicKeyPath -Raw | ForEach-Object { $_.Trim() }
Write-Host "✅ Đã đọc public key từ máy local" -ForegroundColor Green

# Tạo script PowerShell để chạy trên VPS
$vpsScript = @"
# Tạo thư mục .ssh nếu chưa có
`$sshDir = "C:\Users\$Username\.ssh"
if (-not (Test-Path `$sshDir)) {
    New-Item -ItemType Directory -Path `$sshDir -Force | Out-Null
    Write-Host "✅ Đã tạo thư mục .ssh"
}

# Thêm SSH key vào authorized_keys
`$authorizedKeysPath = "C:\Users\$Username\.ssh\authorized_keys"
`$newKey = "$publicKey"

# Kiểm tra xem key đã tồn tại chưa
if (Test-Path `$authorizedKeysPath) {
    `$existingKeys = Get-Content `$authorizedKeysPath
    if (`$existingKeys -contains `$newKey) {
        Write-Host "⚠️  SSH key đã tồn tại trong authorized_keys"
        exit 0
    }
    Add-Content -Path `$authorizedKeysPath -Value `$newKey
    Write-Host "✅ Đã thêm SSH key vào authorized_keys"
} else {
    Set-Content -Path `$authorizedKeysPath -Value `$newKey
    Write-Host "✅ Đã tạo file authorized_keys và thêm SSH key"
}

# Đặt quyền cho file authorized_keys
icacls "C:\Users\$Username\.ssh\authorized_keys" /inheritance:r /grant:r "${env:COMPUTERNAME}\$Username`:F" /grant:r "Administrators:F" | Out-Null
Write-Host "✅ Đã đặt quyền cho authorized_keys"

Write-Host ""
Write-Host "=== HOÀN TẤT ===" -ForegroundColor Green
Write-Host "SSH key đã được thêm vào user: $Username"
Write-Host "Bạn có thể SSH vào VPS bằng: ssh $Username@103.75.182.180"
"@

# Gửi script lên VPS và chạy
Write-Host "🔄 Đang thêm SSH key vào VPS..." -ForegroundColor Yellow
ssh $VPSHost "powershell -Command '$vpsScript'"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ THÀNH CÔNG!" -ForegroundColor Green
    Write-Host "Bạn có thể SSH vào VPS bằng user mới:" -ForegroundColor Cyan
    Write-Host "  ssh $Username@103.75.182.180" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "❌ Có lỗi xảy ra. Kiểm tra lại tên user và kết nối VPS." -ForegroundColor Red
}

