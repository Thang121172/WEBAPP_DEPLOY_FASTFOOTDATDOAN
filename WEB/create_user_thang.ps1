# Script tạo user "thang" trên VPS
# Chạy script này trên VPS (qua Remote Desktop hoặc SSH)

Write-Host "=== TẠO USER THANG ===" -ForegroundColor Cyan

try {
    # Kiểm tra xem user đã tồn tại chưa
    $existingUser = Get-LocalUser -Name "thang" -ErrorAction SilentlyContinue
    if ($existingUser) {
        Write-Host "⚠️  User 'thang' đã tồn tại!" -ForegroundColor Yellow
        Write-Host "Tên user: $($existingUser.Name)"
        Write-Host "Mô tả: $($existingUser.Description)"
    } else {
        # Tạo user mới
        $securePassword = ConvertTo-SecureString "Thang2004" -AsPlainText -Force
        New-LocalUser -Name "thang" -Password $securePassword -FullName "Thang" -Description "User created for SSH access"
        Write-Host "✅ Đã tạo user: thang" -ForegroundColor Green
        
        # Thêm user vào nhóm Remote Desktop Users
        Add-LocalGroupMember -Group "Remote Desktop Users" -Member "thang" -ErrorAction SilentlyContinue
        Write-Host "✅ Đã thêm user vào nhóm Remote Desktop Users" -ForegroundColor Green
        
        Write-Host ""
        Write-Host "=== THÔNG TIN USER ===" -ForegroundColor Cyan
        Write-Host "Tên user: thang"
        Write-Host "Password: Thang2004"
    }
    
    Write-Host ""
    Write-Host "✅ HOÀN TẤT!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Bước tiếp theo: Chạy script add_ssh_key_to_user_thang.ps1 để thêm SSH key" -ForegroundColor Yellow
    
} catch {
    Write-Host "❌ Lỗi: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

