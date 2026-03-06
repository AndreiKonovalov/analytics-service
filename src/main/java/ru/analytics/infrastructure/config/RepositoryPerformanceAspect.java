package ru.analytics.infrastructure.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Аспект для мониторинга производительности репозиториев
 * Обнаруживает N+1 проблемы и медленные запросы
 */
@Aspect
@Component
@Slf4j
public class RepositoryPerformanceAspect {

    @PersistenceContext
    private EntityManager entityManager;

    private final Map<String, QueryStats> queryStats = new ConcurrentHashMap<>();
    private final AtomicInteger nPlusOneDetections = new AtomicInteger(0);

    @Pointcut("execution(* ru.analytics.domain.repository.*.*(..))")
    public void repositoryMethods() {}

    @Pointcut("execution(* ru.analytics.application.service.*.*(..))")
    public void serviceMethods() {}

    /**
     * Мониторинг производительности запросов
     */
    @Around("repositoryMethods() || serviceMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Statistics stats = getStatistics();
        long initialQueryCount = stats.getQueryExecutionCount();
        long initialTransactionCount = stats.getTransactionCount();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // Анализ производительности
            analyzePerformance(joinPoint, duration, stats, initialQueryCount, initialTransactionCount);

            return result;
        } catch (Exception e) {
            log.error("Error in method {}: {}", joinPoint.getSignature(), e.getMessage());
            throw e;
        }
    }

    /**
     * Анализ производительности выполнения метода
     */
    private void analyzePerformance(
            ProceedingJoinPoint joinPoint,
            long duration,
            Statistics stats,
            long initialQueryCount,
            long initialTransactionCount
    ) {
        String methodName = joinPoint.getSignature().toShortString();
        long queryCount = stats.getQueryExecutionCount() - initialQueryCount;

        // Логируем медленные запросы
        if (duration > 100) { // Более 100ms считается медленным
            log.warn("SLOW METHOD: {} executed in {} ms with {} queries",
                    methodName, duration, queryCount);

            // Записываем статистику
            queryStats.computeIfAbsent(methodName, k -> new QueryStats())
                    .recordExecution(duration, queryCount);
        }

        // Обнаружение N+1 проблем
        if (queryCount > 10 && duration > 50) { // Эвристика для N+1
            long transactionCount = stats.getTransactionCount() - initialTransactionCount;
            if (queryCount > transactionCount * 5) { // Подозрительное соотношение
                int count = nPlusOneDetections.incrementAndGet();
                log.error("N+1 DETECTED in {}: {} queries in {} transactions ({} ms). Total detections: {}",
                        methodName, queryCount, transactionCount, duration, count);

                // Логируем детали для отладки
                if (log.isDebugEnabled()) {
                    String[] queryStrings = stats.getQueries();
                    if (queryStrings.length > 0) {
                        log.debug("Executed queries:");
                        for (String query : queryStrings) {
                            log.debug("  - {}", query);
                        }
                    }
                }
            }
        }

        // Периодический отчет
        if (queryStats.size() % 10 == 0) {
            logReport();
        }
    }

    /**
     * Получение статистики Hibernate
     */
    private Statistics getStatistics() {
        return entityManager.getEntityManagerFactory()
                .unwrap(org.hibernate.SessionFactory.class)
                .getStatistics();
    }

    /**
     * Генерация отчета о производительности
     */
    private void logReport() {
        if (queryStats.isEmpty()) {
            return;
        }

        log.info("=== PERFORMANCE REPORT ===");
        log.info("Total methods monitored: {}", queryStats.size());
        log.info("N+1 detections: {}", nPlusOneDetections.get());

        queryStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().getAverageTime(), a.getValue().getAverageTime()))
                .limit(10)
                .forEach(entry -> {
                    QueryStats stats = entry.getValue();
                    log.info("Method: {}", entry.getKey());
                    log.info("  - Calls: {}", stats.getCallCount());
                    log.info("  - Avg time: {} ms", stats.getAverageTime());
                    log.info("  - Max time: {} ms", stats.getMaxTime());
                    log.info("  - Avg queries per call: {}", stats.getAverageQueries());
                });

        log.info("==========================");
    }

    /**
     * Внутренний класс для хранения статистики запросов
     */
    private static class QueryStats {
        @Getter
        private int callCount = 0;
        private long totalTime = 0;
        @Getter
        private long maxTime = 0;
        private long totalQueries = 0;

        public synchronized void recordExecution(long duration, long queryCount) {
            callCount++;
            totalTime += duration;
            totalQueries += queryCount;
            if (duration > maxTime) {
                maxTime = duration;
            }
        }

        public long getAverageTime() {
            return callCount > 0 ? totalTime / callCount : 0;
        }

        public long getAverageQueries() {
            return callCount > 0 ? totalQueries / callCount : 0;
        }
    }
}
