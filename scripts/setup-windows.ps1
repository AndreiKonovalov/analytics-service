# Скрипт установки полного стека для Windows

Write-Host "=== Установка FinTech Analytics Stack ===" -ForegroundColor Green

# Проверка Docker Desktop
Write-Host "1. Проверяем Docker Desktop..." -ForegroundColor Yellow
$dockerRunning = docker info 2>$null
if (-not $dockerRunning) {
    Write-Host "Ошибка: Docker Desktop не запущен!" -ForegroundColor Red
    Write-Host "Запустите Docker Desktop и включите Kubernetes" -ForegroundColor Yellow
    exit 1
}

# Проверка Kubernetes
Write-Host "2. Проверяем Kubernetes..." -ForegroundColor Yellow
kubectl cluster-info
if ($LASTEXITCODE -ne 0) {
    Write-Host "Ошибка: Kubernetes не доступен!" -ForegroundColor Red
    exit 1
}

# Создаем namespace для нашего проекта
Write-Host "3. Создаем namespace 'fintech'..." -ForegroundColor Yellow
kubectl create namespace fintech --dry-run=client -o yaml | kubectl apply -f -

# Устанавливаем Ingress NGINX
Write-Host "4. Устанавливаем Ingress NGINX..." -ForegroundColor Yellow
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
Start-Sleep -Seconds 10

# Устанавливаем Metrics Server
Write-Host "5. Устанавливаем Metrics Server..." -ForegroundColor Yellow
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Патчим Metrics Server для работы в Docker Desktop
$patch = @"
spec:
  template:
    spec:
      containers:
      - name: metrics-server
        args:
        - --cert-dir=/tmp
        - --secure-port=4443
        - --kubelet-insecure-tls
        - --kubelet-preferred-address-types=InternalIP,ExternalIP,Hostname
        - --kubelet-use-node-status-port
"@

$patch | kubectl patch deployment metrics-server -n kube-system --patch-file /dev/stdin

# Устанавливаем Helm (если не установлен)
Write-Host "6. Проверяем установку Helm..." -ForegroundColor Yellow
if (-not (Get-Command helm -ErrorAction SilentlyContinue)) {
    Write-Host "Устанавливаем Helm..." -ForegroundColor Cyan
    choco install kubernetes-helm
}

# Добавляем репозитории Helm
Write-Host "7. Добавляем Helm репозитории..." -ForegroundColor Yellow
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add jetstack https://charts.jetstack.io
helm repo update

# Устанавливаем Cert Manager для TLS
Write-Host "8. Устанавливаем Cert Manager..." -ForegroundColor Yellow
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.2/cert-manager.yaml
Start-Sleep -Seconds 30

# Создаем ClusterIssuer для локального использования
Write-Host "9. Создаем тестовый ClusterIssuer..." -ForegroundColor Yellow
$clusterIssuer = @"
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: selfsigned-issuer
spec:
  selfSigned: {}
"@

$clusterIssuer | kubectl apply -f -

# Устанавливаем Grafana и Prometheus
Write-Host "10. Устанавливаем мониторинг (Grafana + Prometheus)..." -ForegroundColor Yellow

# Создаем values файл для Prometheus Stack
$prometheusValues = @"
grafana:
  enabled: true
  adminPassword: admin123
  persistence:
    enabled: true
    size: 2Gi
  ingress:
    enabled: true
    hosts:
      - grafana.local
    annotations:
      cert-manager.io/cluster-issuer: selfsigned-issuer
    tls:
      - hosts:
          - grafana.local
        secretName: grafana-tls

prometheus:
  enabled: true
  prometheusSpec:
    retention: 7d
    storageSpec:
      volumeClaimTemplate:
        spec:
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 5Gi

alertmanager:
  enabled: false

prometheus-node-exporter:
  enabled: false
"@

$prometheusValues | Out-File -FilePath "prometheus-values.yaml" -Encoding UTF8

helm install monitoring prometheus-community/kube-prometheus-stack `
    -n monitoring `
    --create-namespace `
    -f prometheus-values.yaml `
    --wait

# Создаем секреты для нашего приложения
Write-Host "11. Создаем секреты для приложения..." -ForegroundColor Yellow
$secrets = @"
apiVersion: v1
kind: Secret
metadata:
  name: analytics-secrets
  namespace: fintech
type: Opaque
stringData:
  SPRING_DATASOURCE_USERNAME: postgres
  SPRING_DATASOURCE_PASSWORD: postgres123
  JAVA_OPTS: -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
"@

$secrets | kubectl apply -f -

# Настраиваем hosts файл для локальных доменов
Write-Host "12. Настраиваем hosts файл..." -ForegroundColor Yellow
$hostsEntry = "127.0.0.1 analytics.local grafana.local"
$hostsPath = "$env:SystemRoot\System32\drivers\etc\hosts"

# Делаем резервную копию
Copy-Item $hostsPath "$hostsPath.backup" -Force

# Добавляем записи
Add-Content $hostsPath "`n$hostsEntry" -Encoding ASCII

Write-Host ""
Write-Host "=== Установка завершена! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Доступные сервисы:" -ForegroundColor Cyan
Write-Host "1. Kubernetes Dashboard:" -ForegroundColor Yellow
Write-Host "   Запустите: .\scripts\install-dashboard.ps1" -ForegroundColor White
Write-Host ""
Write-Host "2. Наше приложение будет доступно по:" -ForegroundColor Yellow
Write-Host "   http://analytics.local" -ForegroundColor White
Write-Host ""
Write-Host "3. Grafana Dashboard:" -ForegroundColor Yellow
Write-Host "   http://grafana.local" -ForegroundColor White
Write-Host "   Логин: admin" -ForegroundColor White
Write-Host "   Пароль: admin123" -ForegroundColor White
Write-Host ""
Write-Host "4. Для доступа к сервисам используйте port-forward:" -ForegroundColor Yellow
Write-Host "   kubectl port-forward -n fintech service/analytics-service 8080:8080" -ForegroundColor White
Write-Host ""
Write-Host "5. Рекомендуемые UI инструменты:" -ForegroundColor Cyan
Write-Host "   - Lens IDE: https://k8slens.dev/" -ForegroundColor White
Write-Host "   - K9s (терминальный): choco install k9s" -ForegroundColor White