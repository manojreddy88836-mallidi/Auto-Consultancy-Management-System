@echo off
title Auto Consultancy - Frontend Dev Server
color 0B

echo.
echo  ======================================================
echo   AUTO CONSULTANCY  ^|  React Frontend (Vite)
echo   http://localhost:5173
echo  ======================================================
echo.

:: Check Node.js
node -v >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js not found. Please install Node.js 18+ from https://nodejs.org
    pause & exit /b 1
)

for /f "delims=" %%v in ('node -v') do echo [OK]   Node.js: %%v

:: Auto-install dependencies if node_modules missing
if not exist "node_modules\" (
    echo [INFO] First run - installing dependencies...
    echo.
    call npm install
    if errorlevel 1 (
        echo [ERROR] npm install failed. Check your internet connection.
        pause & exit /b 1
    )
)

echo [OK]   Dependencies ready
echo.
echo [INFO] Backend must be running on http://localhost:8080
echo [INFO] API calls (/api/*) are proxied to backend automatically
echo.

call npm run dev

pause
