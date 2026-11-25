@echo off
echo ===================================================
echo  Starting LocalStack SQS on Windows (Port 4566)
echo ===================================================

where docker >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker is not installed or not in PATH!
    echo.
    echo To use LocalStack, please install Docker Desktop for Windows:
    echo   https://www.docker.com/products/docker-desktop/
    echo   Or run: winget install Docker.DockerDesktop
    echo.
    echo ---------------------------------------------------
    echo TIP: If you do NOT want to install Docker, you can run
    echo the lightweight native SQS server without Docker:
    echo   .\scripts\start-sqs-mock.bat
    echo ---------------------------------------------------
    pause
    exit /b 1
)

echo Checking if LocalStack container is already running...
docker ps -q -f name=mana-localstack > temp_container.txt
set /p CONTAINER_ID=<temp_container.txt
del temp_container.txt

if not "%CONTAINER_ID%"=="" (
    echo LocalStack is already running! (Container: %CONTAINER_ID%)
    echo Port: 4566 (http://localhost:4566)
    exit /b 0
)

echo Starting LocalStack SQS container...
docker run --rm -d --name mana-localstack -p 4566:4566 -e SERVICES=sqs -e AWS_DEFAULT_REGION=eu-north-1 localstack/localstack

echo.
echo LocalStack SQS is up and running on http://localhost:4566!
echo To stop LocalStack: docker stop mana-localstack
