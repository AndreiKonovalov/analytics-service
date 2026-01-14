package ru.analytics.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.analytics.application.dto.TransferRequest;
import ru.analytics.application.dto.TransferResult;
import ru.analytics.application.service.TransactionProcessingService;

import java.math.BigDecimal;

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

    @Operation(summary = "Выполнить перевод с оптимистической блокировкой")
    @PostMapping("/transfer/optimistic")
    public ResponseEntity<TransferResult> transferOptimistic(
            @Valid @RequestBody TransferRequest transferRequest) {
        log.info("Обработка перевода с оптимистической блокировкой: {}", transferRequest);

        try {
            TransferResult result = transactionProcessingService
                    .processTransferWithOptimisticLock(transferRequest);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Ошибка при выполнении перевода с оптимистической блокировкой", e);
            return ResponseEntity.badRequest()
                    .body(TransferResult.failure(e.getMessage()));
        }
    }

    @Operation(summary = "Выполнить крупный перевод (сериализуемый уровень изоляции)")
    @PostMapping("/transfer/high-value")
    public ResponseEntity<TransferResult> transferHighValue(
            @Valid @RequestBody TransferRequest transferRequest) {
        log.info("Обработка крупного перевода: {}", transferRequest);

        if (transferRequest.amount().compareTo(new BigDecimal("100000")) <= 0) {
            return ResponseEntity.badRequest()
                    .body(TransferResult.failure("Для вызова этого метода сумма должна быть больше 100,000"));
        }

        try {
            TransferResult result = transactionProcessingService
                    .processHighValueTransfer(transferRequest);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Ошибка при выполнении крупного перевода", e);
            return ResponseEntity.badRequest()
                    .body(TransferResult.failure(e.getMessage()));
        }
    }

    @Operation(summary = "Получить баланс счета")
    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable Long accountId) {
        log.info("Получение баланса счета: {}", accountId);

        try {
            BigDecimal balance = transactionProcessingService.getAccountBalance(accountId);
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            log.error("Ошибка при получении баланса", e);
            return ResponseEntity.badRequest().body(BigDecimal.ZERO);
        }
    }

    @Operation(summary = "Демонстрация уровней изоляции транзакций")
    @GetMapping("/demo/isolation-levels")
    public ResponseEntity<String> demonstrateIsolationLevels() {
        log.info("Демонстрация уровней изоляции транзакций");
        transactionProcessingService.demonstrateIsolationLevels();
        return ResponseEntity.ok("Демонстрация завершена, проверьте логи для деталей");
    }

    @Operation(summary = "Пакетная обработка переводов")
    @PostMapping("/transfer/batch")
    public ResponseEntity<Integer> processBatchTransfers(
            @Valid @RequestBody java.util.List<TransferRequest> transferRequests) {
        log.info("Пакетная обработка {} переводов", transferRequests.size());

        if (transferRequests.size() > 100) {
            return ResponseEntity.badRequest().body(-1);
        }

        try {
            int successCount = transactionProcessingService
                    .processBatchTransfers(transferRequests);
            return ResponseEntity.ok(successCount);
        } catch (Exception e) {
            log.error("Ошибка при пакетной обработке переводов", e);
            return ResponseEntity.badRequest().body(0);
        }
    }
}