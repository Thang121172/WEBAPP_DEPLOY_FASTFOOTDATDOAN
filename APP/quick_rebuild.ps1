# Quick rebuild và install
cd app
Write-Host "Cleaning..." -ForegroundColor Yellow
.\gradlew.bat clean
Write-Host "Building..." -ForegroundColor Yellow  
.\gradlew.bat assembleDebug
Write-Host "Installing..." -ForegroundColor Yellow
adb install -r build\outputs\apk\debug\app-debug.apk
cd ..

