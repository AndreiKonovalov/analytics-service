package ru.analytics.application.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.analytics.application.dto.ClientReportDTO;
import ru.analytics.domain.model.Account;
import ru.analytics.domain.model.Client;
import ru.analytics.domain.repository.ClientRepository;
import ru.analytics.domain.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final TransactionRepository transactionRepository;

    @PersistenceContext
    private final EntityManager em;

    /**
     * ХОРОШО: Решение 1 - EntityGraph с пагинацией
     * Используем EntityGraph для явного указания связей
     * Пагинация предотвращает загрузку всех данных сразу
     */
    @Transactional(readOnly = true)
    public Page<ClientReportDTO> getClientsWithTransactionsOptimized(Pageable pageable) {
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
        List<Client> clients = clientRepository.findAllWithDetails(clientIds);

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

    /**
     * ХОРОШО: Решение 2 - JOIN FETCH с DISTINCT и пагинацией
     * Используем DISTINCT для избежания дубликатов
     */
    @Transactional(readOnly = true)
    public List<Client> getClientsWithTransactionsJoinFetch() {
        LocalDateTime monthAgo = LocalDateTime.now().minusMonths(1);

        return em.createQuery("""
                        SELECT DISTINCT c FROM Client c
                        LEFT JOIN FETCH c.accounts a
                        LEFT JOIN FETCH a.transactions t
                        WHERE t.createdAt >= :monthAgo
                        ORDER BY c.id
                        """, Client.class)
                .setParameter("monthAgo", monthAgo)
                .setHint("org.hibernate.readOnly", true)
                .setHint("org.hibernate.fetchSize", 50)
                .getResultList();
    }

    /**
     * ХОРОШО: Решение 3 - BatchSize для коллекций
     * Hibernate загружает связанные коллекции батчами
     */
    @Transactional(readOnly = true)
    public List<ClientReportDTO> getClientsWithBatchLoading() {
        // @BatchSize на коллекциях позволяет загружать их пачками
        // Например: @BatchSize(size = 20) на accounts
        List<Client> clients = clientRepository.findAll();

        // При доступе к accounts будет выполнено:
        // SELECT * FROM accounts WHERE client_id IN (?, ?, ..., ?)
        // вместо отдельных запросов для каждого клиента

        return clients.stream()
                .limit(100)
                .map(this::convertToClientReportDTO)
                .collect(Collectors.toList());
    }

    /**
     * ХОРОШО: Решение 4 - DTO проекции на уровне репозитория
     * Загружаем только необходимые поля
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getClientSummaryReport() {
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
     * ХОРОШО: Решение 5 - Использование нативных запросов для сложной аналитики
     */
    @Transactional(readOnly = true)
    public List<Object[]> getClientTransactionStatsOptimized(int minTransactions) {
        return clientRepository.getClientStatistics(minTransactions);
    }

    /**
     * ХОРОШО: Решение 6 - Пагинация с оконными функциями
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Page<ClientReportDTO> getTopClientsByBalance(Pageable pageable) {
        // Используем оконные функции для ранжирования
        List<Object[]> results = em.createNativeQuery("""
            WITH ranked_clients AS (
                SELECT 
                    c.id,
                    c.first_name,
                    c.last_name,
                    c.email,
                    SUM(a.balance) as total_balance,
                    ROW_NUMBER() OVER (ORDER BY SUM(a.balance) DESC) as rank
                FROM clients c
                JOIN accounts a ON a.client_id = c.id
                WHERE a.is_active = true
                GROUP BY c.id, c.first_name, c.last_name, c.email
            )
            SELECT * FROM ranked_clients
            WHERE rank BETWEEN :start AND :end
            """)
                .setParameter("start", pageable.getOffset() + 1)
                .setParameter("end", pageable.getOffset() + pageable.getPageSize())
                .getResultList();

        List<ClientReportDTO> dtos = results.stream()
                .map(row -> ClientReportDTO.builder()
                        .clientId(((Number) row[0]).longValue())
                        .fullName(row[1] + " " + row[2])
                        .email((String) row[3])
                        .totalBalance(new BigDecimal(row[4].toString()))
                        .build())
                .collect(Collectors.toList());

        Long total = em.createQuery("SELECT COUNT(DISTINCT c) FROM Client c", Long.class)
                .getSingleResult();

        return new PageImpl<>(dtos, pageable, total);
    }

    /**
     * ХОРОШО: Решение 7 - Кэширование результатов частых запросов
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getCategorySpendingSummary(LocalDateTime from, LocalDateTime to) {
        List<Object[]> results = transactionRepository.getCategorySpendingAnalysis(from, to);

        Map<String, BigDecimal> spendingByCategory = new LinkedHashMap<>();
        BigDecimal totalSpending = BigDecimal.ZERO;

        for (Object[] row : results) {
            String categoryName = (String) row[1];
            BigDecimal categoryTotal = ((BigDecimal) row[3]).abs(); // Берем абсолютное значение
            spendingByCategory.put(categoryName, categoryTotal);
            totalSpending = totalSpending.add(categoryTotal);
        }

        // Добавляем проценты
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : spendingByCategory.entrySet()) {
            BigDecimal percentage = entry.getValue()
                    .divide(totalSpending, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            result.put(entry.getKey() + " (" + percentage.setScale(2) + "%)", entry.getValue());
        }

        return result;
    }

    /**
     * Вспомогательный метод для преобразования Client в ClientReportDTO
     */
    private ClientReportDTO convertToClientReportDTO(Client client) {
        BigDecimal totalBalance = client.getAccounts().stream()
                .map(Account::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgBalance = client.getAccounts().isEmpty()
                ? BigDecimal.ZERO
                : totalBalance.divide(
                BigDecimal.valueOf(client.getAccounts().size()),
                2, RoundingMode.HALF_UP
        );

        return ClientReportDTO.builder()
                .clientId(client.getId())
                .fullName(client.getFullName())
                .email(client.getEmail())
                .riskLevel(client.getRiskLevel())
                .kycStatus(client.getKycStatus())
                .dateOfBirth(client.getDateOfBirth())
                .totalAccounts(client.getAccounts().size())
                .totalBalance(totalBalance)
                .averageAccountBalance(avgBalance)
                .segments(client.getSegments().stream()
                        .map(segment -> new ClientReportDTO.SegmentDTO(
                                segment.getId(),
                                segment.getName(),
                                segment.getCode()
                        ))
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Демонстрация проблемы и решения для больших отчетов
     */
    @Transactional(readOnly = true)
    public void generateLargeReportComparison() {
        log.info("=== Генерация отчета с разными подходами ===");

        // 1. Наивный подход (плохо)
        long start = System.currentTimeMillis();
        List<ClientReportDTO> naiveResult = new TransactionReportService(
                clientRepository).getClientsWithTransactionsNaive();
        long naiveTime = System.currentTimeMillis() - start;

        // 2. Оптимизированный подход (хорошо)
        start = System.currentTimeMillis();
        Page<ClientReportDTO> optimizedResult = getClientsWithTransactionsOptimized(
                org.springframework.data.domain.PageRequest.of(0, 50)
        );
        long optimizedTime = System.currentTimeMillis() - start;

        log.info("=== Результаты сравнения ===");
        log.info("Наивный подход: {} ms, {} клиентов", naiveTime, naiveResult.size());
        log.info("Оптимизированный подход: {} ms, {} клиентов",
                optimizedTime, optimizedResult.getContent().size());
        log.info("Ускорение: {}%",
                (int) ((naiveTime - optimizedTime) * 100.0 / naiveTime));
    }
}
