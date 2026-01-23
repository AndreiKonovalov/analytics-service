Write-Host "=== Full Deployment with Helm ===" -ForegroundColor Green

# 1. Очистка
Write-Host "1. Cleaning previous installations..." -ForegroundColor Yellow
helm uninstall analytics -n fintech 2>$null
kubectl delete namespace fintech monitoring --ignore-not-found=true 2>$null
Start-Sleep -Seconds 3

# 2. Создание namespaces
Write-Host "2. Creating namespaces..." -ForegroundColor Yellow
kubectl create namespace fintech
kubectl create namespace monitoring

# 3. Установка мониторинга БЕЗ node-exporter и БЕЗ ожидания
Write-Host "3. Installing monitoring stack..." -ForegroundColor Yellow
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Установка с отключением проблемных компонентов
helm upgrade --install monitoring prometheus-community/kube-prometheus-stack `
    -n monitoring `
    --set grafana.adminPassword=admin123 `
    --set grafana.sidecar.dashboards.enabled=true `
    --set grafana.sidecar.dashboards.label=grafana_dashboard `
    --set grafana.sidecar.dashboards.folder=/var/lib/grafana/dashboards/default `
    --set grafana.sidecar.datasources.enabled=true `
    --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false `
    --set nodeExporter.enabled=false `
    --set kube-state-metrics.enabled=false

# Дадим время на установку, но не ждём завершения всех компонентов
Write-Host "   Waiting 25 seconds for monitoring components..." -ForegroundColor Gray
Start-Sleep -Seconds 25

# Проверяем что установилось
Write-Host "   Checking monitoring status..." -ForegroundColor Gray
kubectl get pods -n monitoring

# 4. Сборка Docker образа
Write-Host "4. Building Docker image..." -ForegroundColor Yellow
docker build -t fintech/analytics-service:latest .

# 5. Установка приложения с Helm
Write-Host "5. Deploying application with Helm..." -ForegroundColor Yellow
helm upgrade --install analytics ./helm `
    -n fintech `
    --set app.image.repository=fintech/analytics-service `
    --set app.image.tag=latest `
    --set postgresql.enabled=true `
    --set monitoring.enabled=true `
    --wait `
    --timeout 2m

# 6. Проверка с правильными именами сервисов
Write-Host "6. Verifying deployment..." -ForegroundColor Yellow
Start-Sleep -Seconds 20

Write-Host "`n=== Application Status ===" -ForegroundColor Cyan
kubectl get pods,svc -n fintech -o wide

Write-Host "`n=== Monitoring Status ===" -ForegroundColor Cyan
kubectl get pods,svc -n monitoring -o wide

Write-Host "`n=== Service Names ===" -ForegroundColor Cyan
Write-Host "In fintech namespace:" -ForegroundColor Yellow
kubectl get svc -n fintech

Write-Host "`nIn monitoring namespace:" -ForegroundColor Yellow
kubectl get svc -n monitoring

Write-Host "`n=== Access Instructions ===" -ForegroundColor Green
Write-Host "Application (Spring Boot):" -ForegroundColor White
Write-Host "   kubectl port-forward -n fintech svc/analytics-analytics 8080:8080" -ForegroundColor Gray
Write-Host "   http://localhost:8080/actuator/health" -ForegroundColor Gray

Write-Host "`nDatabase (PostgreSQL):" -ForegroundColor White
Write-Host "   kubectl port-forward -n fintech svc/analytics-postgres 5432:5432" -ForegroundColor Gray
Write-Host "   Connection: host=localhost, port=5432, database=fintech_analytics" -ForegroundColor Gray

Write-Host "`nGrafana (if monitoring installed):" -ForegroundColor White
Write-Host "   kubectl get svc -n monitoring | findstr grafana" -ForegroundColor Gray
$grafanaSvc = kubectl get svc -n monitoring -o=jsonpath='{.items[?(@.metadata.name=~".*grafana.*")].metadata.name}' 2>$null
if ($grafanaSvc) {
    Write-Host "   kubectl port-forward -n monitoring svc/$grafanaSvc 3000:80" -ForegroundColor Gray
    Write-Host "   http://localhost:3000 (admin/admin123)" -ForegroundColor Gray
} else {
    Write-Host "   Grafana service not found. Monitoring installation may have failed." -ForegroundColor Red
}

Write-Host "`nPrometheus (if monitoring installed):" -ForegroundColor White
$prometheusSvc = kubectl get svc -n monitoring -o=jsonpath='{.items[?(@.metadata.name=~".*prometheus.*")].metadata.name}' 2>$null
if ($prometheusSvc) {
    Write-Host "   kubectl port-forward -n monitoring svc/$prometheusSvc 9090:9090" -ForegroundColor Gray
    Write-Host "   http://localhost:9090" -ForegroundColor Gray
} else {
    Write-Host "   Prometheus service not found." -ForegroundColor Red
}

Write-Host "`n=== Troubleshooting ===" -ForegroundColor Yellow
Write-Host "If monitoring failed to install, you can:" -ForegroundColor Gray
Write-Host "1. Install simpler monitoring stack:" -ForegroundColor Gray
Write-Host "   helm uninstall monitoring -n monitoring" -ForegroundColor Gray
Write-Host "   helm install prometheus prometheus-community/prometheus -n monitoring --set server.service.type=ClusterIP" -ForegroundColor Gray
Write-Host "   helm install grafana grafana/grafana -n monitoring --set adminPassword=admin123 --set service.type=ClusterIP" -ForegroundColor Gray

Write-Host "`n2. Check application logs:" -ForegroundColor Gray
Write-Host "   kubectl logs -n fintech deployment/analytics-analytics" -ForegroundColor Gray

Write-Host "`n3. Check database connection:" -ForegroundColor Gray
Write-Host "   kubectl exec -n fintech deployment/analytics-postgres -- pg_isready -U postgres" -ForegroundColor Gray

Write-Host "`n=== Deployment Complete! ===" -ForegroundColor Green