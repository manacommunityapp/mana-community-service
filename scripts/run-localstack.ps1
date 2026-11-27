# ===================================================
#  Starting LocalStack SQS on Windows (Port 4566)
# ===================================================

$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    Write-Host "[ERROR] Docker is not installed or not in PATH!" -ForegroundColor Red
    Write-Host ""
    Write-Host "To use LocalStack with Docker, install Docker Desktop for Windows:" -ForegroundColor Yellow
    Write-Host "  https://www.docker.com/products/docker-desktop/" -ForegroundColor Cyan
    Write-Host "  Or run: winget install Docker.DockerDesktop" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "---------------------------------------------------------" -ForegroundColor Green
    Write-Host "TIP: If you don't want Docker, run the Zero-Docker native SQS mock:" -ForegroundColor Green
    Write-Host "  .\scripts\start-sqs-mock.ps1" -ForegroundColor Green
    Write-Host "---------------------------------------------------------" -ForegroundColor Green
    exit 1
}

$running = docker ps -q -f name=mana-localstack
if ($running) {
    Write-Host "LocalStack is already running in container $running on http://localhost:4566" -ForegroundColor Green
    exit 0
}

Write-Host "Starting LocalStack container (mana-localstack) on port 4566..." -ForegroundColor Cyan
docker run --rm -d --name mana-localstack -p 4566:4566 -e SERVICES=sqs -e AWS_DEFAULT_REGION=eu-north-1 localstack/localstack

Write-Host ""
Write-Host "LocalStack SQS is up and running on http://localhost:4566" -ForegroundColor Green
Write-Host "To stop: docker stop mana-localstack" -ForegroundColor Yellow
