# Script build và cài đặt Android app
# Chạy: .\build_and_install.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BUILD VÀ CÀI ĐẶT ANDROID APP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra IP hiện tại
Write-Host "[1/4] Kiểm tra cấu hình..." -ForegroundColor Yellow
$ip = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -like "192.168.1.*" } | Select-Object -First 1).IPAddress
if ($ip) {
    Write-Host "✓ IP mạng Wi-Fi: $ip" -ForegroundColor Green
} else {
    Write-Host "⚠ Không tìm thấy IP mạng 192.168.1.x" -ForegroundColor Yellow
    Write-Host "  Vui lòng kiểm tra lại kết nối mạng" -ForegroundColor Yellow
}

# Kiểm tra backend
Write-Host "[2/4] Kiểm tra backend..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://$ip`:8001/" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
    Write-Host "✓ Backend đang chạy tại http://$ip`:8001" -ForegroundColor Green
} catch {
    Write-Host "✗ Không thể kết nối đến backend tại http://$ip`:8001" -ForegroundColor Red
    Write-Host "  Vui lòng đảm bảo backend đang chạy: cd APP && docker compose up -d" -ForegroundColor Yellow
    exit 1
}

# Build app
Write-Host "[3/4] Đang build app..." -ForegroundColor Yellow
Set-Location app
if (Test-Path "gradlew.bat") {
    .\gradlew.bat clean assembleDebug
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Build thành công!" -ForegroundColor Green
    } else {
        Write-Host "✗ Build thất bại!" -ForegroundColor Red
        Set-Location ..
        exit 1
    }
} else {
    Write-Host "✗ Không tìm thấy gradlew.bat" -ForegroundColor Red
    Set-Location ..
    exit 1
}

# Kiểm tra APK
Write-Host "[4/4] Kiểm tra APK..." -ForegroundColor Yellow
$apkPath = "build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apkSize = (Get-Item $apkPath).Length / 1MB
    Write-Host "✓ APK đã được tạo: $apkPath ($([math]::Round($apkSize, 2)) MB)" -ForegroundColor Green
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  CÀI ĐẶT APP" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Cách 1: Cài đặt qua ADB (USB)" -ForegroundColor Yellow
    Write-Host "  1. Kết nối điện thoại qua USB" -ForegroundColor White
    Write-Host "  2. Bật USB Debugging trên điện thoại" -ForegroundColor White
    Write-Host "  3. Chạy: adb install $apkPath" -ForegroundColor White
    Write-Host ""
    Write-Host "Cách 2: Cài đặt trực tiếp" -ForegroundColor Yellow
    Write-Host "  .\gradlew.bat installDebug" -ForegroundColor White
    Write-Host ""
    Write-Host "Cách 3: Copy APK sang điện thoại" -ForegroundColor Yellow
    Write-Host "  Copy file: $apkPath" -ForegroundColor White
    Write-Host "  Sang điện thoại và cài đặt thủ công" -ForegroundColor White
    Write-Host ""
    Write-Host "Lưu ý:" -ForegroundColor Cyan
    Write-Host "  - Đảm bảo điện thoại và máy tính cùng mạng Wi-Fi" -ForegroundColor White
    Write-Host "  - Backend URL: http://$ip`:8001" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "✗ Không tìm thấy APK tại $apkPath" -ForegroundColor Red
    Set-Location ..
    exit 1
}

Set-Location ..

