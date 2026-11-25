@echo off
echo =====================================================================
echo  Zero-Docker Native Local SQS Server (ElasticMQ)
echo  Runs directly on Windows via Java on port 4566 (LocalStack compatible)
echo =====================================================================

set SQS_DIR=%~dp0..\.sqs-mock
set JAR_PATH=%SQS_DIR%\elasticmq-server-1.5.8.jar
set JAR_URL=https://s3-eu-west-1.amazonaws.com/softwaremill-public/elasticmq-server-1.5.8.jar

if not exist "%SQS_DIR%" mkdir "%SQS_DIR%"

if not exist "%JAR_PATH%" (
    echo Downloading ElasticMQ Standalone SQS Server (36MB)...
    powershell -Command "Invoke-WebRequest -Uri '%JAR_URL%' -OutFile '%JAR_PATH%'"
    if %ERRORLEVEL% neq 0 (
        echo [ERROR] Failed to download ElasticMQ jar.
        pause
        exit /b 1
    )
    echo Download complete!
)

echo.
echo Starting AWS SQS Server on http://localhost:4566 ...
echo Press Ctrl+C to stop.
echo.

java -Dnode-address.protocol=http -Dnode-address.host=localhost -Dnode-address.port=4566 -Drest-sqs.bind-port=4566 -Drest-sqs.bind-hostname=0.0.0.0 -jar "%JAR_PATH%"
