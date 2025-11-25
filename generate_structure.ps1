# Script to generate project structure tree

function Get-ProjectTree {
    param(
        [string]$RootPath,
        [string]$OutputFile,
        [string]$ProjectName
    )
    
    $excludeDirs = @('node_modules', 'build', '.git', '.gradle', '.idea', '.vscode', 'uploads', 'dist', '.next', '__pycache__', 'venv', 'env', '.venv', '.DS_Store')
    $excludeExts = @('.log', '.tmp', '.pyc', '.apk', '.aab', '.jar', '.DS_Store')
    
    $script:output = @()
    $script:output += "$ProjectName Project Structure"
    $script:output += "=" * 50
    $script:output += ""
    
    function Get-Tree {
        param(
            [string]$Path,
            [string]$Prefix = "",
            [bool]$IsLast = $true
        )
        
        try {
            $items = Get-ChildItem -Path $Path -ErrorAction SilentlyContinue | 
                Where-Object { 
                    $excludeDirs -notcontains $_.Name -and
                    ($_.PSIsContainer -or ($_.Extension -notin $excludeExts -and $_.Name -notmatch 'Thumbs\.db'))
                } | 
                Sort-Object { -$_.PSIsContainer }, Name
            
            $count = $items.Count
            $index = 0
            
            foreach ($item in $items) {
                $index++
                $isLastItem = ($index -eq $count)
                
                if ($isLastItem) {
                    $connector = "+-- "
                    $nextPrefix = $Prefix + "    "
                } else {
                    $connector = "+-- "
                    $nextPrefix = $Prefix + "|   "
                }
                
                if ($item.PSIsContainer) {
                    $script:output += "$Prefix$connector$($item.Name)/"
                    Get-Tree -Path $item.FullName -Prefix $nextPrefix -IsLast $isLastItem
                } else {
                    $script:output += "$Prefix$connector$($item.Name)"
                }
            }
        } catch {
            # Ignore errors
        }
    }
    
    Get-Tree -Path $RootPath
    $script:output | Out-File -FilePath $OutputFile -Encoding UTF8
    Write-Host "Generated: $OutputFile ($($script:output.Count) lines)" -ForegroundColor Green
}

# Generate APP structure
Write-Host "Generating APP structure..." -ForegroundColor Yellow
Get-ProjectTree -RootPath "T:\FASTFOOD\APP" -OutputFile "T:\FASTFOOD\APP_structure.txt" -ProjectName "APP"

# Generate WEB structure
Write-Host "Generating WEB structure..." -ForegroundColor Yellow
Get-ProjectTree -RootPath "T:\FASTFOOD\WEB" -OutputFile "T:\FASTFOOD\WEB_structure.txt" -ProjectName "WEB"

Write-Host "`nDone! Check APP_structure.txt and WEB_structure.txt" -ForegroundColor Green
