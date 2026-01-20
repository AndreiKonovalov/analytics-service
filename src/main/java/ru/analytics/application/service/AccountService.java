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
import ru.analytics.exception.InsufficientFundsException;
import ru.analytics.infrastructure.web.dto.AccountCreateRequest;
import ru.analytics.infrastructure.web.dto.AccountUpdateRequest;
import ru.analytics.infrastructure.web.dto.DepositRequest;
import ru.analytics.infrastructure.web.dto.WithdrawRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    @Transactional
    public Account closeAccount(Long id) {
        log.debug("Закрытие счета ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.setActive(false);
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    @Transactional
    public Account deposit(Long accountId, DepositRequest request) {
        log.debug("Пополнение счета ID: {} на сумму: {}", accountId, request.getAmount());

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        account.getClient().getId();

        if (!account.isActive()) {
            throw new IllegalStateException("Счет заблокирован или закрыт");
        }

        // Увеличиваем баланс
        account.credit(request.getAmount());
        account.setUpdatedAt(LocalDateTime.now());

        // Создаем транзакцию
        Transaction transaction = Transaction.builder()
                .account(account)
                .amount(request.getAmount())
                .client(account.getClient())
                .currencyCode(account.getCurrencyCode())
                .type(TransactionType.DEPOSIT)
                .description(request.getDescription())
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .executedAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        accountRepository.save(account);

        return account;
    }

    @Transactional
    public Account withdraw(Long accountId, WithdrawRequest request) {
        log.debug("Снятие со счета ID: {} суммы: {}", accountId, request.getAmount());

        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (!account.isActive()) {
            throw new IllegalStateException("Счет заблокирован или закрыт");
        }

        // Проверяем достаточность средств
        BigDecimal availableBalance = account.getBalance().add(account.getCreditLimit());
        if (availableBalance.compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Недостаточно средств. Доступно: " + availableBalance +
                            ", требуется: " + request.getAmount());
        }

        // Снимаем средства
        account.debit(request.getAmount());
        account.setUpdatedAt(LocalDateTime.now());

        // Создаем транзакцию
        Transaction transaction = Transaction.builder()
                .account(account)
                .amount(request.getAmount().negate())
                .client(account.getClient())
                .currencyCode(account.getCurrencyCode())
                .type(TransactionType.WITHDRAWAL)
                .description(request.getDescription())
                .status("COMPLETED")
                .createdAt(LocalDateTime.now())
                .executedAt(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        accountRepository.save(account);

        return account;
    }

    @Transactional
    public void transfer(Long fromAccountId, Long toAccountId,
                         BigDecimal amount, String description) {
        log.debug("Перевод с {} на {}, сумма: {}", fromAccountId, toAccountId, amount);

        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Нельзя переводить на тот же счет");
        }

        // Используем пессимистичную блокировку для избежания deadlock
        Account fromAccount = accountRepository.findByIdWithLock(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(fromAccountId));

        Account toAccount = accountRepository.findByIdWithLock(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(toAccountId));

        if (!fromAccount.isActive() || !toAccount.isActive()) {
            throw new IllegalStateException("Один из счетов заблокирован или закрыт");
        }

        if (!fromAccount.getCurrencyCode().equals(toAccount.getCurrencyCode())) {
            throw new IllegalArgumentException("Валюты счетов не совпадают");
        }

        // Снимаем со счета отправителя
        WithdrawRequest withdrawRequest = new WithdrawRequest();
        withdrawRequest.setAmount(amount);
        withdrawRequest.setDescription(description != null ?
                description : "Transfer to account " + toAccountId);
        withdraw(fromAccountId, withdrawRequest);

        // Пополняем счет получателя
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(amount);
        depositRequest.setDescription(description != null ?
                description : "Transfer from account " + fromAccountId);
        deposit(toAccountId, depositRequest);
    }

    @Transactional
    public Account blockAccount(Long id) {
        log.debug("Блокировка счета ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.setActive(false);
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
    }

    @Transactional
    public Account unblockAccount(Long id) {
        log.debug("Разблокировка счета ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        account.setActive(true);
        account.setUpdatedAt(LocalDateTime.now());

        return accountRepository.save(account);
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

    @Transactional(readOnly = true)
    public Account getAccountByNumber(String accountNumber) {
        log.debug("Получение счета по номеру: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Счет с номером " + accountNumber + " не найден"));
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByClient(Long clientId) {
        log.debug("Получение счетов клиента: {}", clientId);
        return accountRepository.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public List<Account> getActiveAccounts() {
        log.debug("Получение активных счетов");
        return accountRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Account> getHighBalanceAccounts(BigDecimal minBalance) {
        log.debug("Получение счетов с балансом выше: {}", minBalance);
        return accountRepository.findHighBalanceAccounts(minBalance);
    }

    @Transactional(readOnly = true)
    public Account getAccountWithDetails(Long id) {
        log.debug("Получение счета с деталями по ID: {}", id);
        return accountRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long id) {
        log.debug("Получение баланса счета ID: {}", id);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return account.getBalance();
    }

    @Transactional(readOnly = true)
    public boolean hasSufficientFunds(Long id, BigDecimal amount) {
        log.debug("Проверка средств на счете ID: {} для суммы: {}", id, amount);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        BigDecimal availableBalance = account.getBalance().add(account.getCreditLimit());
        return availableBalance.compareTo(amount) >= 0;
    }
}