@echo off
title Auto Consultancy - Backend Server
color 0A

echo.
echo  ======================================================
echo   AUTO CONSULTANCY  ^|  Spring Boot Backend
echo   http://localhost:8080
echo  ======================================================
echo.

:: Auto-detect JAVA_HOME if not set
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
)
set PATH=%JAVA_HOME%\bin;%PATH%

:: Verify Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install Java 17+ and set JAVA_HOME.
    pause & exit /b 1
)
echo [OK]   Java: %JAVA_HOME%

:: Find Maven from wrapper cache
set MVN_CMD=
for /f "delims=" %%i in ('dir /B /S "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.16\*\bin\mvn.cmd" 2^>nul') do (
    set "MVN_CMD=%%i"
    goto :found
)

:found
if "%MVN_CMD%"=="" (
    :: Try PATH as fallback
    where mvn >nul 2>&1
    if not errorlevel 1 (
        set MVN_CMD=mvn
        goto :start
    )
    echo [ERROR] Maven not found. Run backend once via mvnw to download Maven.
    pause & exit /b 1
)

:start
echo [OK]   Maven: %MVN_CMD%
echo.
echo [INFO] MySQL required on localhost:3306
echo [INFO] DB password in: src\main\resources\application.properties
echo [INFO] First run seeds: admin + 3 workers + 14 manufacturers + 90+ bike models
echo.

"%MVN_CMD%\" spring-boot:run

echo.
echo [INFO] Server stopped.
if errorlevel 1 (
    echo [ERROR] Startup failed. Check MySQL connection and port 8080 availability.
)
pause
