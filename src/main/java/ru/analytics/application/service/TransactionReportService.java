package ru.analytics.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.analytics.application.dto.ClientReportDTO;
import ru.analytics.domain.model.Client;
import ru.analytics.domain.repository.ClientRepository;

import java.math.BigDecimal;
import java.util.List;
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
     */
    @Transactional(readOnly = true)
    public List<ClientReportDTO> getClientsWithTransactionsNaive() {
        long startTime = System.currentTimeMillis();

        // 1 запрос: получить всех клиентов
        List<Client> clients = clientRepository.findAll();
        log.info("Found {} clients", clients.size());

        List<ClientReportDTO> result = clients.stream()
                .limit(50) // Ограничиваем для демонстрации
                .map(client -> {
                    ClientReportDTO dto = new ClientReportDTO();
                    dto.setClientId(client.getId());
                    dto.setFullName(client.getFullName());
                    dto.setEmail(client.getEmail());
                    dto.setRiskLevel(client.getRiskLevel());

                    // ПРОБЛЕМА: для каждого клиента делается отдельный запрос accounts
                    // Если у клиента 3 аккаунта, это 3 отдельных запроса
                    dto.setTotalAccounts(client.getAccounts().size());

                    // ПРОБЛЕМА: расчет баланса вызывает дополнительные запросы
                    BigDecimal totalBalance = client.getAccounts().stream()
                            .map(account -> {
                                // ПРОБЛЕМА: каждая транзакция загружается отдельно
                                // Плюс category для каждой транзакции
                                account.getTransactions().forEach(transaction -> {
                                    // Доступ к category вызывает отдельный запрос
                                    if (transaction.getCategory() != null) {
                                        transaction.getCategory().getName();
                                    }
                                });
                                return account.getBalance();
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    dto.setTotalBalance(totalBalance);
                    return dto;
                })
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.warn("Naive method executed in {} ms with {} clients", duration, result.size());

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
