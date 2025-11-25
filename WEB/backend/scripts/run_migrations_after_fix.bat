@echo off
REM Script để chạy migrations sau khi đã xóa migration records
echo ========================================
echo Chạy migrations cho orders app
echo ========================================
cd /d %~dp0..
py -3.13 manage.py migrate orders
echo.
echo ========================================
echo Kiểm tra migration status
echo ========================================
py -3.13 manage.py showmigrations orders
echo.
echo ========================================
echo Hoàn thành!
echo ========================================
pause

