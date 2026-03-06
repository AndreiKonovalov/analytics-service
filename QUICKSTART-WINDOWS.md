# Быстрый старт на Windows

## Требования
1. Windows 10/11 Pro, Enterprise или Education (для WSL2)
2. 8+ GB RAM
3. 20+ GB свободного места

## Установка

### 1. Установите Docker Desktop
- Скачайте: https://www.docker.com/products/docker-desktop/
- Установите с настройками по умолчанию
- Включите WSL2 (рекомендуется)

### 2. Включите Kubernetes
- Откройте Docker Desktop
- Settings → Kubernetes
- Включите "Enable Kubernetes"
- Нажмите "Apply & Restart"

### 3. Установите необходимые инструменты
Откройте PowerShell от имени администратора:

```powershell
# Установка Chocolatey (пакетный менеджер)
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Установка инструментов
choco install kubernetes-cli kubernetes-helm k9s