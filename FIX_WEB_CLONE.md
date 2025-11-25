# Hướng dẫn sửa lỗi WEB folder trống khi clone từ GitHub

## 🔍 Nguyên nhân

WEB folder trống vì:
1. **WEB từng là git submodule** trong các commit cũ
2. Khi clone repository, submodule không được clone tự động
3. Cần pull về commit mới nhất hoặc clone lại đúng cách

## ✅ Giải pháp

### Cách 1: Pull về commit mới nhất (Khuyến nghị)

```powershell
# Di chuyển vào thư mục project
cd C:\Projects\fastfood

# Pull về commit mới nhất
git pull origin main

# Nếu WEB vẫn trống, xóa và pull lại
Remove-Item -Recurse -Force WEB -ErrorAction SilentlyContinue
git checkout HEAD -- WEB
```

### Cách 2: Clone lại repository

```powershell
# Xóa thư mục cũ (nếu cần)
Remove-Item -Recurse -Force C:\Projects\fastfood -ErrorAction SilentlyContinue

# Clone lại repository
git clone https://github.com/Thang121172/WEBAPP_DEPLOY_FASTFOOTDATDOAN.git C:\Projects\fastfood

# Di chuyển vào thư mục
cd C:\Projects\fastfood

# Kiểm tra WEB folder
ls WEB
```

### Cách 3: Nếu WEB vẫn là submodule

Nếu sau khi pull mà WEB vẫn trống, có thể nó vẫn là submodule:

```powershell
cd C:\Projects\fastfood

# Kiểm tra xem có .gitmodules không
if (Test-Path ".gitmodules") {
    Write-Host "WEB is a submodule, initializing..."
    git submodule update --init --recursive
} else {
    Write-Host "WEB is not a submodule"
}
```

### Cách 4: Reset và pull lại WEB folder

```powershell
cd C:\Projects\fastfood

# Xóa WEB folder
Remove-Item -Recurse -Force WEB -ErrorAction SilentlyContinue

# Reset về commit mới nhất
git fetch origin
git reset --hard origin/main

# Hoặc checkout lại WEB folder
git checkout origin/main -- WEB
```

## 🔍 Kiểm tra trạng thái

Sau khi thực hiện, kiểm tra:

```powershell
cd C:\Projects\fastfood

# Kiểm tra WEB có file không
ls WEB

# Kiểm tra git status
git status

# Kiểm tra xem WEB có phải submodule không
git ls-tree HEAD WEB
# Nếu thấy "160000" thì là submodule
# Nếu thấy "040000" thì là thư mục thông thường
```

## 📝 Lưu ý

- Commit mới nhất (763f353) đã chuyển WEB từ submodule sang thư mục thông thường
- Đảm bảo bạn đang ở branch `main` và commit mới nhất
- Nếu vẫn gặp vấn đề, thử clone lại repository hoàn toàn

## 🚀 Script tự động

Tạo file `fix_web_clone.ps1`:

```powershell
# Script tự động sửa lỗi WEB folder trống
Write-Host "Fixing WEB folder..." -ForegroundColor Yellow

$projectPath = "C:\Projects\fastfood"

if (-not (Test-Path $projectPath)) {
    Write-Host "Project folder not found at $projectPath" -ForegroundColor Red
    exit 1
}

Set-Location $projectPath

# Pull về commit mới nhất
Write-Host "Pulling latest changes..." -ForegroundColor Cyan
git pull origin main

# Kiểm tra WEB folder
if (-not (Test-Path "WEB") -or (Get-ChildItem "WEB" -ErrorAction SilentlyContinue).Count -eq 0) {
    Write-Host "WEB folder is empty, fixing..." -ForegroundColor Yellow
    
    # Xóa WEB folder
    Remove-Item -Recurse -Force WEB -ErrorAction SilentlyContinue
    
    # Checkout lại từ git
    git checkout HEAD -- WEB
    
    # Nếu vẫn trống, thử reset hard
    if (-not (Test-Path "WEB") -or (Get-ChildItem "WEB" -ErrorAction SilentlyContinue).Count -eq 0) {
        Write-Host "Resetting to latest commit..." -ForegroundColor Yellow
        git fetch origin
        git reset --hard origin/main
    }
}

# Kiểm tra kết quả
if ((Get-ChildItem "WEB" -ErrorAction SilentlyContinue).Count -gt 0) {
    Write-Host "✓ WEB folder fixed successfully!" -ForegroundColor Green
    Write-Host "WEB contains $((Get-ChildItem "WEB" -Recurse -File).Count) files" -ForegroundColor Green
} else {
    Write-Host "✗ Failed to fix WEB folder" -ForegroundColor Red
    Write-Host "Try cloning the repository again" -ForegroundColor Yellow
}
```


