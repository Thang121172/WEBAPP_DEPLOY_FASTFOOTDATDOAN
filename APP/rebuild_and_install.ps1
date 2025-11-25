# Script clean, rebuild và cài đặt app
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CLEAN, REBUILD VÀ CÀI ĐẶT APP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Set-Location app

# Step 1: Clean
Write-Host "[1/4] Đang clean project..." -ForegroundColor Yellow
& .\gradlew.bat clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Clean thất bại!" -ForegroundColor Red
    Set-Location ..
    exit 1
}
Write-Host "✓ Clean thành công!" -ForegroundColor Green
Write-Host ""

# Step 2: Build
Write-Host "[2/4] Đang build app..." -ForegroundColor Yellow
& .\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Build thất bại!" -ForegroundColor Red
    Set-Location ..
    exit 1
}
Write-Host "✓ Build thành công!" -ForegroundColor Green
Write-Host ""

# Step 3: Kiểm tra APK
Write-Host "[3/4] Kiểm tra APK..." -ForegroundColor Yellow
$apkPath = "build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    $apk = Get-Item $apkPath
    $apkSize = [math]::Round($apk.Length / 1MB, 2)
    Write-Host "✓ APK đã được tạo!" -ForegroundColor Green
    Write-Host "  Đường dẫn: $($apk.FullName)" -ForegroundColor White
    Write-Host "  Kích thước: $apkSize MB" -ForegroundColor White
    Write-Host "  Thời gian: $($apk.LastWriteTime)" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "✗ Không tìm thấy APK tại $apkPath" -ForegroundColor Red
    Set-Location ..
    exit 1
}

# Step 4: Cài đặt
Write-Host "[4/4] Cài đặt APK..." -ForegroundColor Yellow
Write-Host "Kiểm tra thiết bị kết nối:" -ForegroundColor Cyan
$devices = & adb devices
Write-Host $devices

$deviceCount = ($devices | Select-String "device$" | Measure-Object).Count
if ($deviceCount -eq 0) {
    Write-Host "⚠ Không có thiết bị nào được kết nối!" -ForegroundColor Yellow
    Write-Host "  Vui lòng:" -ForegroundColor White
    Write-Host "  1. Kết nối điện thoại qua USB" -ForegroundColor White
    Write-Host "  2. Bật USB Debugging" -ForegroundColor White
    Write-Host "  3. Chạy lại script này" -ForegroundColor White
    Write-Host ""
    Write-Host "Hoặc copy APK thủ công:" -ForegroundColor Cyan
    Write-Host "  $($apk.FullName)" -ForegroundColor White
    Set-Location ..
    exit 0
}

Write-Host "Đang cài đặt APK..." -ForegroundColor Cyan
& adb install -r $apkPath
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✓ Cài đặt thành công!" -ForegroundColor Green
    Write-Host ""
    Write-Host "App đã sẵn sàng sử dụng!" -ForegroundColor Cyan
    Write-Host "Backend URL: http://192.168.1.130:8001" -ForegroundColor White
} else {
    Write-Host "✗ Cài đặt thất bại!" -ForegroundColor Red
    Write-Host "  Thử cài đặt thủ công: adb install -r $apkPath" -ForegroundColor Yellow
}

Set-Location ..

