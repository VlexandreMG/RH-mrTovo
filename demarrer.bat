@echo off
setlocal
cd /d "%~dp0"

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /C:"21." >nul
        if not errorlevel 1 goto java_ready
    )
    set "JAVA_HOME="
)

for /d %%J in ("%ProgramFiles%\Java\jdk-21*" "%ProgramFiles%\Eclipse Adoptium\jdk-21*") do (
    if exist "%%~J\bin\java.exe" (
        "%%~J\bin\java.exe" -version 2>&1 | findstr /C:"21." >nul
        if not errorlevel 1 (
            set "JAVA_HOME=%%~J"
            goto java_ready
        )
    )
)

echo Java 21 is required. Install JDK 21 or set JAVA_HOME to its location.
pause
exit /b 1

:java_ready
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0backend"
call "%~dp0backend\mvnw.cmd" spring-boot:run
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
    echo.
    echo Spring Boot stopped with exit code %EXIT_CODE%.
    pause
)
exit /b %EXIT_CODE%
