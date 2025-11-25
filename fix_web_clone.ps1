# Script tự động sửa lỗi WEB folder trống
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  FIX WEB FOLDER EMPTY ISSUE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$projectPath = Read-Host "Enter project path (default: C:\Projects\fastfood)"
if ([string]::IsNullOrWhiteSpace($projectPath)) {
    $projectPath = "C:\Projects\fastfood"
}

if (-not (Test-Path $projectPath)) {
    Write-Host "Project folder not found at $projectPath" -ForegroundColor Red
    Write-Host "Please clone the repository first:" -ForegroundColor Yellow
    Write-Host "git clone https://github.com/Thang121172/WEBAPP_DEPLOY_FASTFOOTDATDOAN.git $projectPath" -ForegroundColor White
    exit 1
}

Set-Location $projectPath

# Kiểm tra có phải git repository không
if (-not (Test-Path ".git")) {
    Write-Host "Not a git repository!" -ForegroundColor Red
    exit 1
}

Write-Host "Current directory: $(Get-Location)" -ForegroundColor Gray
Write-Host ""

# Kiểm tra branch hiện tại
$currentBranch = git branch --show-current
Write-Host "Current branch: $currentBranch" -ForegroundColor Cyan
Write-Host ""

# Pull về commit mới nhất
Write-Host "[1/4] Pulling latest changes..." -ForegroundColor Yellow
git pull origin main
if ($LASTEXITCODE -ne 0) {
    Write-Host "Warning: Failed to pull, continuing anyway..." -ForegroundColor Yellow
}
Write-Host ""

# Kiểm tra WEB folder
Write-Host "[2/4] Checking WEB folder..." -ForegroundColor Yellow
$webExists = Test-Path "WEB"
$webEmpty = $false

if ($webExists) {
    $webFiles = Get-ChildItem "WEB" -ErrorAction SilentlyContinue
    $webEmpty = ($webFiles.Count -eq 0)
    Write-Host "WEB folder exists: $webExists" -ForegroundColor $(if ($webExists) { "Green" } else { "Red" })
    Write-Host "WEB folder empty: $webEmpty" -ForegroundColor $(if ($webEmpty) { "Yellow" } else { "Green" })
} else {
    Write-Host "WEB folder does not exist" -ForegroundColor Red
}
Write-Host ""

# Kiểm tra xem WEB có phải submodule không
Write-Host "[3/4] Checking if WEB is a submodule..." -ForegroundColor Yellow
$isSubmodule = $false
if (Test-Path ".gitmodules") {
    $gitmodules = Get-Content ".gitmodules"
    if ($gitmodules -match "WEB") {
        $isSubmodule = $true
        Write-Host "WEB is a submodule" -ForegroundColor Yellow
    }
}

$lsTree = git ls-tree HEAD WEB 2>&1
if ($lsTree -match "160000") {
    $isSubmodule = $true
    Write-Host "WEB is detected as submodule (mode 160000)" -ForegroundColor Yellow
}

if ($isSubmodule) {
    Write-Host "Initializing submodule..." -ForegroundColor Cyan
    git submodule update --init --recursive WEB
    Write-Host ""
}

# Fix WEB folder nếu trống
if (-not $webExists -or $webEmpty) {
    Write-Host "[4/4] Fixing WEB folder..." -ForegroundColor Yellow
    
    # Xóa WEB folder nếu tồn tại
    if ($webExists) {
        Write-Host "Removing empty WEB folder..." -ForegroundColor Gray
        Remove-Item -Recurse -Force WEB -ErrorAction SilentlyContinue
    }
    
    # Checkout lại từ git
    Write-Host "Checking out WEB from git..." -ForegroundColor Gray
    git checkout HEAD -- WEB 2>&1 | Out-Null
    
    # Nếu vẫn trống, thử reset hard
    if (-not (Test-Path "WEB") -or (Get-ChildItem "WEB" -ErrorAction SilentlyContinue).Count -eq 0) {
        Write-Host "WEB still empty, fetching and resetting..." -ForegroundColor Yellow
        git fetch origin
        git reset --hard origin/main
    }
    
    Write-Host ""
}

# Kiểm tra kết quả
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RESULT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if (Test-Path "WEB") {
    $webFiles = Get-ChildItem "WEB" -Recurse -File -ErrorAction SilentlyContinue
    $webDirs = Get-ChildItem "WEB" -Recurse -Directory -ErrorAction SilentlyContinue
    
    if ($webFiles.Count -gt 0) {
        Write-Host "✓ WEB folder fixed successfully!" -ForegroundColor Green
        Write-Host "  Files: $($webFiles.Count)" -ForegroundColor Green
        Write-Host "  Directories: $($webDirs.Count)" -ForegroundColor Green
        Write-Host ""
        Write-Host "Sample files:" -ForegroundColor Cyan
        Get-ChildItem "WEB" -File | Select-Object -First 5 | ForEach-Object {
            Write-Host "  - $($_.Name)" -ForegroundColor Gray
        }
    } else {
        Write-Host "✗ WEB folder still empty" -ForegroundColor Red
        Write-Host ""
        Write-Host "Try these solutions:" -ForegroundColor Yellow
        Write-Host "1. Clone repository again:" -ForegroundColor White
        Write-Host "   git clone https://github.com/Thang121172/WEBAPP_DEPLOY_FASTFOOTDATDOAN.git" -ForegroundColor Gray
        Write-Host ""
        Write-Host "2. Check if you're on the latest commit:" -ForegroundColor White
        Write-Host "   git log --oneline -1" -ForegroundColor Gray
        Write-Host "   Should show: 'Add complete project: APP (Android + Backend) and WEB directories'" -ForegroundColor Gray
    }
} else {
    Write-Host "✗ WEB folder does not exist" -ForegroundColor Red
    Write-Host "Please clone the repository again" -ForegroundColor Yellow
}

Write-Host ""


