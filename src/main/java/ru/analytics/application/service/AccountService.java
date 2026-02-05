package ru.analytics.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.analytics.domain.model.Account;
import ru.analytics.domain.model.Client;
import ru.analytics.domain.model.Transaction;
import ru.analytics.domain.model.enums.TransactionType;
import ru.analytics.domain.repository.AccountRepository;
import ru.analytics.domain.repository.ClientRepository;
import ru.analytics.domain.repository.TransactionRepository;
import ru.analytics.exception.AccountNotFoundException;
import ru.analytics.exception.ClientNotFoundException;
import ru.analytics.infrastructure.web.dto.AccountCreateRequest;
import ru.analytics.infrastructure.web.dto.AccountUpdateRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Account createAccount(AccountCreateRequest request) {
        log.debug("Создание счета для клиента: {}", request.getClientId());

        // Проверяем существование клиента
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ClientNotFoundException(request.getClientId()));

        // Проверяем уникальность номера счета
        accountRepository.findByAccountNumber(request.getAccountNumber())
                .ifPresent(account -> {
                    throw new IllegalArgumentException("Счет с номером " +
                            request.getAccountNumber() + " уже существует");
                });

        Account account = Account.builder()
                .accountNumber(request.getAccountNumber())
                .balance(request.getInitialBalance())
                .creditLimit(request.getCreditLimit())
                .type(ru.analytics.domain.model.enums.AccountType.valueOf(
                        request.getAccountType()))
                .currencyCode(request.getCurrencyCode())
                .isActive(true)
                .client(client)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Если есть начальный баланс, создаем транзакцию
        if (request.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            Transaction transaction = Transaction.builder()
                    .account(account)
                    .amount(request.getInitialBalance())
                    .client(client)
                    .currencyCode(request.getCurrencyCode())
                    .type(TransactionType.DEPOSIT)
                    .description("Initial deposit")
                    .status("COMPLETED")
                    .createdAt(LocalDateTime.now())
                    .executedAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(transaction);
        }

        return accountRepository.save(account);
    }

    @Transactional
    public Account updateAccount(Long id, AccountUpdateRequest request) {
        log.debug("Обновление счета ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        if (request.getAccountType() != null) {
            account.setType(ru.analytics.domain.model.enums.AccountType.valueOf(
                    request.getAccountType()));
        }
        if (request.getCurrencyCode() != null) {
            account.setCurrencyCode(request.getCurrencyCode());
        }
        if (request.getCreditLimit() != null) {
            account.setCreditLimit(request.getCreditLimit());
        }
        if (request.getIsActive() != null) {
            account.setActive(request.getIsActive());
        }

        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    @Transactional
    public Account partialUpdateAccount(Long id, AccountUpdateRequest request) {
        // Используем ту же логику
        return updateAccount(id, request);
    }

    @Transactional
    public void deleteAccount(Long id) {
        log.debug("Удаление счета ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        // Проверяем, что баланс равен 0
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Нельзя удалить счет с ненулевым балансом. " +
                    "Текущий баланс: " + account.getBalance());
        }

        accountRepository.delete(account);
    }

    // ========== GET METHODS ==========

    @Transactional(readOnly = true)
    public Page<Account> getAllAccounts(Pageable pageable) {
        log.debug("Получение всех счетов");
        return accountRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Account getAccountById(Long id) {
        log.debug("Получение счета по ID: {}", id);
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

}
