# Кэширование и Redis: демонстрация для стажёров

Эта инструкция показывает, как включить Redis-кэш и наглядно продемонстрировать эффект кэширования на аналитических эндпоинтах.

## 1. Запуск инфраструктуры

```bash
# PostgreSQL + Redis + сервис
Docker-compose up -d
```

По умолчанию в docker-профиле включён Redis-кэш (`SPRING_PROFILES_ACTIVE=docker,redis`).

## 2. Наблюдение за кэшем

Откройте отдельный терминал и подключитесь к Redis:

```bash
redis-cli -h localhost -p 6379
```

Внутри Redis можно смотреть ключи:

```
KEYS analytics.*
TTL analytics.clients.summary
```

## 3. Демонстрация кэширования

### Шаг 1: Прогреть кэш

```bash
curl -s http://localhost:8080/api/v1/analytics/clients/summary > /dev/null
```

### Шаг 2: Повторить запрос

```bash
curl -s http://localhost:8080/api/v1/analytics/clients/summary > /dev/null
```

**Что увидят стажёры:**
- В логах приложения сообщение `Cache miss for client summary projection` появится только на первом запросе.
- В Redis появится ключ вида `analytics.clients.summary::SimpleKey []`.

### Шаг 3: Очистить кэш и повторить

```bash
curl -X POST http://localhost:8080/api/v1/analytics/cache/evict
curl -s http://localhost:8080/api/v1/analytics/clients/summary > /dev/null
```

### Дополнительно: сравнить с пагинацией

```bash
curl -s "http://localhost:8080/api/v1/analytics/clients/optimized?page=0&size=20" > /dev/null
curl -s "http://localhost:8080/api/v1/analytics/clients/optimized?page=0&size=20" > /dev/null
```

Для каждой комбинации `page/size/sort` создаётся отдельный ключ кэша.

## 4. Что объяснить стажёрам

- **Когда стоит кэшировать**: редкие изменения данных + тяжёлые расчёты.
- **Ключи кэша**: важно включать параметры (страница, сортировка, фильтры).
- **TTL**: чтобы данные не устаревали навсегда (в проекте по умолчанию 30 секунд).
- **Инвалидация**: после записи/обновления данных кэш нужно чистить (демо-эндпоинт делает это вручную).

## 5. Если Redis недоступен

Локально по умолчанию работает Caffeine-кэш (in-memory) из `application.yml`. Для Redis используйте профиль `redis`:

```bash
./gradlew bootRun --args='--spring.profiles.active=local,redis'
```