Write-Host "=== Simple Deployment ===" -ForegroundColor Green

# 1. Очистка
Write-Host "1. Cleaning..." -ForegroundColor Yellow
helm uninstall analytics -n fintech 2>$null
kubectl delete namespace fintech --ignore-not-found=true 2>$null
Start-Sleep -Seconds 2

# 2. Создание namespace
Write-Host "2. Creating namespace..." -ForegroundColor Yellow
kubectl create namespace fintech 2>$null

# 3. Сборка образа
Write-Host "3. Building Docker image..." -ForegroundColor Yellow
docker build -t fintech/analytics-service:latest .

# 4. Деплой приложения
Write-Host "4. Deploying application..." -ForegroundColor Yellow
helm upgrade --install analytics ./helm `
    -n fintech `
    --create-namespace `
    --set app.image.repository=fintech/analytics-service `
    --set app.image.tag=latest `
    --set database.enabled=true `
    --wait

# 5. Проверка
Write-Host "5. Verifying deployment..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

Write-Host "`n=== Status ===" -ForegroundColor Cyan
kubectl get pods,svc -n fintech

Write-Host "`n=== Logs ===" -ForegroundColor Cyan
kubectl logs -n fintech -l app=fintech-analytics --tail=5 2>$null

Write-Host "`n=== Access ===" -ForegroundColor Green
Write-Host "To access application:" -ForegroundColor White
Write-Host "  kubectl port-forward -n fintech svc/fintech-analytics 8080:8080" -ForegroundColor Gray
Write-Host "  Then open: http://localhost:8080/actuator/health" -ForegroundColor Gray

Write-Host "`n=== Deployment Complete! ===" -ForegroundColor Green