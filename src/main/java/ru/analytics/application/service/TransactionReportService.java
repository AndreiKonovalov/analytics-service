package ru.analytics.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.analytics.application.dto.AccountReport;
import ru.analytics.application.dto.ClientReportDTO;
import ru.analytics.domain.model.Account;
import ru.analytics.domain.model.Client;
import ru.analytics.domain.model.Transaction;
import ru.analytics.domain.repository.ClientRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Пример НЕОПТИМИЗИРОВАННОГО сервиса с проблемой N+1
 * Плохая практика - демонстрирует типичные ошибки
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionReportService {

    private final ClientRepository clientRepository;

    /**
     * ПЛОХО: Метод с проблемой N+1 запросов
     * Каждая итерация вызывает отдельные SQL запросы
     * <p>
     * Проблемы:
     * 1. SELECT clients (1 запрос)
     * 2. Для каждого клиента: SELECT accounts (N запросов)
     * 3. Для каждого аккаунта: SELECT transactions (M запросов)
     * 4. Для каждой транзакции: SELECT category (K запросов)
     * <p>
     * Итого: 1 + N + N*M + N*M*K запросов
     * Отключаем batch fetching
     */
    @Transactional(readOnly = true)
    public List<ClientReportDTO> getClientsWithTransactionsNaive() {
        long startTime = System.currentTimeMillis();

        // Загружаем клиентов
        List<Client> clients = clientRepository.findAll();
        log.info("Загружено {} клиентов", clients.size());

        List<ClientReportDTO> result = new ArrayList<>();

        // Используем обычный цикл (не stream), чтобы точно вызывать lazy load
        for (Client client : clients) {

            ClientReportDTO dto = ClientReportDTO.builder()
                    .clientId(client.getId())
                    .fullName(client.getFullName())
                    .email(client.getEmail())
                    .riskLevel(client.getRiskLevel())
                    .kycStatus(client.getKycStatus())
                    .dateOfBirth(client.getDateOfBirth())
                    .build();

            // === N+1: accounts (1 запрос на клиента) ===
            List<AccountReport> accountReports = new ArrayList<>();

            for (Account account : client.getAccounts()) { // ← LAZY LOAD: отдельный запрос!
                AccountReport accountDto = AccountReport.builder()
                        .accountId(account.getId())
                        .accountNumber(account.getAccountNumber())
                        .accountType(account.getType() != null ? account.getType().name() : null)
                        .currencyCode(account.getCurrencyCode())
                        .balance(account.getBalance())
                        .creditLimit(account.getCreditLimit())
                        .isActive(account.isActive())
                        .createdAt(account.getCreatedAt())
                        .build();

                // === N+1: transactions (1 запрос на счёт) ===
                List<AccountReport.TransactionSummary> txSummaries = new ArrayList<>();
                BigDecimal monthlyInflow = BigDecimal.ZERO;
                BigDecimal monthlyOutflow = BigDecimal.ZERO;
                int txCount = 0;

                for (Transaction tx : account.getTransactions()) { // ← LAZY LOAD: отдельный запрос!
                    txSummaries.add(AccountReport.TransactionSummary.builder()
                            .date(tx.getCreatedAt())
                            .amount(tx.getAmount())
                            .type(tx.getType() != null ? tx.getType().name() : null)
                            .description(tx.getDescription())
                            .counterpartyName(tx.getCounterpartyName())
                            .suspicious(tx.isSuspicious())
                            .build());

                    // Считаем статистику
                    if (tx.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                        monthlyInflow = monthlyInflow.add(tx.getAmount());
                    } else {
                        monthlyOutflow = monthlyOutflow.add(tx.getAmount().abs());
                    }
                    txCount++;

                    // === N+1: category (1 запрос на транзакцию!) ===
                    String categoryName = "Без категории";
                    if (tx.getCategory() != null) {
                        categoryName = tx.getCategory().getName(); // ← LAZY LOAD: отдельный запрос!
                    }
                }

                accountDto.setMonthlyInflow(monthlyInflow);
                accountDto.setMonthlyOutflow(monthlyOutflow);
                accountDto.setMonthlyTransactionCount(txCount);
                accountDto.setRecentTransactions(txSummaries.subList(0, Math.min(5, txSummaries.size())));

                accountReports.add(accountDto);
            }

            dto.setAccounts(accountReports);
            dto.setTotalAccounts(accountReports.size());

            // Расчёт totalBalance
            BigDecimal totalBalance = accountReports.stream()
                    .map(AccountReport::getBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setTotalBalance(totalBalance);
            dto.setAverageAccountBalance(
                    accountReports.isEmpty() ? null :
                            totalBalance.divide(BigDecimal.valueOf(accountReports.size()), 2, RoundingMode.HALF_UP)
            );

            // Последняя транзакция
            LocalDateTime lastTx = accountReports.stream()
                    .flatMap(acc -> acc.getRecentTransactions().stream())
                    .map(AccountReport.TransactionSummary::getDate)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            dto.setLastTransactionDate(lastTx);

            // Segments (ещё один N+1!)
            List<ClientReportDTO.SegmentDTO> segments = client.getSegments().stream() // ← LAZY LOAD!
                    .map(seg -> ClientReportDTO.SegmentDTO.builder()
                            .id(seg.getId())
                            .name(seg.getName())
                            .code(seg.getCode())
                            .build())
                    .collect(Collectors.toList());
            dto.setSegments(segments);

            result.add(dto);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.warn("TRUE N+1 method executed in {} ms with {} clients", duration, result.size());
        return result;
    }

    /**
     * ПЛОХО: Еще один пример проблемы N+1
     * JOIN FETCH без DISTINCT приводит к дублированию данных
     */
    @Transactional(readOnly = true)
    public List<Client> getClientsWithDetailsBad() {
        // Проблема: JOIN FETCH без пагинации на больших данных
        return clientRepository.findVerifiedClientsWithDetails();
    }

    /**
     * ПЛОХО: Использование EAGER fetching в сущностях
     * Приводит к избыточной загрузке данных
     */
    @Transactional(readOnly = true)
    public List<Client> getClientsWithEagerLoading() {
        // Если в сущности Client указано fetch = FetchType.EAGER для accounts,
        // то при загрузке клиентов всегда будут загружаться все аккаунты
        return clientRepository.findAll();
    }
}
