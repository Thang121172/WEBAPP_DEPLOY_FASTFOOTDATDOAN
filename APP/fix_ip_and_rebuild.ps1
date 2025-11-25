# Script fix IP và rebuild app
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  FIX IP VÀ REBUILD APP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra IP Wi-Fi hiện tại
Write-Host "[1/4] Kiểm tra IP Wi-Fi hiện tại..." -ForegroundColor Yellow
$wifiIp = $null
$ipconfig = ipconfig
$inWifiSection = $false

foreach ($line in $ipconfig) {
    if ($line -match "Wireless LAN adapter Wi-Fi") {
        $inWifiSection = $true
        continue
    }
    if ($inWifiSection -and $line -match "IPv4.*: ([\d.]+)") {
        $wifiIp = $matches[1]
        break
    }
    if ($inWifiSection -and $line -match "^$") {
        $inWifiSection = $false
    }
}

if ($wifiIp) {
    Write-Host "✓ IP Wi-Fi: $wifiIp" -ForegroundColor Green
} else {
    # Fallback: lấy IP đầu tiên trong dải 192.168.x.x
    $allIps = ipconfig | Select-String -Pattern "IPv4.*192\.168\.\d+\.\d+" | ForEach-Object {
        if ($_ -match "192\.168\.\d+\.\d+") { $matches[0] }
    }
    if ($allIps) {
        $wifiIp = $allIps[0]
        Write-Host "✓ IP mạng local: $wifiIp" -ForegroundColor Green
    } else {
        Write-Host "⚠ Không tìm thấy IP Wi-Fi, dùng IP mặc định: 192.168.1.130" -ForegroundColor Yellow
        $wifiIp = "192.168.1.130"
    }
}

Write-Host ""
Write-Host "[2/4] Kiểm tra cấu hình trong build.gradle.kts..." -ForegroundColor Yellow
$gradleFile = "app\build.gradle.kts"
if (Test-Path $gradleFile) {
    $content = Get-Content $gradleFile -Raw
    if ($content -match 'BASE_URL","\\"http://([\d.]+):8001/\\""') {
        $currentIp = $matches[1]
        Write-Host "  IP hiện tại trong file: $currentIp" -ForegroundColor White
        if ($currentIp -ne $wifiIp) {
            Write-Host "  ⚠ IP không khớp! Đang cập nhật..." -ForegroundColor Yellow
            $content = $content -replace "http://$currentIp:8001/", "http://$wifiIp:8001/"
            Set-Content -Path $gradleFile -Value $content -NoNewline
            Write-Host "  ✓ Đã cập nhật IP thành: $wifiIp" -ForegroundColor Green
        } else {
            Write-Host "  ✓ IP đã đúng trong file" -ForegroundColor Green
        }
    }
} else {
    Write-Host "  ✗ Không tìm thấy build.gradle.kts" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[3/4] Clean và build app..." -ForegroundColor Yellow
Write-Host "  Đang clean..." -ForegroundColor Cyan
& .\gradlew.bat clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ✗ Clean thất bại!" -ForegroundColor Red
    exit 1
}

Write-Host "  Đang build debug APK..." -ForegroundColor Cyan
& .\gradlew.bat :app:assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ✗ Build thất bại!" -ForegroundColor Red
    exit 1
}

Write-Host "  ✓ Build thành công!" -ForegroundColor Green

Write-Host ""
Write-Host "[4/4] Cài đặt APK lên thiết bị..." -ForegroundColor Yellow
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apkPath) {
    Write-Host "  Đang cài đặt..." -ForegroundColor Cyan
    & adb install -r $apkPath
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✓ Cài đặt thành công!" -ForegroundColor Green
        
        Write-Host ""
        Write-Host "  Đang khởi động app..." -ForegroundColor Cyan
        & adb shell am start -n com.example.app/.MainActivity
        
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  ✅ HOÀN TẤT!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "App đã được rebuild và cài đặt với IP: $wifiIp" -ForegroundColor White
        Write-Host "Backend URL: http://$wifiIp`:8001/" -ForegroundColor White
    } else {
        Write-Host "  ✗ Cài đặt thất bại!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "  ✗ Không tìm thấy APK tại $apkPath" -ForegroundColor Red
    exit 1
}

