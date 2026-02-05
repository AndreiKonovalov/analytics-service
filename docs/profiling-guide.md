# Профилирование и поиск слабых мест

Ниже — готовый сценарий, как создать нагрузку на сервис, увидеть узкие места и зафиксировать их профайлером.

## 1. Подготовка данных

При старте приложения автоматически срабатывает `DataSeeder` и наполняет базу тестовыми данными. Этого достаточно для демонстрации N+1 на аналитических эндпоинтах.

## 2. Запуск сервиса

```bash
# Поднимаем Postgres (локально)
docker-compose up -d postgres

# Запускаем сервис
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 3. Генерация нагрузки

В репозитории есть скрипт для нагрузки (по умолчанию бьёт в «наивный» эндпоинт с N+1):

```bash
./scripts/load-demo.sh
```

Параметры можно менять через переменные окружения:

```bash
BASE_URL=http://localhost:8080 \
REQUESTS=400 \
CONCURRENCY=40 \
MODE=compare \
./scripts/load-demo.sh
```

Режимы нагрузки:
- `MODE=naive` — только `/api/v1/analytics/clients/naive`
- `MODE=optimized` — только `/api/v1/analytics/clients/optimized`
- `MODE=compare` — оба эндпоинта последовательно

## 4. Профилирование через Java Flight Recorder (JFR)

JFR уже встроен в JDK. Запись можно запустить прямо во время нагрузки.

1) Найдите PID процесса:

```bash
jcmd | rg analytics-service
# или
jps -l
```

2) Запустите запись (пример на 60 секунд):

```bash
jcmd <PID> JFR.start name=profiling settings=profile duration=60s filename=./build/profile.jfr
```

3) Откройте `./build/profile.jfr` в **JDK Mission Control** и посмотрите:
- Hot Methods → `TransactionReportService#getClientsWithTransactionsNaive`
- Время на ORM/SQL → пакеты `org.hibernate`/`org.springframework.data`

Это покажет основную «боль» N+1 и задержки в ленивых загрузках.

## 5. Профилирование через async-profiler (CPU/alloc)

Если нужно увидеть flamegraph, используйте async-profiler:

```bash
# CPU-профиль на 60 секунд
./profiler.sh -d 60 -e cpu -f ./build/profile-cpu.html <PID>

# Аллокации
./profiler.sh -d 60 -e alloc -f ./build/profile-alloc.html <PID>
```

Открывайте HTML и ищите «горячие» пути вызовов и массовые аллокации в ORM/мапперах.

## Что искать в профайлере

- Пиковые методы в `TransactionReportService#getClientsWithTransactionsNaive`
- Большое число вызовов Hibernate (lazy-load), что указывает на N+1
- Увеличенные задержки на построение DTO (маппинг/агрегации)

## Как сравнить с оптимизацией

Для контраста запустите нагрузку на оптимизированный эндпоинт:

```bash
MODE=optimized ./scripts/load-demo.sh
```

Разница в времени и профиле (меньше SQL и меньше времени в Hibernate) хорошо видна в JFR и flamegraph.
