@echo off
echo Adding all files...
git add -A

echo.
echo Committing changes...
git commit -m "Add Vercel configuration and deployment documentation"

echo.
echo Pushing to GitHub...
git push origin main

echo.
echo Done! Press any key to exit...
pause

