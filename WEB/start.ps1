# Script PowerShell để khởi động Backend và Frontend
# Chạy: .\start.ps1

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  KHỞI ĐỘNG BACKEND VÀ FRONTEND" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra Docker có đang chạy không
Write-Host "📦 Kiểm tra Docker..." -ForegroundColor Yellow
$dockerRunning = docker info 2>&1 | Select-String -Pattern "Server Version"
if (-not $dockerRunning) {
    Write-Host "❌ Docker không chạy! Vui lòng khởi động Docker Desktop trước." -ForegroundColor Red
    exit 1
}
Write-Host "✓ Docker đang chạy" -ForegroundColor Green
Write-Host ""

# Kiểm tra và khởi động database và redis
Write-Host "🗄️  Khởi động Database và Redis..." -ForegroundColor Yellow
docker-compose up -d db redis
Start-Sleep -Seconds 2
Write-Host "✓ Database và Redis đã sẵn sàng" -ForegroundColor Green
Write-Host ""

# Khởi động Backend
Write-Host "🚀 Khởi động Backend..." -ForegroundColor Yellow
docker-compose up -d backend
Start-Sleep -Seconds 3

# Kiểm tra Backend có chạy không
$backendStatus = docker-compose ps backend | Select-String -Pattern "Up"
if ($backendStatus) {
    Write-Host "✓ Backend đang chạy tại: http://localhost:8000" -ForegroundColor Green
} else {
    Write-Host "⚠️  Backend có thể đang khởi động..." -ForegroundColor Yellow
}
Write-Host ""

# Khởi động Frontend
Write-Host "🎨 Khởi động Frontend..." -ForegroundColor Yellow
docker-compose up -d frontend
Start-Sleep -Seconds 2

# Kiểm tra Frontend có chạy không
$frontendStatus = docker-compose ps frontend | Select-String -Pattern "Up"
if ($frontendStatus) {
    Write-Host "✓ Frontend đang chạy tại: http://localhost:5174" -ForegroundColor Green
} else {
    Write-Host "⚠️  Frontend có thể đang khởi động..." -ForegroundColor Yellow
}
Write-Host ""

# Hiển thị trạng thái
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  TRẠNG THÁI CÁC SERVICES" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
docker-compose ps
Write-Host ""

# Mở trình duyệt
Write-Host "🌐 Bạn có muốn mở trình duyệt? (Y/N): " -ForegroundColor Yellow -NoNewline
$response = Read-Host
if ($response -eq "Y" -or $response -eq "y") {
    Start-Process "http://localhost:5174"
    Write-Host "✓ Đã mở trình duyệt" -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  CÁC LỆNH HỮU ÍCH" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Xem logs Backend:     docker-compose logs -f backend" -ForegroundColor White
Write-Host "Xem logs Frontend:    docker-compose logs -f frontend" -ForegroundColor White
Write-Host "Dừng tất cả:          docker-compose stop" -ForegroundColor White
Write-Host "Khởi động lại:        docker-compose restart backend frontend" -ForegroundColor White
Write-Host ""

