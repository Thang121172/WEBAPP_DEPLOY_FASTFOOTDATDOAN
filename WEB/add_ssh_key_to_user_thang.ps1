# Script thêm SSH key từ máy local vào user "thang" trên VPS
# Chạy script này trên máy local (không phải VPS)

$publicKeyPath = "$env:USERPROFILE\.ssh\id_ed25519.pub"
$publicKey = Get-Content $publicKeyPath -Raw | ForEach-Object { $_.Trim() }

Write-Host "=== THÊM SSH KEY VÀO USER THANG ===" -ForegroundColor Cyan
Write-Host "Public key từ: $publicKeyPath" -ForegroundColor Yellow
Write-Host ""
Write-Host "Key: $publicKey" -ForegroundColor Gray
Write-Host ""

# Tạo script PowerShell để chạy trên VPS
$vpsScript = @"
# Tạo thư mục .ssh nếu chưa có
`$sshDir = "C:\Users\thang\.ssh"
if (-not (Test-Path `$sshDir)) {
    New-Item -ItemType Directory -Path `$sshDir -Force | Out-Null
    Write-Host "✅ Đã tạo thư mục .ssh"
}

# Thêm SSH key vào authorized_keys
`$authorizedKeysPath = "C:\Users\thang\.ssh\authorized_keys"
`$newKey = "$publicKey"

# Kiểm tra xem key đã tồn tại chưa
if (Test-Path `$authorizedKeysPath) {
    `$existingKeys = Get-Content `$authorizedKeysPath
    if (`$existingKeys -contains `$newKey) {
        Write-Host "⚠️  SSH key đã tồn tại trong authorized_keys" -ForegroundColor Yellow
        exit 0
    }
    Add-Content -Path `$authorizedKeysPath -Value `$newKey
    Write-Host "✅ Đã thêm SSH key vào authorized_keys" -ForegroundColor Green
} else {
    Set-Content -Path `$authorizedKeysPath -Value `$newKey
    Write-Host "✅ Đã tạo file authorized_keys và thêm SSH key" -ForegroundColor Green
}

# Đặt quyền cho file authorized_keys
icacls "C:\Users\thang\.ssh\authorized_keys" /inheritance:r /grant:r "`${env:COMPUTERNAME}\thang`:F" /grant:r "Administrators:F" | Out-Null
Write-Host "✅ Đã đặt quyền cho authorized_keys" -ForegroundColor Green

Write-Host ""
Write-Host "=== HOÀN TẤT ===" -ForegroundColor Green
Write-Host "SSH key đã được thêm vào user: thang" -ForegroundColor Cyan
Write-Host "Bạn có thể SSH vào VPS bằng: ssh thang@103.75.182.180" -ForegroundColor Yellow
"@

# Gửi script lên VPS và chạy
Write-Host "🔄 Đang thêm SSH key vào VPS..." -ForegroundColor Yellow
ssh vps "powershell -Command '$vpsScript'"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ THÀNH CÔNG!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Bạn có thể SSH vào VPS bằng user 'thang':" -ForegroundColor Cyan
    Write-Host "  ssh thang@103.75.182.180" -ForegroundColor White
    Write-Host ""
    Write-Host "Hoặc nếu đã cấu hình SSH config:" -ForegroundColor Cyan
    Write-Host "  ssh thang@vps" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "❌ Có lỗi xảy ra. Kiểm tra lại:" -ForegroundColor Red
    Write-Host "  1. User 'thang' đã được tạo chưa?" -ForegroundColor Yellow
    Write-Host "  2. Kết nối SSH đến VPS có OK không?" -ForegroundColor Yellow
}

