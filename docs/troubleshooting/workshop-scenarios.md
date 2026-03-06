# Troubleshooting workshop: producer-side incident simulation

Этот документ помогает провести практику для стажёров по системной диагностике инцидентов в связке из двух микросервисов (producer + consumer).

## Цель тренировки

Стажёр должен пройти одинаковый алгоритм диагностики в двух кейсах:
1. **"Сервис медленно отвечает"**
2. **"Kafka Consumer lag растёт"**

Во всех кейсах работаем по цепочке: **логи → метрики (Prometheus/Grafana) → трассировка (Jaeger) → гипотеза → проверка → фикc**.

---

## Подготовка стенда

1. Поднять producer-сервис и мониторинг.
2. Убедиться, что `/actuator/prometheus` доступен.
3. Для кейса с lag нужен второй сервис-consumer (ваш отдельный микросервис), подписанный на topic `analytics-events`.

---

## Кейс 1: «Сервис медленно отвечает»

### Как симулировать

Запустите нагрузку на "тяжёлый" endpoint:

```bash
BASE_URL=http://localhost:8080 REQUESTS=800 CONCURRENCY=80 MODE=naive ./scripts/load-demo.sh
```

Дополнительно создайте фоновую нагрузку на базу (N+1/сложные выборки):

```bash
./scripts/troubleshooting/simulate-slow-response.sh
```

### Что дать стажёру

- Логи producer за последние 15 минут.
- Графики в Grafana:
  - p95/p99 latency
  - `http.server.requests` (rate + max)
  - CPU, GC pause, DB pool active connections
- Трейсы Jaeger по endpoint `/api/v1/analytics/clients/naive`

### Ожидаемый план диагностики от стажёра

1. Подтвердить деградацию по SLI/SLO (p95, error rate).
2. Сопоставить временной диапазон с ростом RPS.
3. Проверить в логах долгие SQL/повторяющиеся запросы.
4. Проверить трейс: где основное время (Controller/Service/Repository/DB).
5. Сформулировать гипотезу (N+1, отсутствие индекса, маленький пул, GC pressure).
6. Предложить фикс и верификацию (сравнение до/после).

### Возможные решения

- Перевести потребителей на оптимизированный endpoint `/optimized`.
- Добавить/проверить индексы на часто фильтруемых полях.
- Оптимизировать JPA-запросы (`EntityGraph`, DTO projection, batch fetch).
- Перенастроить Hikari pool и лимиты БД.

---

## Кейс 2: «Kafka Consumer lag растёт»

### Как симулировать

С producer-сервиса быстро публикуем события создания клиентов (бёрст в topic `analytics-events`):

```bash
BASE_URL=http://localhost:8080 TOTAL=3000 PARALLEL=60 ./scripts/troubleshooting/simulate-kafka-burst.sh
```

На стороне consumer можно искусственно замедлить обработку (например, sleep в listener) — lag вырастет ещё быстрее.

### Что дать стажёру

- Логи producer + consumer.
- Метрики Kafka:
  - consumer lag (по group/topic/partition)
  - producer send rate
  - consumer records consumed rate
  - rebalance count
- Трейсы (если есть trace propagation между сервисами).

### Ожидаемый план диагностики от стажёра

1. Подтвердить, что lag действительно растёт и где именно (group/topic/partition).
2. Сравнить **produce rate** vs **consume rate**.
3. Исключить инфраструктурные проблемы (broker down, сетевые ошибки, rebalance storm).
4. Проверить логи consumer на ошибки десериализации/ретраи/таймауты.
5. Проверить downstream-зависимости consumer (БД/HTTP), которые тормозят обработку.
6. Подготовить mitigation + root cause fix.

### Возможные решения

- Увеличить число consumer instances/партиций.
- Снизить batch-processing latency в consumer.
- Перенастроить `max.poll.records`, `fetch.min.bytes`, `max.poll.interval.ms`.
- Ввести backpressure/throttling на producer в пиковые окна.

---

## Шаблон отчёта стажёра

1. **Симптом:**
2. **Импакт:**
3. **Наблюдения из логов:**
4. **Наблюдения из метрик:**
5. **Наблюдения из трейсов:**
6. **Гипотеза причины:**
7. **План проверки гипотезы:**
8. **Временный mitigation:**
9. **Постоянное исправление:**
10. **Как убедились, что проблема решена:**

---

## Чеклист преподавателя

- Стажёр использует системный порядок (не прыгает сразу к «фиксу»).
- Любая гипотеза подтверждается данными (log/metric/trace).
- Стажёр различает mitigation и root cause fix.
- Есть критерий «инцидент закрыт» (метрики стабилизировались, lag убывает, SLO восстановлен).
