package ru.analytics.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.analytics.application.dto.AnalyticsReport;
import ru.analytics.application.dto.ClientReportDTO;
import ru.analytics.application.service.OptimizedReportService;
import ru.analytics.application.service.TransactionReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Аналитика", description = "API для аналитики финансовых данных")
@Slf4j
public class AnalyticsController {

    private final OptimizedReportService optimizedReportService;
    private final TransactionReportService transactionReportService;

    @Operation(summary = "Получить клиентов с транзакциями (оптимизированный)")
    @GetMapping("/clients/optimized")
    public ResponseEntity<Page<ClientReportDTO>> getClientsOptimized(
            @PageableDefault(size = 20, sort = "clientId") Pageable pageable) {
        log.info("Получение клиентов с оптимизированным подходом, страница: {}", pageable.getPageNumber());
        return ResponseEntity.ok(optimizedReportService.getClientsWithTransactionsOptimized(pageable));
    }

    @Operation(summary = "Получить клиентов с транзакциями (наивный подход - для сравнения)")
    @GetMapping("/clients/naive")
    public ResponseEntity<List<ClientReportDTO>> getClientsNaive() {
        log.info("Получение клиентов с наивным подходом");
        return ResponseEntity.ok(transactionReportService.getClientsWithTransactionsNaive());
    }

    @Operation(summary = "Получить сводный отчет по клиентам")
    @GetMapping("/clients/summary")
    public ResponseEntity<List<Map<String, Object>>> getClientSummaryReport() {
        log.info("Получение сводного отчета по клиентам");
        return ResponseEntity.ok(optimizedReportService.getClientSummaryReport());
    }

    @Operation(summary = "Топ клиентов по балансу")
    @GetMapping("/clients/top-by-balance")
    public ResponseEntity<Page<ClientReportDTO>> getTopClientsByBalance(
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("Получение топ клиентов по балансу");
        return ResponseEntity.ok(optimizedReportService.getTopClientsByBalance(pageable));
    }

    @Operation(summary = "Анализ расходов по категориям")
    @GetMapping("/spending/category-analysis")
    public ResponseEntity<Map<String, BigDecimal>> getCategorySpendingAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        log.info("Анализ расходов по категориям с {} по {}", from, to);
        return ResponseEntity.ok(optimizedReportService.getCategorySpendingSummary(from, to));
    }

    @Operation(summary = "Сравнение производительности подходов")
    @GetMapping("/performance/comparison")
    public ResponseEntity<String> comparePerformance() {
        log.info("Сравнение производительности подходов");
        optimizedReportService.generateLargeReportComparison();
        return ResponseEntity.ok("Сравнение завершено, проверьте логи для деталей");
    }

    @Operation(summary = "Генерация полного аналитического отчета")
    @GetMapping("/reports/full")
    public ResponseEntity<AnalyticsReport> generateFullAnalyticsReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Генерация полного отчета с {} по {}", startDate, endDate);
        // TODO: Реализовать генерацию полного отчета
        return ResponseEntity.ok(AnalyticsReport.builder()
                .reportDate(LocalDate.now())
                .generatedAt(LocalDateTime.now())
                .build());
    }

    @Operation(summary = "Демонстрация N+1 проблемы")
    @GetMapping("/demo/n-plus-one")
    public ResponseEntity<String> demonstrateNPlusOneProblem() {
        log.info("Демонстрация N+1 проблемы");
        transactionReportService.getClientsWithTransactionsNaive();
        return ResponseEntity.ok("N+1 проблема продемонстрирована, проверьте логи SQL запросов");
    }

    @Operation(summary = "Статистика производительности")
    @GetMapping("/performance/stats")
    public ResponseEntity<Map<String, Object>> getPerformanceStats() {
        log.info("Получение статистики производительности");
        // TODO: Реализовать сбор статистики
        return ResponseEntity.ok(Map.of(
                "status", "metrics collection not implemented yet",
                "timestamp", LocalDateTime.now()
        ));
    }
}
