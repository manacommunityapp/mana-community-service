# =====================================================================
#  Zero-Docker Native Local SQS Server (ElasticMQ)
#  Runs directly on Windows via Java 17+ on port 4566 (LocalStack compatible)
# =====================================================================

$jarDir = Join-Path $PSScriptRoot "..\.sqs-mock"
if (-not (Test-Path $jarDir)) {
    New-Item -ItemType Directory -Path $jarDir -Force | Out-Null
}

$jarPath = Join-Path $jarDir "elasticmq-server-1.5.8.jar"
$jarUrl = "https://s3-eu-west-1.amazonaws.com/softwaremill-public/elasticmq-server-1.5.8.jar"

if (-not (Test-Path $jarPath)) {
    Write-Host "Downloading ElasticMQ Standalone SQS Server (36MB)..." -ForegroundColor Cyan
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $jarUrl -OutFile $jarPath
    Write-Host "Download complete!" -ForegroundColor Green
}

Write-Host "=========================================================" -ForegroundColor Yellow
Write-Host " Starting Native AWS SQS Server on http://localhost:4566" -ForegroundColor Green
Write-Host " Region:     eu-north-1" -ForegroundColor Cyan
Write-Host " Port:       4566" -ForegroundColor Cyan
Write-Host " Zero Docker required! Press Ctrl+C to stop." -ForegroundColor Yellow
Write-Host "=========================================================" -ForegroundColor Yellow

& java "-Dnode-address.protocol=http" "-Dnode-address.host=localhost" "-Dnode-address.port=4566" "-Drest-sqs.bind-port=4566" "-Drest-sqs.bind-hostname=0.0.0.0" -jar "$jarPath"
