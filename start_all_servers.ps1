# Script khởi động tất cả servers cho WEB và APP
# Chạy script này: .\start_all_servers.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KHỞI ĐỘNG TẤT CẢ SERVERS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra Docker
Write-Host "[1/6] Kiểm tra Docker..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Docker đã cài đặt: $dockerVersion" -ForegroundColor Green
    } else {
        Write-Host "✗ Docker chưa được cài đặt hoặc chưa khởi động" -ForegroundColor Red
        Write-Host "  Vui lòng khởi động Docker Desktop và thử lại" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ Không thể kiểm tra Docker: $_" -ForegroundColor Red
    exit 1
}

# Kiểm tra Docker daemon
Write-Host "[2/6] Kiểm tra Docker daemon..." -ForegroundColor Yellow
try {
    docker ps > $null 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Docker daemon đang chạy" -ForegroundColor Green
    } else {
        Write-Host "✗ Docker daemon chưa chạy. Đang thử khởi động Docker Desktop..." -ForegroundColor Yellow
        Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe" -ErrorAction SilentlyContinue
        Write-Host "  Đợi 15 giây để Docker Desktop khởi động..." -ForegroundColor Yellow
        Start-Sleep -Seconds 15
        
        # Kiểm tra lại
        $retries = 0
        while ($retries -lt 10) {
            docker ps > $null 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✓ Docker daemon đã sẵn sàng" -ForegroundColor Green
                break
            }
            Write-Host "  Đợi thêm 3 giây... ($retries/10)" -ForegroundColor Yellow
            Start-Sleep -Seconds 3
            $retries++
        }
        
        if ($retries -eq 10) {
            Write-Host "✗ Không thể kết nối đến Docker daemon sau 30 giây" -ForegroundColor Red
            Write-Host "  Vui lòng khởi động Docker Desktop thủ công và thử lại" -ForegroundColor Red
            exit 1
        }
    }
} catch {
    Write-Host "✗ Lỗi kiểm tra Docker daemon: $_" -ForegroundColor Red
    exit 1
}

# Tạo .env files nếu chưa có
Write-Host "[3/6] Kiểm tra file .env..." -ForegroundColor Yellow

# WEB .env
$webEnvPath = "WEB\.env"
if (-not (Test-Path $webEnvPath)) {
    Write-Host "  Tạo WEB\.env..." -ForegroundColor Yellow
    @"
# Django Backend Environment Variables
DJANGO_SECRET_KEY=dev-secret-key-change-in-production
DEBUG=True
ALLOWED_HOSTS=127.0.0.1,localhost,backend
CORS_ORIGINS=http://localhost:5173,http://localhost:5174
POSTGRES_DB=fastfood
POSTGRES_USER=app
POSTGRES_PASSWORD=123456
"@ | Out-File -FilePath $webEnvPath -Encoding utf8
    Write-Host "  ✓ Đã tạo WEB\.env" -ForegroundColor Green
} else {
    Write-Host "  ✓ WEB\.env đã tồn tại" -ForegroundColor Green
}

# APP .env
$appEnvPath = "APP\.env"
if (-not (Test-Path $appEnvPath)) {
    Write-Host "  Tạo APP\.env..." -ForegroundColor Yellow
    @"
# Node.js Backend Environment Variables
JWT_SECRET=supersecret
ADMIN_SECRET=adminkey
POSTGRES_DB=fastfood
POSTGRES_USER=app
POSTGRES_PASSWORD=123456
DEBUG_SHOW_OTP=true
ALLOW_SMOKE_SEED=true
DEV_TOKEN=testtoken
"@ | Out-File -FilePath $appEnvPath -Encoding utf8
    Write-Host "  ✓ Đã tạo APP\.env" -ForegroundColor Green
} else {
    Write-Host "  ✓ APP\.env đã tồn tại" -ForegroundColor Green
}

# Dừng các container cũ
Write-Host "[4/6] Dừng các container cũ..." -ForegroundColor Yellow
Set-Location WEB
docker compose down 2>&1 | Out-Null
Write-Host "  ✓ Đã dừng WEB containers" -ForegroundColor Green

Set-Location ..\APP
docker compose down 2>&1 | Out-Null
Write-Host "  ✓ Đã dừng APP containers" -ForegroundColor Green

Set-Location ..

# Khởi động WEB services
Write-Host "[5/6] Khởi động WEB services..." -ForegroundColor Yellow
Set-Location WEB
Write-Host "  Đang build và khởi động WEB (db, redis, backend, celery, frontend)..." -ForegroundColor Yellow
docker compose up -d --build
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✓ WEB services đã khởi động" -ForegroundColor Green
} else {
    Write-Host "  ✗ Có lỗi khi khởi động WEB services" -ForegroundColor Red
    Write-Host "  Xem logs: cd WEB && docker compose logs" -ForegroundColor Yellow
}

Set-Location ..

# Khởi động APP services
Write-Host "[6/6] Khởi động APP services..." -ForegroundColor Yellow
Set-Location APP
Write-Host "  Đang build và khởi động APP (db, redis, backend, adminer)..." -ForegroundColor Yellow
docker compose up -d --build
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✓ APP services đã khởi động" -ForegroundColor Green
} else {
    Write-Host "  ✗ Có lỗi khi khởi động APP services" -ForegroundColor Red
    Write-Host "  Xem logs: cd APP && docker compose logs" -ForegroundColor Yellow
}

Set-Location ..

# Hiển thị trạng thái
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TRẠNG THÁI SERVICES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "WEB Services:" -ForegroundColor Yellow
docker ps --filter "name=fastfood" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

Write-Host ""
Write-Host "APP Services:" -ForegroundColor Yellow
docker ps --filter "name=ff_" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  CÁC CỔNG ĐANG CHẠY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "WEB Backend:    http://localhost:8000" -ForegroundColor Green
Write-Host "WEB Frontend:   http://localhost:5174" -ForegroundColor Green
Write-Host "WEB Database:   localhost:5433" -ForegroundColor Green
Write-Host "WEB Redis:      localhost:6380" -ForegroundColor Green
Write-Host ""
Write-Host "APP Backend:    http://localhost:8001" -ForegroundColor Green
Write-Host "APP Database:   localhost:5433 (cùng với WEB)" -ForegroundColor Yellow
Write-Host "APP Redis:      localhost:6379" -ForegroundColor Green
Write-Host "APP Adminer:    http://localhost:8080" -ForegroundColor Green
Write-Host ""

Write-Host "Để xem logs:" -ForegroundColor Cyan
Write-Host "  WEB: cd WEB && docker compose logs -f" -ForegroundColor White
Write-Host "  APP: cd APP && docker compose logs -f" -ForegroundColor White
Write-Host ""

