package ru.analytics.application.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ОПТИМИЗИРОВАННЫЙ сервис с решениями проблем производительности
 * Хорошая практика - демонстрирует правильные подходы
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizedReportService {

    private final ClientRepository clientRepository;
    @PersistenceContext
    private final EntityManager em;

    /**
     * ХОРОШО: Решение 1 - EntityGraph с пагинацией
     * Используем EntityGraph для явного указания связей
     * Пагинация предотвращает загрузку всех данных сразу
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analytics.clients.optimized", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    public Page<ClientReportDTO> getClientsWithTransactionsOptimized(Pageable pageable) {
        log.info("Cache miss for optimized clients report: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        long startTime = System.currentTimeMillis();

        // Используем нативный запрос для получения ID клиентов с пагинацией
        List<Long> clientIds = em.createQuery(
                        "SELECT c.id FROM Client c ORDER BY c.id", Long.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        if (clientIds.isEmpty()) {
            return Page.empty();
        }

        // ХОРОШО: EntityGraph с указанием только нужных связей
        List<Client> clients = clientRepository.findAllWithDetailsEntityGraph(clientIds);

        // Преобразование в DTO с использованием Stream API
        List<ClientReportDTO> dtos = clients.stream()
                .map(this::convertToClientReportDTO)
                .collect(Collectors.toList());

        // Получаем общее количество для пагинации
        Long total = em.createQuery("SELECT COUNT(c) FROM Client c", Long.class)
                .getSingleResult();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Optimized method executed in {} ms for {} clients",
                duration, dtos.size());

        return new PageImpl<>(dtos, pageable, total);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    @Cacheable(cacheNames = "analytics.clients.summary")
    public List<Map<String, Object>> getClientSummaryProjection() {
        log.info("Cache miss for client summary projection");
        LocalDateTime startDate = LocalDateTime.now().minusMonths(1);
        LocalDateTime endDate = LocalDateTime.now();

        // Используем DTO проекции из репозитория
        List<Map> results = em.createQuery("""
                        SELECT new map(
                            c.id as clientId,
                            c.firstName as firstName,
                            c.lastName as lastName,
                            COUNT(a) as accountCount,
                            SUM(a.balance) as totalBalance,
                            COUNT(t) as transactionCount
                        )
                        FROM Client c
                        LEFT JOIN c.accounts a
                        LEFT JOIN a.transactions t ON t.createdAt BETWEEN :startDate AND :endDate
                        GROUP BY c.id, c.firstName, c.lastName
                        HAVING COUNT(t) > 0
                        ORDER BY totalBalance DESC
                        """, Map.class)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setMaxResults(100)
                .getResultList();

        // Безопасное преобразование
        List<Map<String, Object>> typedResults = new ArrayList<>();
        for (Map map : results) {
            typedResults.add(new HashMap<>(map));
        }
        return typedResults;
    }

    /**
     * Вспомогательный метод для преобразования Client в ClientReportDTO
     */
    private ClientReportDTO convertToClientReportDTO(Client client) {
        List<Account> accounts = new ArrayList<>(client.getAccounts());

        // Вычисляем балансы на уровне сущностей Account
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageBalance = accounts.isEmpty()
                ? null
                : totalBalance.divide(
                BigDecimal.valueOf(accounts.size()),
                2,
                RoundingMode.HALF_UP
        );

        List<AccountReport> accountReports = client.getAccounts().stream()
                .map(this::convertToAccountReport)
                .collect(Collectors.toList());

        LocalDateTime lastTxDate = client.getAccounts().stream()
                .flatMap(acc -> acc.getTransactions().stream())
                .map(Transaction::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return ClientReportDTO.builder()
                .clientId(client.getId())
                .fullName(client.getFullName())
                .email(client.getEmail())
                .riskLevel(client.getRiskLevel())
                .kycStatus(client.getKycStatus())
                .dateOfBirth(client.getDateOfBirth())
                .totalAccounts(accountReports.size())
                .totalBalance(totalBalance)
                .averageAccountBalance(averageBalance)
                .lastTransactionDate(lastTxDate)
                .accounts(accountReports) // ← Теперь заполняется!
                .segments(client.getSegments().stream()
                        .map(seg -> ClientReportDTO.SegmentDTO.builder()
                                .id(seg.getId())
                                .name(seg.getName())
                                .code(seg.getCode())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private AccountReport convertToAccountReport(Account account) {
        List<AccountReport.TransactionSummary> recentTxs = account.getTransactions().stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .limit(5)
                .map(tx -> AccountReport.TransactionSummary.builder()
                        .date(tx.getCreatedAt())
                        .amount(tx.getAmount())
                        .type(tx.getType() != null ? tx.getType().name() : null)
                        .description(tx.getDescription())
                        .counterpartyName(tx.getCounterpartyName())
                        .suspicious(tx.isSuspicious())
                        .build())
                .collect(Collectors.toList());

        return AccountReport.builder()
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getType() != null ? account.getType().name() : null)
                .currencyCode(account.getCurrencyCode())
                .balance(account.getBalance())
                .creditLimit(account.getCreditLimit())
                .isActive(account.isActive())
                .createdAt(account.getCreatedAt())
                .monthlyTransactionCount(account.getTransactions().size())
                .recentTransactions(recentTxs)
                .build();
    }

}
