# Script to compress and upload project to VPS
# VPS IP: 103.75.182.180

$VPS_IP = "103.75.182.180"
$PROJECT_DIR = "T:\FASTFOOD"
$ARCHIVE_NAME = "FASTFOOD_project_$(Get-Date -Format 'yyyyMMdd_HHmmss').tar.gz"
$ARCHIVE_PATH = Join-Path $env:TEMP $ARCHIVE_NAME

Write-Host "=== Compressing FASTFOOD project ===" -ForegroundColor Green
Write-Host "Creating archive: $ARCHIVE_NAME" -ForegroundColor Yellow

# Change to parent directory
$ParentDir = Split-Path -Parent $PROJECT_DIR
$ProjectName = Split-Path -Leaf $PROJECT_DIR

Push-Location $ParentDir

try {
    # Use tar (available in Windows 10+)
    # Exclude unnecessary files/directories
    $excludePatterns = @(
        "node_modules",
        "build",
        "*.log",
        ".git",
        "hs_err_*.log",
        "replay_*.log",
        "app_backend_logs*.txt",
        "app_restart.txt",
        "app_status.txt",
        "docker_status.txt",
        "web_startup*.txt",
        "web_status.txt",
        "*.tmp",
        "tmp_log_extract.txt",
        "uploads",
        ".next",
        "dist",
        "__pycache__",
        "*.pyc"
    )

    # Create exclude list for tar
    $excludeArgs = @()
    foreach ($pattern in $excludePatterns) {
        $excludeArgs += "--exclude=$pattern"
    }

    # Create archive
    Write-Host "Compressing project (this may take a few minutes)..." -ForegroundColor Yellow
    Write-Host "Excluding: node_modules, build, logs, uploads..." -ForegroundColor Gray
    
    $tarArgs = @("czf", $ARCHIVE_PATH) + $excludeArgs + $ProjectName
    & tar $tarArgs
    
    if ($LASTEXITCODE -eq 0) {
        $fileSize = (Get-Item $ARCHIVE_PATH).Length / 1MB
        Write-Host "Archive created successfully!" -ForegroundColor Green
        Write-Host "File: $ARCHIVE_PATH" -ForegroundColor Cyan
        Write-Host "Size: $([math]::Round($fileSize, 2)) MB" -ForegroundColor Cyan
        Write-Host ""
        
        Write-Host "=== Instructions to upload to VPS ===" -ForegroundColor Green
        Write-Host ""
        Write-Host "Step 1: Copy file to VPS using SCP:" -ForegroundColor Yellow
        Write-Host "scp `"$ARCHIVE_PATH`" root@${VPS_IP}:/root/" -ForegroundColor White
        Write-Host ""
        Write-Host "Or if using different username (replace 'root' with your username):" -ForegroundColor Yellow
        Write-Host "scp `"$ARCHIVE_PATH`" your_username@${VPS_IP}:/home/your_username/" -ForegroundColor White
        Write-Host ""
        Write-Host "Step 2: SSH into VPS and extract:" -ForegroundColor Yellow
        Write-Host "ssh root@${VPS_IP}" -ForegroundColor White
        Write-Host "cd /root" -ForegroundColor White
        Write-Host "tar -xzf $ARCHIVE_NAME" -ForegroundColor White
        Write-Host ""
        
        # Ask if want to auto upload
        $upload = Read-Host "Do you want to upload to VPS now? (y/n)"
        if ($upload -eq "y" -or $upload -eq "Y") {
            $username = Read-Host "Enter SSH username (default: root)"
            if ([string]::IsNullOrWhiteSpace($username)) {
                $username = "root"
            }
            
            $remotePath = Read-Host "Enter destination path on VPS (default: /root/)"
            if ([string]::IsNullOrWhiteSpace($remotePath)) {
                $remotePath = "/root/"
            }
            
            Write-Host ""
            Write-Host "Uploading to VPS..." -ForegroundColor Yellow
            Write-Host "Note: You will be prompted for SSH password" -ForegroundColor Yellow
            Write-Host ""
            
            scp $ARCHIVE_PATH "${username}@${VPS_IP}:${remotePath}"
            
            if ($LASTEXITCODE -eq 0) {
                Write-Host ""
                Write-Host "Upload successful!" -ForegroundColor Green
                Write-Host ""
                Write-Host "Now SSH into VPS and extract:" -ForegroundColor Yellow
                Write-Host "ssh ${username}@${VPS_IP}" -ForegroundColor White
                Write-Host "cd ${remotePath}" -ForegroundColor White
                Write-Host "tar -xzf $ARCHIVE_NAME" -ForegroundColor White
            } else {
                Write-Host "Upload failed. Please check SSH connection." -ForegroundColor Red
            }
        }
    } else {
        Write-Host "Error creating archive" -ForegroundColor Red
    }
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
} finally {
    Pop-Location
}
