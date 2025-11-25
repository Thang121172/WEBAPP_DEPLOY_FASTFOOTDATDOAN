# Commit and push all changes to GitHub

Write-Host "Adding all files..." -ForegroundColor Yellow
git add -A

Write-Host "Checking status..." -ForegroundColor Yellow
git status --short

Write-Host "`nCommitting changes..." -ForegroundColor Yellow
git commit -m "Add Vercel configuration and deployment documentation

- Add vercel.json for Vercel deployment  
- Add comprehensive deployment guides
- Add troubleshooting documentation
- Update backend core/app.py
- Add deployment helper scripts"

Write-Host "`nPushing to GitHub..." -ForegroundColor Yellow
git push origin main

Write-Host "`nDone! ✅" -ForegroundColor Green

