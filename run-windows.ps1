# Скрипт запуска FinTech Analytics на Windows

Write-Host "=== FinTech Analytics - Windows Edition ===" -ForegroundColor Green
Write-Host ""

# Проверяем Docker
Write-Host "Проверяем Docker Desktop..." -ForegroundColor Yellow
if (-not (docker info 2>$null)) {
    Write-Host "Ошибка: Docker Desktop не запущен!" -ForegroundColor Red
    Write-Host "Запустите Docker Desktop и включите Kubernetes" -ForegroundColor Yellow
    exit 1
}

# Проверяем Kubernetes
Write-Host "Проверяем Kubernetes..." -ForegroundColor Yellow
kubectl cluster-info
if ($LASTEXITCODE -ne 0) {
    Write-Host "Ошибка: Kubernetes не доступен!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Выберите действие:" -ForegroundColor Cyan
Write-Host "1. Полная установка (рекомендуется для первого запуска)" -ForegroundColor White
Write-Host "2. Запуск только приложения" -ForegroundColor White
Write-Host "3. Остановка всего" -ForegroundColor White
Write-Host "4. Просмотр логов" -ForegroundColor White
Write-Host "5. Открыть UI Dashboard" -ForegroundColor White
Write-Host ""
$choice = Read-Host "Введите номер [1-5]"

switch ($choice) {
    "1" {
        # Полная установка
        Write-Host "Выполняем полную установку..." -ForegroundColor Green

        # 1. Устанавливаем базовые компоненты
        Write-Host "1. Устанавливаем базовые компоненты..." -ForegroundColor Yellow
        .\scripts\setup-windows.ps1

        # 2. Собираем Docker образ
        Write-Host "2. Собираем Docker образ..." -ForegroundColor Yellow
        docker build -t fintech/analytics-service:windows-latest .

        # 3. Устанавливаем Helm чарт
        Write-Host "3. Устанавливаем Helm чарт..." -ForegroundColor Yellow
        cd helm
        helm dependency update
        helm install analytics . `
            -n fintech `
            -f values-windows.yaml `
            --create-namespace `
            --wait

        # 4. Устанавливаем UI Dashboard
        Write-Host "4. Устанавливаем UI Dashboard..." -ForegroundColor Yellow
        .\scripts\install-dashboard.ps1

        # 5. Настраиваем port-forward
        Write-Host "5. Настраиваем доступ к сервисам..." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Для доступа к сервисам откройте новые окна PowerShell и выполните:" -ForegroundColor Green
        Write-Host ""
        Write-Host "Окно 1 - Приложение:" -ForegroundColor Cyan
        Write-Host "kubectl port-forward -n fintech svc/analytics-analytics-service 8080:8080" -ForegroundColor White
        Write-Host ""
        Write-Host "Окно 2 - Grafana:" -ForegroundColor Cyan
        Write-Host "kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80" -ForegroundColor White
        Write-Host ""
        Write-Host "Окно 3 - Prometheus:" -ForegroundColor Cyan
        Write-Host "kubectl port-forward -n monitoring svc/monitoring-prometheus 9090:9090" -ForegroundColor White
        Write-Host ""
        Write-Host "Или используйте Lens IDE для удобного доступа" -ForegroundColor Yellow
    }

    "2" {
        # Только приложение
        Write-Host "Запускаем только приложение..." -ForegroundColor Green

        # Собираем образ
        docker build -t fintech/analytics-service:windows-latest .

        # Устанавливаем Helm
        cd helm
        helm upgrade --install analytics . `
            -n fintech `
            -f values-windows.yaml `
            --wait

        Write-Host "Приложение запущено!" -ForegroundColor Green
        Write-Host "Для доступа выполните:" -ForegroundColor Yellow
        Write-Host "kubectl port-forward -n fintech svc/analytics-analytics-service 8080:8080" -ForegroundColor White
        Write-Host "И откройте: http://localhost:8080" -ForegroundColor White
    }

    "3" {
        # Остановка
        Write-Host "Останавливаем все..." -ForegroundColor Yellow
        helm uninstall analytics -n fintech
        helm uninstall monitoring -n monitoring
        kubectl delete namespace fintech monitoring --ignore-not-found=true

        Write-Host "Все остановлено!" -ForegroundColor Green
    }

    "4" {
        # Логи
        Write-Host "Просмотр логов..." -ForegroundColor Yellow

        Write-Host "Выберите сервис:" -ForegroundColor Cyan
        Write-Host "1. Основное приложение" -ForegroundColor White
        Write-Host "2. База данных" -ForegroundColor White
        Write-Host "3. Ingress контроллер" -ForegroundColor White
        Write-Host ""
        $logChoice = Read-Host "Введите номер [1-3]"

        switch ($logChoice) {
            "1" {
                kubectl logs -n fintech -l app.kubernetes.io/name=fintech-analytics --tail=50 -f
            }
            "2" {
                kubectl logs -n fintech -l app=postgresql --tail=50 -f
            }
            "3" {
                kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx --tail=50 -f
            }
        }
    }

    "5" {
        # UI Dashboard
        Write-Host "Открываем UI Dashboard..." -ForegroundColor Yellow

        # Запускаем Dashboard
        Start-Process "http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/"

        # Запускаем kubectl proxy в фоне
        Start-Job -ScriptBlock {
            kubectl proxy
        }

        Write-Host "Dashboard открывается в браузере..." -ForegroundColor Green
        Write-Host "Для остановки нажмите Ctrl+C" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== Готово! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Рекомендуемые инструменты для работы:" -ForegroundColor Cyan
Write-Host "1. Lens IDE: https://k8slens.dev/ (лучший UI для Kubernetes)" -ForegroundColor White
Write-Host "2. K9s: choco install k9s (терминальный UI)" -ForegroundColor White
Write-Host "3. Octant: choco install octant (альтернативный UI)" -ForegroundColor White
Write-Host ""
Write-Host "Доступные URL:" -ForegroundColor Cyan
Write-Host "- Приложение: http://localhost:8080" -ForegroundColor White
Write-Host "- Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "- Actuator: http://localhost:8080/actuator" -ForegroundColor White
Write-Host "- Grafana: http://localhost:3000 (admin/admin123)" -ForegroundColor White
Write-Host "- Prometheus: http://localhost:9090" -ForegroundColor White