# Script de chay Backend va Frontend trong 2 cua so PowerShell rieng biet

Write-Host "Dang khoi dong Backend va Frontend..." -ForegroundColor Cyan

# Lay duong dan thu muc hien tai
$rootDir = $PSScriptRoot
if (-not $rootDir) {
    $rootDir = Get-Location
}

$backendDir = Join-Path $rootDir "backend"
$frontendDir = Join-Path $rootDir "frontend"

# Kiem tra thu muc backend va frontend
if (-not (Test-Path $backendDir)) {
    Write-Host "Khong tim thay thu muc backend!" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $frontendDir)) {
    Write-Host "Khong tim thay thu muc frontend!" -ForegroundColor Red
    exit 1
}

# Kiem tra Python
$pythonPath = "python"
try {
    $pythonVersion = & $pythonPath --version 2>&1
    Write-Host "Tim thay Python: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "Khong tim thay Python trong PATH. Dang thu duong dan cu the..." -ForegroundColor Yellow
    $pythonPath = "C:\Users\ASUS\AppData\Local\Programs\Python\Python312\python.exe"
    if (Test-Path $pythonPath) {
        Write-Host "Tim thay Python tai: $pythonPath" -ForegroundColor Green
    } else {
        Write-Host "Khong tim thay Python! Vui long cai dat Python." -ForegroundColor Red
        exit 1
    }
}

# Kiem tra Node.js
try {
    $nodeVersion = & node --version 2>&1
    Write-Host "Tim thay Node.js: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "Khong tim thay Node.js! Vui long cai dat Node.js." -ForegroundColor Red
    exit 1
}

# Tao script cho Backend
$backendScriptContent = @"
# Backend Server
Write-Host "Dang khoi dong Django Backend..." -ForegroundColor Cyan
Set-Location "$backendDir"
Write-Host "Thu muc: `$(Get-Location)" -ForegroundColor Gray

# Kiem tra virtual environment
if (Test-Path "venv\Scripts\Activate.ps1") {
    Write-Host "Kich hoat virtual environment..." -ForegroundColor Green
    & "venv\Scripts\Activate.ps1"
} elseif (Test-Path "..\venv\Scripts\Activate.ps1") {
    Write-Host "Kich hoat virtual environment tu thu muc goc..." -ForegroundColor Green
    & "..\venv\Scripts\Activate.ps1"
}

# Chay Django server
Write-Host "`nDang chay Django development server..." -ForegroundColor Green
Write-Host "Backend API: http://localhost:8000" -ForegroundColor Yellow
Write-Host "Admin: http://localhost:8000/admin" -ForegroundColor Yellow
Write-Host "`nNhan Ctrl+C de dung server`n" -ForegroundColor Gray
& "$pythonPath" manage.py runserver
"@

# Tao script cho Frontend
$frontendScriptContent = @"
# Frontend Server
Write-Host "Dang khoi dong Vite Frontend..." -ForegroundColor Cyan
Set-Location "$frontendDir"
Write-Host "Thu muc: `$(Get-Location)" -ForegroundColor Gray

# Kiem tra node_modules
if (-not (Test-Path "node_modules")) {
    Write-Host "node_modules chua co. Dang cai dat dependencies..." -ForegroundColor Yellow
    npm install
}

# Chay Vite dev server
Write-Host "`nDang chay Vite development server..." -ForegroundColor Green
Write-Host "Frontend: http://localhost:5173" -ForegroundColor Yellow
Write-Host "`nNhan Ctrl+C de dung server`n" -ForegroundColor Gray
npm run dev
"@

# Luu script tam thoi
$backendScriptPath = Join-Path $env:TEMP "start-backend.ps1"
$frontendScriptPath = Join-Path $env:TEMP "start-frontend.ps1"

$backendScriptContent | Out-File -FilePath $backendScriptPath -Encoding UTF8
$frontendScriptContent | Out-File -FilePath $frontendScriptPath -Encoding UTF8

# Mo cua so PowerShell cho Backend
Write-Host "`nDang mo cua so Backend..." -ForegroundColor Cyan
Start-Process pwsh -ArgumentList "-NoExit", "-File", "`"$backendScriptPath`""

# Doi mot chut truoc khi mo cua so Frontend
Start-Sleep -Seconds 2

# Mo cua so PowerShell cho Frontend
Write-Host "Dang mo cua so Frontend..." -ForegroundColor Cyan
Start-Process pwsh -ArgumentList "-NoExit", "-File", "`"$frontendScriptPath`""

Write-Host "`nDa mo 2 cua so PowerShell:" -ForegroundColor Green
Write-Host "   - Backend: Django server tai http://localhost:8000" -ForegroundColor White
Write-Host "   - Frontend: Vite server tai http://localhost:5173" -ForegroundColor White
Write-Host "`nTip: Dong cua so PowerShell de dung server tuong ung" -ForegroundColor Gray
