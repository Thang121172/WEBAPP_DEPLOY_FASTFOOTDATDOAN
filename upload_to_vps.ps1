# Script to upload project archive to VPS
# VPS IP: 103.75.182.180

$VPS_IP = "103.75.182.180"
$ARCHIVE_FILE = "T:\FASTFOOD_project_20251126_021844.tar.gz"

# Check if archive exists
if (-not (Test-Path $ARCHIVE_FILE)) {
    Write-Host "Error: Archive file not found at $ARCHIVE_FILE" -ForegroundColor Red
    Write-Host "Please run compress_and_upload.ps1 first to create the archive." -ForegroundColor Yellow
    exit 1
}

Write-Host "=== Upload to VPS ===" -ForegroundColor Green
Write-Host "Archive: $ARCHIVE_FILE" -ForegroundColor Cyan
$fileSize = (Get-Item $ARCHIVE_FILE).Length / 1MB
Write-Host "Size: $([math]::Round($fileSize, 2)) MB" -ForegroundColor Cyan
Write-Host ""

# Get SSH username
$username = Read-Host "Enter SSH username (default: root)"
if ([string]::IsNullOrWhiteSpace($username)) {
    $username = "root"
}

# Get remote path
$remotePath = Read-Host "Enter destination path on VPS (default: /root/)"
if ([string]::IsNullOrWhiteSpace($remotePath)) {
    $remotePath = "/root/"
}

# Ensure remote path ends with /
if (-not $remotePath.EndsWith("/")) {
    $remotePath += "/"
}

Write-Host ""
Write-Host "Uploading to ${username}@${VPS_IP}:${remotePath}..." -ForegroundColor Yellow
Write-Host "You will be prompted for SSH password" -ForegroundColor Yellow
Write-Host ""

# Upload using SCP
scp $ARCHIVE_FILE "${username}@${VPS_IP}:${remotePath}"

if ($LASTEXITCODE -eq 0) {
    $archiveName = Split-Path -Leaf $ARCHIVE_FILE
    Write-Host ""
    Write-Host "Upload successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. SSH into VPS:" -ForegroundColor White
    Write-Host "   ssh ${username}@${VPS_IP}" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "2. Navigate to destination and extract:" -ForegroundColor White
    Write-Host "   cd ${remotePath}" -ForegroundColor Cyan
    Write-Host "   tar -xzf $archiveName" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "3. The project will be extracted as FASTFOOD/" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "Upload failed. Please check:" -ForegroundColor Red
    Write-Host "- SSH connection to VPS" -ForegroundColor Yellow
    Write-Host "- Username and password" -ForegroundColor Yellow
    Write-Host "- Network connectivity" -ForegroundColor Yellow
}

