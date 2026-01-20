Write-Host "=== Full Deployment with Database & Monitoring ===" -ForegroundColor Green

# 1. Очистка
Write-Host "1. Cleaning previous installations..." -ForegroundColor Yellow
helm uninstall analytics -n fintech 2>$null
helm uninstall prometheus -n monitoring 2>$null
helm uninstall grafana -n monitoring 2>$null
kubectl delete namespace fintech monitoring --ignore-not-found=true 2>$null
Start-Sleep -Seconds 3

# 2. Создание namespaces
Write-Host "2. Creating namespaces..." -ForegroundColor Yellow
kubectl create namespace fintech
kubectl create namespace monitoring
kubectl label namespace fintech monitoring=true

# 3. Установка PostgreSQL БД
Write-Host "3. Installing PostgreSQL database..." -ForegroundColor Yellow
kubectl apply -n fintech -f .\helm\templates\postgres.yaml
Write-Host "   Waiting for PostgreSQL to start (30 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

# Проверяем PostgreSQL
Write-Host "   Checking PostgreSQL status..." -ForegroundColor Gray
kubectl get pods -n fintech -l app=postgres
kubectl logs -n fintech -l app=postgres --tail=3

# 4. Установка kube-prometheus-stack (всё в одном)
Write-Host "4. Installing kube-prometheus-stack (Prometheus + Grafana)..." -ForegroundColor Yellow

# Проверяем, не установлен ли уже
$monitoringStatus = helm list -n monitoring 2>$null | Select-String "monitoring"
if (-not $monitoringStatus) {
    helm install monitoring prometheus-community/kube-prometheus-stack `
        -n monitoring `
        --create-namespace `
        --set grafana.adminPassword=admin123 `
        --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false
} else {
    Write-Host "   Monitoring stack already installed, skipping..." -ForegroundColor Gray
}

Write-Host "   Waiting for monitoring stack (30 seconds)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

# 5. Сборка Docker образа
Write-Host "5. Building Docker image..." -ForegroundColor Yellow
docker build -t fintech/analytics-service:latest .

# 6. Деплой приложения через Helm
Write-Host "6. Deploying application with Helm..." -ForegroundColor Yellow
helm upgrade --install analytics ./helm `
    -n fintech `
    --create-namespace `
    --set app.image.repository=fintech/analytics-service `
    --set app.image.tag=latest `
    --set database.enabled=true `
    --set monitoring.enabled=true `
    --wait `
    --timeout 5m

# 7. Проверка деплоя
Write-Host "7. Verifying deployment..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

Write-Host "`n=== Application Status ===" -ForegroundColor Cyan
kubectl get pods,svc -n fintech -o wide

Write-Host "`n=== Database Status ===" -ForegroundColor Cyan
kubectl get pods,svc -n fintech -l app=postgres -o wide

Write-Host "`n=== Monitoring Status ===" -ForegroundColor Cyan
kubectl get pods,svc -n monitoring -o wide

Write-Host "`n=== Application Logs ===" -ForegroundColor Cyan
kubectl logs -n fintech -l app=fintech-analytics --tail=10 2>$null

Write-Host "`n=== Database Logs ===" -ForegroundColor Cyan
kubectl logs -n fintech -l app=postgres --tail=5 2>$null

Write-Host "`n=== Access Instructions ===" -ForegroundColor Green

Write-Host "1. Application:" -ForegroundColor White
Write-Host "   kubectl port-forward -n fintech svc/fintech-analytics 8080:8080" -ForegroundColor Gray
Write-Host "   http://localhost:8080/actuator/health" -ForegroundColor Gray
Write-Host "   http://localhost:8080/actuator/prometheus" -ForegroundColor Gray

Write-Host "`n2. Database (PostgreSQL):" -ForegroundColor White
Write-Host "   kubectl port-forward -n fintech svc/postgres 5432:5432" -ForegroundColor Gray
Write-Host "   Connection: host=localhost, port=5432, database=fintech_analytics" -ForegroundColor Gray
Write-Host "   Username: postgres, Password: postgres123" -ForegroundColor Gray

Write-Host "`n3. Prometheus (metrics):" -ForegroundColor White
Write-Host "   kubectl port-forward -n monitoring svc/prometheus-server 9090:80" -ForegroundColor Gray
Write-Host "   http://localhost:9090" -ForegroundColor Gray
Write-Host "   Check targets: http://localhost:9090/targets" -ForegroundColor Gray

Write-Host "`n4. Grafana (dashboards):" -ForegroundColor White
Write-Host "   kubectl port-forward -n monitoring svc/grafana 3000:80" -ForegroundColor Gray
Write-Host "   http://localhost:3000" -ForegroundColor Gray
Write-Host "   Username: admin" -ForegroundColor Gray
Write-Host "   Password: admin123" -ForegroundColor Gray

Write-Host "`n=== Useful Commands ===" -ForegroundColor Cyan
Write-Host "   View app logs: kubectl logs -n fintech -l app=fintech-analytics -f" -ForegroundColor Gray
Write-Host "   View DB logs: kubectl logs -n fintech -l app=postgres -f" -ForegroundColor Gray
Write-Host "   Restart app: kubectl rollout restart deployment/fintech-analytics -n fintech" -ForegroundColor Gray
Write-Host "   Check DB connection: kubectl exec -n fintech -it deployment/postgres -- psql -U postgres -d fintech_analytics" -ForegroundColor Gray

Write-Host "`n=== Troubleshooting ===" -ForegroundColor Yellow
Write-Host "   If application fails to connect to DB, check:" -ForegroundColor Gray
Write-Host "   1. kubectl get pods -n fintech (both app and postgres should be Running)" -ForegroundColor Gray
Write-Host "   2. kubectl logs -n fintech -l app=postgres (check DB logs)" -ForegroundColor Gray
Write-Host "   3. Test DB: kubectl exec -n fintech deployment/postgres -- pg_isready -U postgres" -ForegroundColor Gray

Write-Host "`n=== Deployment Complete! ===" -ForegroundColor Green