package ru.analytics.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.analytics.application.dto.TransferRequest;
import ru.analytics.application.dto.TransferResult;
import ru.analytics.application.event.TransferCompletedEvent;
import ru.analytics.domain.model.Account;
import ru.analytics.domain.model.Transaction;
import ru.analytics.domain.repository.AccountRepository;
import ru.analytics.domain.repository.TransactionRepository;
import ru.analytics.exception.AccountNotFoundException;
import ru.analytics.exception.InsufficientFundsException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сервис для обработки транзакций с различными уровнями изоляции
 * Демонстрирует правильное использование транзакций
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionProcessingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Пример 1: Транзакция с пессимистической блокировкой
     * Используется для критических финансовых операций
     */
    @Transactional(
            isolation = Isolation.REPEATABLE_READ,
            propagation = Propagation.REQUIRED,
            timeout = 30
    )
    public TransferResult processTransferWithPessimisticLock(TransferRequest request) {
        log.info("Processing transfer with pessimistic lock: {}", request);

        // Используем пессимистическую блокировку для предотвращения race conditions
        Account fromAccount = accountRepository.findByIdWithLock(request.fromAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.fromAccountId().toString()));

        Account toAccount = accountRepository.findByIdWithLock(request.toAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.toAccountId().toString()));

        return executeTransfer(fromAccount, toAccount, request);
    }


    /**
     * Вспомогательный метод для выполнения перевода
     */
    private TransferResult executeTransfer(Account fromAccount, Account toAccount, TransferRequest request) {
        // Проверка валюты
        if (!fromAccount.getCurrencyCode().equals(request.currencyCode())) {
            throw new IllegalArgumentException(
                    "Currency mismatch: account has " + fromAccount.getCurrencyCode() +
                            ", transfer is " + request.currencyCode()
            );
        }

        // Проверка баланса
        if (fromAccount.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds in account " + fromAccount.getAccountNumber() +
                            ". Balance: " + fromAccount.getBalance() +
                            ", Required: " + request.amount()
            );
        }

        // Проверка активности счетов
        if (!fromAccount.isActive()) {
            throw new IllegalStateException("Source account is not active");
        }

        if (!toAccount.isActive()) {
            throw new IllegalStateException("Destination account is not active");
        }

        // Выполнение перевода
        fromAccount.debit(request.amount());
        toAccount.credit(request.amount());

        // Сохранение счетов
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Создание транзакций
        String externalReference = request.externalReference() != null
                ? request.externalReference()
                : UUID.randomUUID().toString();

        Transaction debitTransaction = createTransaction(
                fromAccount, request.amount().negate(), "DEBIT", request, externalReference
        );

        Transaction creditTransaction = createTransaction(
                toAccount, request.amount(), "CREDIT", request, externalReference
        );

        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);

        // Публикация события
        eventPublisher.publishEvent(new TransferCompletedEvent(
                this, request, externalReference, LocalDateTime.now()
        ));

        log.info("Transfer completed successfully: {}", externalReference);

        return TransferResult.success(
                debitTransaction.getId().toString(),
                externalReference,
                fromAccount.getBalance(),
                toAccount.getBalance()
        );
    }

    /**
     * Создание транзакции
     */
    private Transaction createTransaction(
            Account account,
            BigDecimal amount,
            String type,
            TransferRequest request,
            String externalReference
    ) {
        return Transaction.builder()
                .account(account)
                .amount(amount)
                .currencyCode(request.currencyCode())
                .description(request.description())
                .counterpartyAccount(
                        type.equals("DEBIT") ? request.toAccountId().toString()
                                : request.fromAccountId().toString()
                )
                .counterpartyName(
                        type.equals("DEBIT") ? "Transfer to account"
                                : "Transfer from account"
                )
                .externalReferenceId(externalReference)
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .executedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Демонстрация разных уровней изоляции
     */
    public void demonstrateIsolationLevels() {
        log.info("=== Уровни изоляции транзакций ===");
        log.info("1. READ UNCOMMITTED: Грязное чтение, неповторяемое чтение, фантомное чтение");
        log.info("2. READ COMMITTED: Неповторяемое чтение, фантомное чтение (стандарт PostgreSQL)");
        log.info("3. REPEATABLE READ: Фантомное чтение");
        log.info("4. SERIALIZABLE: Полная изоляция, но медленнее");
        log.info("");
        log.info("=== Рекомендации по использованию ===");
        log.info("- Отчеты: READ COMMITTED");
        log.info("- Финансовые операции: REPEATABLE READ с блокировками");
        log.info("- Критичные операции: SERIALIZABLE");
    }
}
