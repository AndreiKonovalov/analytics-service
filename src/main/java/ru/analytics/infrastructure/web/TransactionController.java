package ru.analytics.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.analytics.application.dto.TransferRequest;
import ru.analytics.application.dto.TransferResult;
import ru.analytics.application.service.TransactionProcessingService;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Транзакции", description = "API для управления финансовыми транзакциями")
@Slf4j
public class TransactionController {

    private final TransactionProcessingService transactionProcessingService;

    @Operation(summary = "Выполнить перевод между счетами")
    @PostMapping("/transfer")
    public ResponseEntity<TransferResult> transfer(
            @Valid @RequestBody TransferRequest transferRequest) {
        log.info("Обработка перевода: {}", transferRequest);

        try {
            TransferResult result = transactionProcessingService
                    .processTransferWithPessimisticLock(transferRequest);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Ошибка при выполнении перевода", e);
            return ResponseEntity.badRequest()
                    .body(TransferResult.failure(e.getMessage()));
        }
    }

    @Operation(summary = "Демонстрация уровней изоляции транзакций")
    @GetMapping("/demo/isolation-levels")
    public ResponseEntity<String> demonstrateIsolationLevels() {
        log.info("Демонстрация уровней изоляции транзакций");
        transactionProcessingService.demonstrateIsolationLevels();
        return ResponseEntity.ok("Демонстрация завершена, проверьте логи для деталей");
    }
}
