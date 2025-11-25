param(
    [string]$Port = "8082",
    [string]$DbHost = "localhost",
    [string]$DbPort = "5433",
    [string]$DbName = "fastfood",
    [string]$DbUser = "app",
    [string]$DbPass = "123456"
)

Write-Host "Starting backend on port $Port with ALLOW_SMOKE_SEED=true"
$env:DB_HOST=$DbHost
$env:DB_PORT=$DbPort
$env:DB_NAME=$DbName
$env:DB_USER=$DbUser
$env:DB_PASSWORD=$DbPass
$env:PORT=$Port
$env:ALLOW_SMOKE_SEED='true'

$proc = Start-Process -FilePath 'node' -ArgumentList 'index.js' -WorkingDirectory (Join-Path $PSScriptRoot '..') -PassThru -NoNewWindow
try {
    $base = "http://localhost:$Port"
    Write-Host "Waiting for backend to become responsive at $base/menu"
    $ok = $false
    for ($i=0; $i -lt 30; $i++) {
        try {
            $r = Invoke-WebRequest -Uri "$base/menu" -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -eq 200) { $ok = $true; break }
        } catch { Start-Sleep -Seconds 1 }
    }
    if (-not $ok) { throw "Backend did not become ready" }

    Write-Host "Backend is ready. Running smoke test..."
    # Set env for tests in this process
    $env:BASE = $base
    $env:SMOKE_DB_HOST = $DbHost
    $env:SMOKE_DB_PORT = $DbPort
    $env:SMOKE_DB_NAME = $DbName
    $env:SMOKE_DB_USER = $DbUser
    $env:SMOKE_DB_PASS = $DbPass

    Push-Location (Join-Path $PSScriptRoot '..')
    try {
        node tests/smoke.js
        $smokeCode = $LASTEXITCODE
        Write-Host "Smoke test exit code: $smokeCode"

        Write-Host "Running refund test..."
        node tests/refund_test.js
        $refundCode = $LASTEXITCODE
        Write-Host "Refund test exit code: $refundCode"
        Write-Host "Running logout test..."
        node tests/logout_test.js
        $logoutCode = $LASTEXITCODE
        Write-Host "Logout test exit code: $logoutCode"
    } finally { Pop-Location }

    if ($smokeCode -eq 0 -and $refundCode -eq 0 -and $logoutCode -eq 0) {
        Write-Host "All tests passed"
        exit 0
    } else {
        Write-Host "Some tests failed (smoke: $smokeCode, refund: $refundCode, logout: $logoutCode)"
        exit 2
    }
} finally {
    if ($proc -and -not $proc.HasExited) {
        Write-Host "Stopping backend (PID $($proc.Id))"
        $proc.Kill()
    }
}
