# Скрипт установки Kubernetes Dashboard для Windows

Write-Host "=== Установка Kubernetes Dashboard ===" -ForegroundColor Green

# 1. Установка Dashboard
Write-Host "1. Устанавливаем Kubernetes Dashboard..." -ForegroundColor Yellow
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

# 2. Создаем сервисный аккаунт
Write-Host "2. Создаем сервисный аккаунт для Dashboard..." -ForegroundColor Yellow

$dashboardServiceAccount = @"
apiVersion: v1
kind: ServiceAccount
metadata:
  name: admin-user
  namespace: kubernetes-dashboard
"@

$dashboardServiceAccount | kubectl apply -f -

# 3. Создаем ClusterRoleBinding
Write-Host "3. Создаем ClusterRoleBinding..." -ForegroundColor Yellow

$clusterRoleBinding = @"
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: admin-user
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: admin-user
  namespace: kubernetes-dashboard
"@

$clusterRoleBinding | kubectl apply -f -

# 4. Получаем токен для доступа
Write-Host "4. Получаем токен доступа..." -ForegroundColor Yellow
$token = kubectl -n kubernetes-dashboard create token admin-user --duration=8760h
Write-Host "Токен: $token" -ForegroundColor Cyan

# 5. Запускаем прокси для доступа к Dashboard
Write-Host "5. Запускаем kubectl proxy..." -ForegroundColor Yellow
Write-Host "Dashboard будет доступен по адресу: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/" -ForegroundColor Green
Write-Host ""
Write-Host "Используйте этот токен для входа:" -ForegroundColor Green
Write-Host "$token" -ForegroundColor Cyan
Write-Host ""
Write-Host "Запустите в отдельном терминале:" -ForegroundColor Yellow
Write-Host "kubectl proxy" -ForegroundColor White
Write-Host ""
Write-Host "Или используйте Port-forward:" -ForegroundColor Yellow
Write-Host "kubectl port-forward -n kubernetes-dashboard service/kubernetes-dashboard 8443:443" -ForegroundColor White