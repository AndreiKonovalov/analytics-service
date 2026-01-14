package ru.analytics.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Здоровье", description = "API для мониторинга состояния сервиса")
@Slf4j
public class HealthController {

    private final HealthEndpoint healthEndpoint;
    private final MetricsEndpoint metricsEndpoint;

    @Operation(summary = "Проверка здоровья сервиса")
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        log.info("Проверка здоровья сервиса");

        HealthComponent health = healthEndpoint.health();
        Map<String, Object> response = new HashMap<>();
        response.put("status", health.getStatus().getCode());
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "fintech-analytics-service");
        response.put("version", "0.0.1-SNAPSHOT");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Получить метрики производительности")
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {
        log.info("Получение метрик производительности");

        Map<String, Object> metrics = new HashMap<>();

        // Пример получения некоторых метрик
        metrics.put("jvm.memory.used",
                metricsEndpoint.metric("jvm.memory.used", null).getMeasurements());
        metrics.put("jvm.threads.live",
                metricsEndpoint.metric("jvm.threads.live", null).getMeasurements());
        metrics.put("http.server.requests",
                metricsEndpoint.metric("http.server.requests", null).getMeasurements());

        return ResponseEntity.ok(metrics);
    }

    @Operation(summary = "Информация о сервисе")
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        log.info("Получение информации о сервисе");

        Map<String, Object> info = new HashMap<>();
        info.put("name", "FinTech Analytics Service");
        info.put("description", "Микросервис для аналитики финансовых транзакций");
        info.put("version", "0.0.1-SNAPSHOT");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("profiles", System.getProperty("spring.profiles.active", "default"));
        info.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(info);
    }
}