Отлично! Вот подробный README.md для проекта:

# FinTech Analytics Service

Микросервис для аналитики финансовых транзакций с акцентом на оптимизацию работы с БД в Java.

## 🎯 Цель проекта

Демонстрация и изучение проблем производительности при работе с ORM (Hibernate) и способов их решения в контексте финтех-приложения.

## 🚀 Технологический стек

- **Java 21** (Records, Pattern Matching)
- **Spring Boot 3.5.8**
- **PostgreSQL 16+** (основная БД)
- **H2 Database** (для тестов)
- **Gradle** (Kotlin DSL)
- **Liquibase** (миграции БД)
- **Spring Data JPA + Hibernate**
- **QueryDSL** (для сложных запросов)
- **Swagger/OpenAPI 3** (документация API)
- **Docker & Testcontainers** (для тестирования)

## 📊 Основные функции

1. **Управление клиентами и счетами**
2. **Обработка финансовых транзакций**
3. **Аналитика расходов по категориям**
4. **Обнаружение подозрительных операций**
5. **Генерация отчетов**
6. **Мониторинг производительности**

## 🏗️ Архитектура

```
src/main/java/ru/analytics/
├── domain/                    # Доменный слой
│   ├── model/                # Сущности JPA
│   ├── repository/           # Репозитории Spring Data
│   └── specification/        # Спецификации для Criteria API
├── application/              # Слой приложения
│   ├── service/             # Бизнес-логика
│   ├── dto/                 # Data Transfer Objects
│   └── event/               # События домена
├── infrastructure/          # Инфраструктурный слой
│   ├── web/                # REST контроллеры
│   ├── config/             # Конфигурации
│   └── persistence/        # Реализации репозиториев
└── exception/              # Кастомные исключения
```

## 🔍 Проблемы производительности и их решения

### Проблема 1: N+1 запросов
**Проблема:** Каждая итерация вызывает отдельные SQL запросы для связанных сущностей.

**Решение:**
- `@EntityGraph` для явной загрузки связей
- `JOIN FETCH` в JPQL запросах
- `@BatchSize` на коллекциях
- DTO проекции вместо загрузки сущностей

### Проблема 2: Отсутствие индексов
**Решение:** Создание индексов для часто используемых полей и составных индексов.

### Проблема 3: Некорректные уровни изоляции
**Решение:**
- `READ COMMITTED` для отчетов
- `REPEATABLE READ` для финансовых операций
- Пессимистические блокировки для критичных операций

### Проблема 4: Отсутствие батчинга
**Решение:** Включение `hibernate.jdbc.batch_size` и использование `saveAll()`.

## 🛠️ Быстрый старт

### Предварительные требования

1. **Java 21** (или выше)
2. **PostgreSQL 16+**
3. **Gradle 8.5+**

### Установка и запуск

1. **Клонирование репозитория:**
```bash
git clone <repository-url>
cd analytics-service
```

2. **Настройка базы данных:**
```sql
-- Создание базы данных
CREATE DATABASE fintech_analytics;

-- Создание пользователя (опционально)
CREATE USER fintech_user WITH PASSWORD 'fintech_pass';
GRANT ALL PRIVILEGES ON DATABASE fintech_analytics TO fintech_user;
```

3. **Конфигурация:**
   Создайте `application-local.yml` в `src/main/resources/`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fintech_analytics
    username: postgres
    password: postgres

  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

4. **Запуск приложения:**
```bash
# Сборка проекта
./gradlew clean build

# Запуск
./gradlew bootRun

# Или запуск с профилем
./gradlew bootRun --args='--spring.profiles.active=local'
```

5. **Проверка работоспособности:**
- Откройте Swagger UI: http://localhost:8080/swagger-ui.html
- Проверьте health endpoint: http://localhost:8080/api/v1/health

## 📚 API Документация

### Основные endpoints

#### Аналитика
- `GET /api/v1/analytics/clients/optimized` - Оптимизированный запрос клиентов
- `GET /api/v1/analytics/clients/naive` - Наивный запрос (для сравнения)
- `GET /api/v1/analytics/demo/n-plus-one` - Демонстрация N+1 проблемы

#### Транзакции
- `POST /api/v1/transactions/transfer` - Перевод между счетами
- `GET /api/v1/transactions/demo/isolation-levels` - Демонстрация уровней изоляции

#### Клиенты
- `POST /api/v1/clients` - Создать клиента
- `GET /api/v1/clients` - Все клиенты с пагинацией
- `GET /api/v1/clients/{id}` - Клиент по ID
- `PUT /api/v1/clients/{id}` - Полное обновление клиента
- `PATCH /api/v1/clients/{id}` - Частичное обновление клиента
- `DELETE /api/v1/clients/{id}` - Удалить клиента
- `GET /api/v1/clients/with-details` - Клиенты с деталями (EntityGraph)

#### Счета
- `POST /api/v1/accounts` - Создать счет
- `GET /api/v1/accounts` - Все счета
- `GET /api/v1/accounts/{id}` - Счет по ID
- `PUT /api/v1/accounts/{id}` - Полное обновление счета
- `PATCH /api/v1/accounts/{id}` - Частичное обновление счета
- `DELETE /api/v1/accounts/{id}` - Удалить счет

