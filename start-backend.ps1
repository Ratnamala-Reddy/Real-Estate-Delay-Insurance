# Script to start all backend services in separate terminal windows

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host " Starting Real Estate Backend Services   " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Start Eureka Server first
Write-Host "[1/9] Starting Eureka Server (Port 8761)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'Eureka-Server'; .\mvnw.cmd spring-boot:run"
Start-Sleep -Seconds 8

# 2. Start API Gateway
Write-Host "[2/9] Starting API Gateway (Port 8080)..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'api-gateway'; .\mvnw.cmd spring-boot:run"
Start-Sleep -Seconds 5

# 3. Start Core Microservices
$services = @(
    "AuthService",
    "ApplicationService",
    "VerifyService",
    "RiskService",
    "PolicyService",
    "PaymentService",
    "ClaimService"
)

$count = 3
foreach ($svc in $services) {
    Write-Host "[$count/9] Starting $svc..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$svc'; .\mvnw.cmd spring-boot:run"
    Start-Sleep -Seconds 3
    $count++
}

Write-Host "`nAll backend services launched!" -ForegroundColor Green
Write-Host "Eureka Dashboard : http://localhost:8761" -ForegroundColor Cyan
Write-Host "API Gateway      : http://localhost:8080" -ForegroundColor Cyan

