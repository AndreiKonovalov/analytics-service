package ru.analytics.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.analytics.application.dto.ClientReportDTO;
import ru.analytics.application.service.OptimizedReportService;
import ru.analytics.application.service.TransactionReportService;

import java.util.List;

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

    @Operation(summary = "Демонстрация N+1 проблемы")
    @GetMapping("/demo/n-plus-one")
    public ResponseEntity<String> demonstrateNPlusOneProblem() {
        log.info("Демонстрация N+1 проблемы");
        transactionReportService.getClientsWithTransactionsNaive();
        return ResponseEntity.ok("N+1 проблема продемонстрирована, проверьте логи SQL запросов");
    }
}
