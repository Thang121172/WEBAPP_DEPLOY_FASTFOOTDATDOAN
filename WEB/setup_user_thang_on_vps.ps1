# Script chạy TRÊN VPS để tạo user thang và setup SSH key
# Chạy script này trên VPS (qua Remote Desktop)

Write-Host "=== TẠO USER THANG VÀ SETUP SSH KEY ===" -ForegroundColor Cyan
Write-Host ""

# Bước 1: Tạo user thang
Write-Host "[1/4] Tạo user 'thang'..." -ForegroundColor Yellow
try {
    $existingUser = Get-LocalUser -Name "thang" -ErrorAction SilentlyContinue
    if ($existingUser) {
        Write-Host "⚠️  User 'thang' đã tồn tại!" -ForegroundColor Yellow
    } else {
        $securePassword = ConvertTo-SecureString "Thang2004" -AsPlainText -Force
        New-LocalUser -Name "thang" -Password $securePassword -FullName "Thang" -Description "User for SSH access"
        Write-Host "✅ Đã tạo user: thang (Password: Thang2004)" -ForegroundColor Green
    }
    
    # Thêm vào nhóm Remote Desktop Users
    Add-LocalGroupMember -Group "Remote Desktop Users" -Member "thang" -ErrorAction SilentlyContinue
    Write-Host "✅ User đã được thêm vào nhóm Remote Desktop Users" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Lỗi tạo user: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Bước 2: Tạo thư mục .ssh
Write-Host ""
Write-Host "[2/4] Tạo thư mục .ssh..." -ForegroundColor Yellow
$sshDir = "C:\Users\thang\.ssh"
if (-not (Test-Path $sshDir)) {
    New-Item -ItemType Directory -Path $sshDir -Force | Out-Null
    Write-Host "✅ Đã tạo thư mục: $sshDir" -ForegroundColor Green
} else {
    Write-Host "✅ Thư mục .ssh đã tồn tại" -ForegroundColor Green
}

# Bước 3: Thêm SSH public key vào authorized_keys
Write-Host ""
Write-Host "[3/4] Thêm SSH public key..." -ForegroundColor Yellow
Write-Host "Vui lòng paste SSH public key từ máy local (từ file C:\Users\ASUS\.ssh\id_ed25519.pub):" -ForegroundColor Cyan
Write-Host "Key: ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIF7xdnL7PsInD8i8LRUnXbCDEzV0sWKACq/cZmXgrpkG github-ssh-key" -ForegroundColor Gray
Write-Host ""

$publicKey = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIF7xdnL7PsInD8i8LRUnXbCDEzV0sWKACq/cZmXgrpkG github-ssh-key"
$authorizedKeysPath = "C:\Users\thang\.ssh\authorized_keys"

# Kiểm tra key đã tồn tại chưa
if (Test-Path $authorizedKeysPath) {
    $existingKeys = Get-Content $authorizedKeysPath
    if ($existingKeys -contains $publicKey) {
        Write-Host "⚠️  SSH key đã tồn tại trong authorized_keys" -ForegroundColor Yellow
    } else {
        Add-Content -Path $authorizedKeysPath -Value $publicKey
        Write-Host "✅ Đã thêm SSH key vào authorized_keys" -ForegroundColor Green
    }
} else {
    Set-Content -Path $authorizedKeysPath -Value $publicKey
    Write-Host "✅ Đã tạo file authorized_keys và thêm SSH key" -ForegroundColor Green
}

# Bước 4: Đặt quyền cho file authorized_keys
Write-Host ""
Write-Host "[4/4] Đặt quyền cho authorized_keys..." -ForegroundColor Yellow
$computerName = $env:COMPUTERNAME
icacls "C:\Users\thang\.ssh\authorized_keys" /inheritance:r /grant:r "${computerName}\thang`:F" /grant:r "Administrators:F" | Out-Null
Write-Host "✅ Đã đặt quyền cho authorized_keys" -ForegroundColor Green

Write-Host ""
Write-Host "=== HOÀN TẤT ===" -ForegroundColor Green
Write-Host ""
Write-Host "Thông tin user:" -ForegroundColor Cyan
Write-Host "  - Tên user: thang" -ForegroundColor White
Write-Host "  - Password: Thang2004" -ForegroundColor White
Write-Host "  - SSH key: Đã được thêm" -ForegroundColor White
Write-Host ""
Write-Host "Bạn có thể SSH vào VPS bằng:" -ForegroundColor Cyan
Write-Host "  ssh thang@103.75.182.180" -ForegroundColor Yellow
Write-Host "  (Không cần nhập password - sẽ dùng SSH key tự động)" -ForegroundColor Gray
Write-Host ""

