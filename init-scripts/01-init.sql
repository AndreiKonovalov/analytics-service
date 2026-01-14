-- init-scripts/01-init.sql
CREATE SCHEMA IF NOT EXISTS analytics;

-- Создаем пользователя для приложения (опционально)
CREATE USER analytics_user WITH PASSWORD 'analytics_pass';

-- Даем права на схему
GRANT ALL PRIVILEGES ON SCHEMA analytics TO analytics_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA analytics TO analytics_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA analytics TO analytics_user;