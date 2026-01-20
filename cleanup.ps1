# cleanup.ps1 - Полная очистка

Write-Host "=== Cleaning up FinTech Analytics ===" -ForegroundColor Yellow

# Удаление ресурсов
kubectl delete namespace fintech --ignore-not-found=true
kubectl delete servicemonitor -n fintech analytics-service-monitor --ignore-not-found=true
kubectl delete podmonitor -n fintech analytics-pod-monitor --ignore-not-found=true

# Удаление Docker образа
docker rmi fintech-analytics:latest -f 2>$null

Write-Host "=== Cleanup complete! ===" -ForegroundColor Green