package ru.analytics.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.analytics.application.service.AccountService;
import ru.analytics.domain.model.Account;
import ru.analytics.infrastructure.web.dto.AccountCreateRequest;
import ru.analytics.infrastructure.web.dto.AccountUpdateRequest;
import ru.analytics.infrastructure.web.dto.DepositRequest;
import ru.analytics.infrastructure.web.dto.WithdrawRequest;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Счета", description = "API для управления банковскими счетами")
@Slf4j
public class AccountController {

    private final AccountService accountService;

    // ========== CRUD OPERATIONS ==========

    @Operation(summary = "Создать новый счет")
    @PostMapping
    public ResponseEntity<Account> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        log.info("Создание нового счета для клиента: {}", request.getClientId());
        Account createdAccount = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @Operation(summary = "Обновить данные счета")
    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountUpdateRequest request) {
        log.info("Обновление счета ID: {}", id);
        Account updatedAccount = accountService.updateAccount(id, request);
        return ResponseEntity.ok(updatedAccount);
    }

    @Operation(summary = "Частичное обновление счета")
    @PatchMapping("/{id}")
    public ResponseEntity<Account> partialUpdateAccount(
            @PathVariable Long id,
            @RequestBody AccountUpdateRequest request) {
        log.info("Частичное обновление счета ID: {}", id);
        Account updatedAccount = accountService.partialUpdateAccount(id, request);
        return ResponseEntity.ok(updatedAccount);
    }

    @Operation(summary = "Удалить счет")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        log.info("Удаление счета ID: {}", id);
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Закрыть счет (мягкое удаление)")
    @PostMapping("/{id}/close")
    public ResponseEntity<Account> closeAccount(@PathVariable Long id) {
        log.info("Закрытие счета ID: {}", id);
        Account closedAccount = accountService.closeAccount(id);
        return ResponseEntity.ok(closedAccount);
    }

    // ========== FINANCIAL OPERATIONS ==========

    @Operation(summary = "Пополнить счет")
    @PostMapping("/{id}/deposit")
    public ResponseEntity<Account> deposit(
            @PathVariable Long id,
            @Valid @RequestBody DepositRequest request) {
        log.info("Пополнение счета ID: {} на сумму: {}", id, request.getAmount());
        Account account = accountService.deposit(id, request);
        return ResponseEntity.ok(account);
    }

    @Operation(summary = "Снять средства со счета")
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Account> withdraw(
            @PathVariable Long id,
            @Valid @RequestBody WithdrawRequest request) {
        log.info("Снятие средств со счета ID: {}, сумма: {}", id, request.getAmount());
        Account account = accountService.withdraw(id, request);
        return ResponseEntity.ok(account);
    }

    @Operation(summary = "Перевести средства между счетами")
    @PostMapping("/transfer")
    public ResponseEntity<String> transferBetweenAccounts(
            @RequestParam Long fromAccountId,
            @RequestParam Long toAccountId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        log.info("Перевод с {} на {}, сумма: {}", fromAccountId, toAccountId, amount);
        accountService.transfer(fromAccountId, toAccountId, amount, description);
        return ResponseEntity.ok("Перевод успешно выполнен");
    }

    @Operation(summary = "Блокировать счет")
    @PostMapping("/{id}/block")
    public ResponseEntity<Account> blockAccount(@PathVariable Long id) {
        log.info("Блокировка счета ID: {}", id);
        Account account = accountService.blockAccount(id);
        return ResponseEntity.ok(account);
    }

    @Operation(summary = "Разблокировать счет")
    @PostMapping("/{id}/unblock")
    public ResponseEntity<Account> unblockAccount(@PathVariable Long id) {
        log.info("Разблокировка счета ID: {}", id);
        Account account = accountService.unblockAccount(id);
        return ResponseEntity.ok(account);
    }

    // ========== GET OPERATIONS (существующие) ==========

    @Operation(summary = "Получить все счета")
    @GetMapping
    public ResponseEntity<Page<Account>> getAllAccounts(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Получение всех счетов, страница: {}", pageable.getPageNumber());
        return ResponseEntity.ok(accountService.getAllAccounts(pageable));
    }

    @Operation(summary = "Получить счет по ID")
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        log.info("Получение счета по ID: {}", id);
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @Operation(summary = "Получить счет по номеру")
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<Account> getAccountByNumber(@PathVariable String accountNumber) {
        log.info("Получение счета по номеру: {}", accountNumber);
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @Operation(summary = "Получить счета клиента")
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Account>> getAccountsByClient(@PathVariable Long clientId) {
        log.info("Получение счетов клиента: {}", clientId);
        return ResponseEntity.ok(accountService.getAccountsByClient(clientId));
    }

    @Operation(summary = "Получить активные счета")
    @GetMapping("/active")
    public ResponseEntity<List<Account>> getActiveAccounts() {
        log.info("Получение активных счетов");
        return ResponseEntity.ok(accountService.getActiveAccounts());
    }

    @Operation(summary = "Получить счета с высоким балансом")
    @GetMapping("/high-balance")
    public ResponseEntity<List<Account>> getHighBalanceAccounts(
            @RequestParam(defaultValue = "10000") BigDecimal minBalance) {
        log.info("Получение счетов с балансом выше: {}", minBalance);
        return ResponseEntity.ok(accountService.getHighBalanceAccounts(minBalance));
    }

    @Operation(summary = "Получить счет с деталями")
    @GetMapping("/{id}/with-details")
    public ResponseEntity<Account> getAccountWithDetails(@PathVariable Long id) {
        log.info("Получение счета с деталями по ID: {}", id);
        return ResponseEntity.ok(accountService.getAccountWithDetails(id));
    }

    @Operation(summary = "Получить баланс счета")
    @GetMapping("/{id}/balance")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable Long id) {
        log.info("Получение баланса счета ID: {}", id);
        BigDecimal balance = accountService.getAccountBalance(id);
        return ResponseEntity.ok(balance);
    }

    @Operation(summary = "Проверить достаточность средств")
    @GetMapping("/{id}/check-funds")
    public ResponseEntity<Boolean> checkSufficientFunds(
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        log.info("Проверка средств на счете ID: {} для суммы: {}", id, amount);
        boolean hasFunds = accountService.hasSufficientFunds(id, amount);
        return ResponseEntity.ok(hasFunds);
    }
}