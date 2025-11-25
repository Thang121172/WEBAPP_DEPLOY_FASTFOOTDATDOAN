# Script PowerShell để dừng Backend và Frontend
# Chạy: .\stop.ps1

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  DỪNG BACKEND VÀ FRONTEND" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "🛑 Đang dừng các services..." -ForegroundColor Yellow
docker-compose stop backend frontend

Write-Host ""
Write-Host "✓ Đã dừng Backend và Frontend" -ForegroundColor Green
Write-Host ""

Write-Host "Lưu ý: Database và Redis vẫn đang chạy." -ForegroundColor Yellow
Write-Host "Nếu muốn dừng tất cả, chạy: docker-compose stop" -ForegroundColor Yellow
Write-Host ""

