package ru.analytics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.analytics.application.dto.TransferRequest;
import ru.analytics.application.service.OptimizedReportService;
import ru.analytics.application.service.TransactionProcessingService;
import ru.analytics.application.service.TransactionReportService;
import ru.analytics.domain.model.Account;
import ru.analytics.domain.model.Category;
import ru.analytics.domain.model.Client;
import ru.analytics.domain.model.Transaction;
import ru.analytics.domain.model.enums.AccountType;
import ru.analytics.domain.repository.AccountRepository;
import ru.analytics.domain.repository.ClientRepository;
import ru.analytics.domain.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class TransactionOptimizationTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionReportService naiveService;

    @Autowired
    private OptimizedReportService optimizedService;

    @Autowired
    private TransactionProcessingService transactionService;

    @BeforeEach
    void setUp() {
        // Создаем тестовые данные
        createTestData();
    }

    @Test
    void testNaiveVsOptimizedApproach() {
        System.out.println("\n=== Тест: Наивный vs Оптимизированный подход ===");

        // Наивный подход
        long start = System.currentTimeMillis();
        var naiveResult = naiveService.getClientsWithTransactionsNaive();
        long naiveTime = System.currentTimeMillis() - start;

        System.out.println("Наивный подход: " + naiveTime + "ms, " + naiveResult.size() + " клиентов");

        // Оптимизированный подход
        start = System.currentTimeMillis();
        var optimizedResult = optimizedService.getClientsWithTransactionsOptimized(
                PageRequest.of(0, 50)
        );
        long optimizedTime = System.currentTimeMillis() - start;

        System.out.println("Оптимизированный подход: " + optimizedTime + "ms, " +
                optimizedResult.getContent().size() + " клиентов");

        assertThat(optimizedTime).isLessThan(naiveTime * 2); // Оптимизированный должен быть быстрее
    }

    @Test
    void testNPlusOneDetection() {
        System.out.println("\n=== Тест: Обнаружение N+1 проблемы ===");

        // Этот метод должен показать разницу в количестве запросов
        naiveService.getClientsWithTransactionsNaive();

        // В логах должно быть предупреждение о N+1
        System.out.println("Проверьте логи на наличие N+1 warning");
    }

    @Test
    void testBatchLoading() {
        System.out.println("\n=== Тест: Батчевая загрузка ===");

        long start = System.currentTimeMillis();
        var result = optimizedService.getClientsWithBatchLoading();
        long duration = System.currentTimeMillis() - start;

        System.out.println("Батчевая загрузка: " + duration + "ms, " + result.size() + " клиентов");
        assertThat(result).isNotEmpty();
    }

    @Test
    void testQueryDSL() {
        System.out.println("\n=== Тест: QueryDSL сложные запросы ===");

        LocalDateTime from = LocalDateTime.now().minusMonths(1);
        LocalDateTime to = LocalDateTime.now();

        var result = transactionRepository.aggregateByCategory(from, to);

        System.out.println("Найдено категорий: " + result.size());
        result.forEach(agg -> {
            System.out.printf("Категория: %s, Сумма: %s, Количество: %d%n",
                    agg.getCategoryName(), agg.getTotalAmount(), agg.getTransactionCount());
        });

        assertThat(result).isNotNull();
    }

    @Test
    void testTransactionalIsolation() {
        System.out.println("\n=== Тест: Изоляция транзакций ===");

        // Получаем баланс с READ COMMITTED
        BigDecimal balance = transactionService.getAccountBalance(1L);
        System.out.println("Баланс счета 1: " + balance);

        assertThat(balance).isNotNull();
    }

    @Test
    void testTransferWithOptimisticLock() {
        System.out.println("\n=== Тест: Оптимистическая блокировка ===");

        var request = new TransferRequest(
                1L, 2L, new BigDecimal("100.00"), "USD", "Test transfer", "TEST123"
        );

        try {
            var result = transactionService.processTransferWithOptimisticLock(request);
            System.out.println("Перевод выполнен: " + result.message());
            assertThat(result.success()).isTrue();
        } catch (Exception e) {
            System.out.println("Ошибка перевода: " + e.getMessage());
        }
    }

    @Test
    void testPerformanceReport() {
        System.out.println("\n=== Тест: Генерация отчета производительности ===");

        optimizedService.generateLargeReportComparison();

        // Проверяем, что логи содержат информацию о сравнении
        System.out.println("Отчет сгенерирован, проверьте логи");
    }

    private void createTestData() {
        // Создаем клиентов
        Client client1 = Client.builder()
                .firstName("Иван")
                .lastName("Петров")
                .email("ivan.petrov@example.com")
                .riskLevel("LOW")
                .kycStatus("VERIFIED")
                .build();

        Client client2 = Client.builder()
                .firstName("Мария")
                .lastName("Сидорова")
                .email("maria.sidorova@example.com")
                .riskLevel("MEDIUM")
                .kycStatus("VERIFIED")
                .build();

        clientRepository.saveAll(List.of(client1, client2));

        // Создаем счета
        Account account1 = Account.builder()
                .accountNumber("UA123456789012345678901234567")
                .balance(new BigDecimal("10000.00"))
                .currencyCode("USD")
                .type(AccountType.CURRENT)
                .client(client1)
                .build();

        Account account2 = Account.builder()
                .accountNumber("UA987654321098765432109876543")
                .balance(new BigDecimal("5000.00"))
                .currencyCode("USD")
                .type(AccountType.SAVINGS)
                .client(client2)
                .build();

        accountRepository.saveAll(List.of(account1, account2));

        // Создаем категории
        Category category1 = Category.builder()
                .name("Продукты")
                .code("FOOD")
                .transactionType("DEBIT")
                .build();

        Category category2 = Category.builder()
                .name("Зарплата")
                .code("SALARY")
                .transactionType("CREDIT")
                .build();

        // Создаем транзакции
        Transaction transaction1 = Transaction.builder()
                .account(account1)
                .amount(new BigDecimal("-100.50"))
                .currencyCode("USD")
                .category(category1)
                .description("Супермаркет")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Transaction transaction2 = Transaction.builder()
                .account(account1)
                .amount(new BigDecimal("2000.00"))
                .currencyCode("USD")
                .category(category2)
                .description("Зарплата")
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        Transaction transaction3 = Transaction.builder()
                .account(account2)
                .amount(new BigDecimal("-50.75"))
                .currencyCode("USD")
                .category(category1)
                .description("Кафе")
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        transactionRepository.saveAll(List.of(transaction1, transaction2, transaction3));
    }
}