### Примеры запросов

#### Создание перевода:
```bash
curl -X POST "http://localhost:8080/api/v1/transactions/transfer" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 1000.00,
    "currencyCode": "USD",
    "description": "Межбанковский перевод",
    "externalReference": "TRF-2024-001"
  }'
```

#### Получение оптимизированной аналитики по клиентам:
```bash
curl -X GET "http://localhost:8080/api/v1/analytics/clients/optimized?page=0&size=20"
```

## 🧪 Тестирование

### Запуск тестов
```bash
# Все тесты
./gradlew test

# Специфичный тест
./gradlew test --tests TransactionOptimizationTest

# С тестовым покрытием
./gradlew test jacocoTestReport
```

### Интеграционные тесты
Проект использует Testcontainers для интеграционного тестирования с PostgreSQL.

## 🔧 Конфигурация

### Основные настройки

#### application.yml
```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        default_batch_fetch_size: 100
        format_sql: true
        generate_statistics: true

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

### Профили

- **local** - локальная разработка
- **test** - тестирование (использует H2)
- **prod** - продукционная конфигурация

## 📊 Мониторинг

### Actuator endpoints
- `/actuator/health` - Состояние сервиса
- `/actuator/metrics` - Метрики приложения
- `/actuator/prometheus` - Метрики в формате Prometheus

### Кастомные метрики
- Количество N+1 проблем
- Время выполнения запросов
- Статистика кэша

## 🐳 Docker

### Сборка Docker образа
```bash
docker build -t fintech-analytics:latest .
```

### Запуск с Docker Compose
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: fintech_analytics
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  analytics-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/fintech_analytics
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on:
      - postgres

volumes:
  postgres_data:
```

## 🎓 Учебные материалы

### Практические задания для студентов

1. **Задание 1**: Реализовать метод получения транзакций с пагинацией, избегая N+1
2. **Задание 2**: Создать спецификацию для поиска подозрительных транзакций
3. **Задание 3**: Реализовать пакетную вставку 1000 транзакций с оптимизацией
4. **Задание 4**: Написать тест, демонстрирующий разницу в изоляции транзакций
5. **Задание 5**: Оптимизировать запрос построения monthly report с помощью оконных функций

### Кейсы для разбора

#### Кейс 1: Медленный отчет клиентов
**Симптомы:** Отчет о клиентах с их транзакциями выполняется очень медленно при большом количестве данных.

**Диагностика:**
- Проверить логи Hibernate на наличие N+1 запросов
- Анализировать план выполнения SQL запросов

**Решение:**
- Использовать `@EntityGraph` или `JOIN FETCH`
- Применить пагинацию
- Использовать DTO проекции

#### Кейс 2: Блокировки при конкурентных переводах
**Симптомы:** Deadlock при одновременных переводах.

**Решение:**
- Использовать пессимистические блокировки
- Настроить таймауты транзакций
- Реализовать retry механизм

## 📈 Производительность

### Рекомендации по оптимизации

1. **Индексы:**
   ```sql
   CREATE INDEX idx_transaction_account_created 
   ON transactions(account_id, created_at DESC);
   ```

2. **Настройки Hibernate:**
   ```yaml
   hibernate:
     jdbc:
       batch_size: 50
     order_inserts: true
     order_updates: true
     default_batch_fetch_size: 100
   ```

3. **Мониторинг:**
    - Включить `hibernate.generate_statistics: true`
    - Использовать `RepositoryPerformanceAspect`

## 🤝 Вклад в проект

### Установка для разработки

1. Форкните репозиторий
2. Создайте feature ветку: `git checkout -b feature/amazing-feature`
3. Сделайте коммит изменений: `git commit -m 'Add amazing feature'`
4. Запушьте ветку: `git push origin feature/amazing-feature`
5. Откройте Pull Request

### Code Style

- Используйте Java 21 фичи (Records, Pattern Matching)
- Следуйте Google Java Style Guide
- Пишите юнит-тесты для нового функционала
- Документируйте публичные API

## 🐛 Отчеты об ошибках

Используйте [Issues](ссылка на issues) для сообщения об ошибках или предложения улучшений.

## 📄 Лицензия

Этот проект лицензирован под MIT License - смотрите файл [LICENSE](LICENSE) для деталей.

## 👥 Авторы

- **Команда аналитики** - [analytics@fintech.ru](mailto:analytics@fintech.ru)

## 🙏 Благодарности

- Spring Framework Team
- Hibernate Team
- Сообщество Java разработчиков

---

## 🚀 Полезные команды

```bash
# Просмотр зависимостей
./gradlew dependencies

# Запуск с определенным профилем
./gradlew bootRun --args='--spring.profiles.active=local'

# Создание fat-jar
./gradlew bootJar

# Проверка стиля кода
./gradlew spotlessCheck
./gradlew spotlessApply
```

## 🔗 Полезные ссылки

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Hibernate User Guide](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [QueryDSL Reference](http://www.querydsl.com/static/querydsl/latest/reference/html/)

---

**Happy coding!** 🚀

Если у вас есть вопросы или нужна помощь, создавайте issue или обращайтесь к команде разработки.
