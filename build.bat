@echo off
setlocal
cd /d "%~dp0"
javac src\*.java
if errorlevel 1 exit /b 1
endlocal
