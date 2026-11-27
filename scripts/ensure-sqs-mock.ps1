# =====================================================================
#  Idempotent ElasticMQ Local SQS Launcher (for IntelliJ Before-Launch)
#  Checks 127.0.0.1:4566; if not listening, starts ElasticMQ detached
#  in the background, waits until port 4566 accepts TCP, then exits 0.
# =====================================================================

$ErrorActionPreference = "Stop"

function Test-PortOpen {
    param([string]$TargetHost = "127.0.0.1", [int]$Port = 4566)
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $iar = $client.BeginConnect($TargetHost, $Port, $null, $null)
        if ($iar.AsyncWaitHandle.WaitOne(250, $false)) {
            $client.EndConnect($iar)
            return $true
        }
        return $false
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

if (Test-PortOpen) {
    Write-Host "ElasticMQ local SQS is already running on http://localhost:4566" -ForegroundColor Green
    exit 0
}

$jarDir = Join-Path $PSScriptRoot "..\.sqs-mock"
if (-not (Test-Path $jarDir)) {
    New-Item -ItemType Directory -Path $jarDir -Force | Out-Null
}

$jarPath = (Resolve-Path $jarDir).Path | Join-Path -ChildPath "elasticmq-server-1.5.8.jar"
$jarUrl = "https://s3-eu-west-1.amazonaws.com/softwaremill-public/elasticmq-server-1.5.8.jar"

if (-not (Test-Path $jarPath)) {
    Write-Host "Downloading ElasticMQ Standalone SQS Server (36MB)..." -ForegroundColor Cyan
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $jarUrl -OutFile $jarPath
    Write-Host "Download complete!" -ForegroundColor Green
}

$stdoutLog = Join-Path (Resolve-Path $jarDir).Path "elasticmq.out.log"
$stderrLog = Join-Path (Resolve-Path $jarDir).Path "elasticmq.err.log"

$javaArgs = @(
    '"-Dnode-address.protocol=http"'
    '"-Dnode-address.host=localhost"'
    '"-Dnode-address.port=4566"'
    '"-Drest-sqs.bind-port=4566"'
    '"-Drest-sqs.bind-hostname=0.0.0.0"'
    '-jar'
    "`"$jarPath`""
)

Write-Host "Starting ElasticMQ local SQS server in background on http://localhost:4566..." -ForegroundColor Cyan
Start-Process -FilePath "java" -ArgumentList $javaArgs -WindowStyle Hidden `
    -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog | Out-Null

for ($i = 0; $i -lt 40; $i++) {
    if (Test-PortOpen) {
        Write-Host "ElasticMQ local SQS is ready on http://localhost:4566" -ForegroundColor Green
        exit 0
    }
    Start-Sleep -Milliseconds 500
}

Write-Error "ElasticMQ failed to listen on port 4566 within 20 seconds. See $stderrLog"
exit 1
