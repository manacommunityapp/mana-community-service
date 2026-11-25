@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0ensure-sqs-mock.ps1"
exit /b %ERRORLEVEL%
