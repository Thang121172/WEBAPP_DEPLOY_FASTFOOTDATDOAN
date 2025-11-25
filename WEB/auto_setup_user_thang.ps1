# Script TỰ ĐỘNG setup user thang và SSH key
# Chạy script này trên MÁY LOCAL của bạn

Write-Host "=== TỰ ĐỘNG SETUP USER THANG VÀ SSH KEY ===" -ForegroundColor Cyan
Write-Host ""

# Đọc SSH public key từ máy local
$publicKeyPath = "$env:USERPROFILE\.ssh\id_ed25519.pub"
if (-not (Test-Path $publicKeyPath)) {
    Write-Host "❌ Không tìm thấy SSH public key tại: $publicKeyPath" -ForegroundColor Red
    Write-Host "Vui lòng tạo SSH key trước: ssh-keygen -t ed25519 -C 'your_email@example.com'" -ForegroundColor Yellow
    exit 1
}

$publicKey = Get-Content $publicKeyPath -Raw | ForEach-Object { $_.Trim() }
Write-Host "✅ Đã đọc SSH public key từ máy local" -ForegroundColor Green
Write-Host "   Key: $publicKey" -ForegroundColor Gray
Write-Host ""

# Tạo script PowerShell để chạy trên VPS
$vpsScript = @"
Write-Host "=== SETUP USER THANG TRÊN VPS ===" -ForegroundColor Cyan
Write-Host ""

# Bước 1: Tạo user thang
Write-Host "[1/4] Tạo user 'thang'..." -ForegroundColor Yellow
try {
    `$existingUser = Get-LocalUser -Name "thang" -ErrorAction SilentlyContinue
    if (`$existingUser) {
        Write-Host "⚠️  User 'thang' đã tồn tại!" -ForegroundColor Yellow
    } else {
        `$securePassword = ConvertTo-SecureString "Thang2004" -AsPlainText -Force
        New-LocalUser -Name "thang" -Password `$securePassword -FullName "Thang" -Description "User for SSH access"
        Write-Host "✅ Đã tạo user: thang (Password: Thang2004)" -ForegroundColor Green
    }
    
    # Thêm vào nhóm Remote Desktop Users
    Add-LocalGroupMember -Group "Remote Desktop Users" -Member "thang" -ErrorAction SilentlyContinue
    Write-Host "✅ User đã được thêm vào nhóm Remote Desktop Users" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Lỗi tạo user: `$(`$_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Bước 2: Tạo thư mục .ssh
Write-Host ""
Write-Host "[2/4] Tạo thư mục .ssh..." -ForegroundColor Yellow
`$sshDir = "C:\Users\thang\.ssh"
if (-not (Test-Path `$sshDir)) {
    New-Item -ItemType Directory -Path `$sshDir -Force | Out-Null
    Write-Host "✅ Đã tạo thư mục: `$sshDir" -ForegroundColor Green
} else {
    Write-Host "✅ Thư mục .ssh đã tồn tại" -ForegroundColor Green
}

# Bước 3: Thêm SSH public key vào authorized_keys
Write-Host ""
Write-Host "[3/4] Thêm SSH public key..." -ForegroundColor Yellow
`$authorizedKeysPath = "C:\Users\thang\.ssh\authorized_keys"
`$newKey = "$publicKey"

# Kiểm tra key đã tồn tại chưa
if (Test-Path `$authorizedKeysPath) {
    `$existingKeys = Get-Content `$authorizedKeysPath
    if (`$existingKeys -contains `$newKey) {
        Write-Host "⚠️  SSH key đã tồn tại trong authorized_keys" -ForegroundColor Yellow
    } else {
        Add-Content -Path `$authorizedKeysPath -Value `$newKey
        Write-Host "✅ Đã thêm SSH key vào authorized_keys" -ForegroundColor Green
    }
} else {
    Set-Content -Path `$authorizedKeysPath -Value `$newKey
    Write-Host "✅ Đã tạo file authorized_keys và thêm SSH key" -ForegroundColor Green
}

# Bước 4: Đặt quyền cho file authorized_keys
Write-Host ""
Write-Host "[4/4] Đặt quyền cho authorized_keys..." -ForegroundColor Yellow
`$computerName = `$env:COMPUTERNAME
icacls "C:\Users\thang\.ssh\authorized_keys" /inheritance:r /grant:r "`${computerName}\thang`:F" /grant:r "Administrators:F" | Out-Null
Write-Host "✅ Đã đặt quyền cho authorized_keys" -ForegroundColor Green

Write-Host ""
Write-Host "=== HOÀN TẤT ===" -ForegroundColor Green
Write-Host ""
Write-Host "Bạn có thể SSH vào VPS bằng user 'thang' mà không cần password:" -ForegroundColor Cyan
Write-Host "  ssh thang@103.75.182.180" -ForegroundColor Yellow
Write-Host ""
"@

# Gửi script lên VPS và chạy
Write-Host "🔄 Đang setup user thang trên VPS..." -ForegroundColor Yellow
Write-Host "   (Bạn sẽ được hỏi password của Administrator)" -ForegroundColor Gray
Write-Host ""

ssh vps "powershell -Command '$vpsScript'"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ THÀNH CÔNG!" -ForegroundColor Green
    Write-Host ""
    Write-Host "=== THÔNG TIN USER ===" -ForegroundColor Cyan
    Write-Host "  - Tên user: thang" -ForegroundColor White
    Write-Host "  - Password: Thang2004" -ForegroundColor White
    Write-Host "  - SSH key: Đã được thêm" -ForegroundColor White
    Write-Host ""
    Write-Host "=== TEST KẾT NỐI ===" -ForegroundColor Cyan
    Write-Host "Bạn có thể SSH vào VPS bằng:" -ForegroundColor Yellow
    Write-Host "  ssh vps-thang" -ForegroundColor White
    Write-Host "  hoặc" -ForegroundColor Gray
    Write-Host "  ssh thang@103.75.182.180" -ForegroundColor White
    Write-Host ""
    Write-Host "⚠️  LƯU Ý: Lần đầu tiên SSH, bạn sẽ được hỏi xác nhận fingerprint." -ForegroundColor Yellow
    Write-Host "   Sau đó, SSH sẽ tự động dùng SSH key, KHÔNG CẦN NHẬP PASSWORD!" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "❌ Có lỗi xảy ra. Kiểm tra lại:" -ForegroundColor Red
    Write-Host "  1. Kết nối SSH đến VPS có OK không? (ping 103.75.182.180)" -ForegroundColor Yellow
    Write-Host "  2. Password của Administrator có đúng không?" -ForegroundColor Yellow
}

