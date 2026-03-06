package ru.analytics.infrastructure.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.analytics.domain.model.Account;
import ru.analytics.domain.model.Category;
import ru.analytics.domain.model.Client;
import ru.analytics.domain.model.Segment;
import ru.analytics.domain.model.Tag;
import ru.analytics.domain.model.Transaction;
import ru.analytics.domain.model.enums.AccountType;
import ru.analytics.domain.model.enums.TransactionType;
import ru.analytics.domain.repository.AccountRepository;
import ru.analytics.domain.repository.CategoryRepository;
import ru.analytics.domain.repository.ClientRepository;
import ru.analytics.domain.repository.SegmentRepository;
import ru.analytics.domain.repository.TagRepository;
import ru.analytics.domain.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final SegmentRepository segmentRepository;

    private static final String[] FIRST_NAMES = {
            "Иван", "Александр", "Сергей", "Дмитрий", "Алексей",
            "Андрей", "Михаил", "Евгений", "Владимир", "Павел",
            "Ольга", "Елена", "Наталья", "Анна", "Мария",
            "Ирина", "Светлана", "Татьяна", "Екатерина", "Юлия"
    };

    private static final String[] LAST_NAMES = {
            "Иванов", "Петров", "Сидоров", "Смирнов", "Кузнецов",
            "Попов", "Васильев", "Соколов", "Михайлов", "Новиков",
            "Федоров", "Морозов", "Волков", "Алексеев", "Лебедев",
            "Семенов", "Егоров", "Павлов", "Козлов", "Степанов"
    };

    private static final String[] CATEGORIES = {
            "Продукты", "Рестораны", "Транспорт", "Развлечения", "Одежда",
            "Здоровье", "Образование", "Жилье", "Коммунальные услуги", "Связь",
            "Путешествия", "Автомобиль", "Подарки", "Спорт", "Красота"
    };

    private static final String[] TAG_NAMES = {
            "подозрительная", "крупная сумма", "регулярная", "срочная",
            "международная", "онлайн", "оффлайн", "автоплатеж",
            "возврат", "кэшбэк", "комиссия", "перевод"
    };

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Начало заполнения базы данных тестовыми данными...");

        long startTime = System.currentTimeMillis();

        // Очищаем данные в правильном порядке (из-за foreign keys)
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        clientRepository.deleteAll();
        categoryRepository.deleteAll();
        tagRepository.deleteAll();
        segmentRepository.deleteAll();

        // 1. Создаем сегменты
        List<Segment> segments = createSegments();
        log.info("Создано {} сегментов", segments.size());

        // 2. Создаем категории
        List<Category> categories = createCategories();
        log.info("Создано {} категорий", categories.size());

        // 3. Создаем теги
        List<Tag> tags = createTags();
        log.info("Создано {} тегов", tags.size());

        // 4. Создаем клиентов
        List<Client> clients = createClients(50);
        log.info("Создано {} клиентов", clients.size());

        // 5. Назначаем клиентам сегменты
        assignSegmentsToClients(clients, segments);

        // 6. Создаем счета для клиентов
        List<Account> accounts = createAccountsForClients(clients);
        log.info("Создано {} счетов", accounts.size());

        // 7. Создаем транзакции для счетов
        List<Transaction> transactions = createTransactionsForAccounts(accounts, categories, tags);
        log.info("Создано {} транзакций", transactions.size());

        // 8. Обновляем балансы счетов на основе транзакций
        updateAccountBalances(accounts);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Заполнение базы данных завершено за {} мс", duration);
        log.info("Статистика:");
        log.info("  - Клиентов: {}", clientRepository.count());
        log.info("  - Счетов: {}", accountRepository.count());
        log.info("  - Транзакций: {}", transactionRepository.count());
        log.info("  - Категорий: {}", categoryRepository.count());
        log.info("  - Тегов: {}", tagRepository.count());
        log.info("  - Сегментов: {}", segmentRepository.count());
    }

    private List<Segment> createSegments() {
        List<Segment> segments = Arrays.asList(
                Segment.builder()
                        .name("Стандартный")
                        .code("STANDARD")
                        .description("Базовый сегмент для новых клиентов")
                        .minimumBalance(BigDecimal.ZERO)
                        .minimumMonthlyTurnover(BigDecimal.ZERO)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Segment.builder()
                        .name("Премиум")
                        .code("PREMIUM")
                        .description("Для клиентов с высоким балансом")
                        .minimumBalance(new BigDecimal("100000"))
                        .minimumMonthlyTurnover(new BigDecimal("500000"))
                        .createdAt(LocalDateTime.now())
                        .build(),
                Segment.builder()
                        .name("VIP")
                        .code("VIP")
                        .description("Эксклюзивный сегмент")
                        .minimumBalance(new BigDecimal("1000000"))
                        .minimumMonthlyTurnover(new BigDecimal("5000000"))
                        .createdAt(LocalDateTime.now())
                        .build(),
                Segment.builder()
                        .name("Студенческий")
                        .code("STUDENT")
                        .description("Специальные условия для студентов")
                        .minimumBalance(BigDecimal.ZERO)
                        .minimumMonthlyTurnover(BigDecimal.ZERO)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Segment.builder()
                        .name("Пенсионный")
                        .code("PENSIONER")
                        .description("Льготные условия для пенсионеров")
                        .minimumBalance(BigDecimal.ZERO)
                        .minimumMonthlyTurnover(BigDecimal.ZERO)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        return segmentRepository.saveAll(segments);
    }

    private List<Category> createCategories() {
        List<Category> categories = new ArrayList<>();

        for (int i = 0; i < CATEGORIES.length; i++) {
            String name = CATEGORIES[i];
            Category category = Category.builder()
                    .name(name)
                    .code(name.toUpperCase().replace(" ", "_"))
                    .description("Транзакции в категории " + name)
                    .transactionType(i < 7 ? "DEBIT" : "CREDIT") // Первые 7 - расходы, остальные - доходы
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            categories.add(category);
        }

        // Добавляем специальные категории
        categories.add(Category.builder()
                .name("Зарплата")
                .code("SALARY")
                .description("Заработная плата")
                .transactionType("CREDIT")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        categories.add(Category.builder()
                .name("Перевод")
                .code("TRANSFER")
                .description("Денежные переводы")
                .transactionType("BOTH")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        return categoryRepository.saveAll(categories);
    }

    private List<Tag> createTags() {
        List<Tag> tags = new ArrayList<>();
        String[] colors = {"#FF6B6B", "#FFD166", "#06D6A0", "#118AB2", "#073B4C",
                "#7209B7", "#F72585", "#4CC9F0", "#FF9E00", "#36B37E"};

        for (int i = 0; i < TAG_NAMES.length; i++) {
            Tag tag = Tag.builder()
                    .name(TAG_NAMES[i])
                    .color(colors[i % colors.length])
                    .createdAt(LocalDateTime.now())
                    .build();
            tags.add(tag);
        }

        return tagRepository.saveAll(tags);
    }

    private List<Client> createClients(int count) {
        List<Client> clients = new ArrayList<>();
        Random random = new Random();

        String[] riskLevels = {"LOW", "MEDIUM", "HIGH"};
        String[] kycStatuses = {"PENDING", "VERIFIED", "REJECTED"};

        for (int i = 1; i <= count; i++) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@example.com";

            Client client = Client.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phoneNumber("+7" + (9000000000L + i))
                    .taxIdentificationNumber("77" + String.format("%012d", i))
                    .dateOfBirth(LocalDate.now().minusYears(20 + random.nextInt(40)).minusDays(random.nextInt(365)))
                    .riskLevel(riskLevels[random.nextInt(riskLevels.length)])
                    .kycStatus(kycStatuses[random.nextInt(kycStatuses.length)])
                    .createdAt(LocalDateTime.now().minusDays(random.nextInt(365)))
                    .updatedAt(LocalDateTime.now())
                    .build();

            clients.add(client);
        }

        return clientRepository.saveAll(clients);
    }

    private void assignSegmentsToClients(List<Client> clients, List<Segment> segments) {
        Random random = new Random();

        for (Client client : clients) {
            // Каждый клиент получает 1-3 случайных сегмента
            int segmentCount = 1 + random.nextInt(3);
            Set<Segment> clientSegments = new HashSet<>();

            for (int i = 0; i < segmentCount; i++) {
                Segment segment = segments.get(random.nextInt(segments.size()));
                clientSegments.add(segment);
            }

            client.setSegments(clientSegments);
        }

        clientRepository.saveAll(clients);
    }

    private List<Account> createAccountsForClients(List<Client> clients) {
        List<Account> accounts = new ArrayList<>();
        Random random = new Random();
        AccountType[] accountTypes = AccountType.values();
        String[] currencies = {"RUB", "USD", "EUR"};

        for (Client client : clients) {
            // Каждый клиент получает 1-3 счета
            int accountCount = 1 + random.nextInt(3);

            for (int i = 0; i < accountCount; i++) {
                Account account = Account.builder()
                        .accountNumber("RU02" + String.format("%018d", (client.getId() * 100 + i)))
                        .balance(BigDecimal.ZERO) // Будем обновлять после создания транзакций
                        .creditLimit(random.nextBoolean() ?
                                new BigDecimal("10000").multiply(new BigDecimal(random.nextInt(10))) :
                                BigDecimal.ZERO)
                        .type(accountTypes[random.nextInt(accountTypes.length)])
                        .currencyCode(currencies[random.nextInt(currencies.length)])
                        .isActive(random.nextDouble() > 0.1) // 90% активных
                        .client(client)
                        .createdAt(LocalDateTime.now().minusMonths(random.nextInt(12)))
                        .updatedAt(LocalDateTime.now())
                        .build();

                accounts.add(account);
            }
        }

        return accountRepository.saveAll(accounts);
    }

    private List<Transaction> createTransactionsForAccounts(
            List<Account> accounts,
            List<Category> categories,
            List<Tag> tags) {

        List<Transaction> transactions = new ArrayList<>();
        Random random = new Random();

        // Создаем разное количество транзакций для разных счетов
        for (Account account : accounts) {
            // От 10 до 100 транзакций на счет
            int transactionCount = 10 + random.nextInt(91);
            LocalDateTime accountCreated = account.getCreatedAt();

            for (int i = 0; i < transactionCount; i++) {
                // Случайная дата от создания счета до сейчас
                long daysBetween = ChronoUnit.DAYS.between(accountCreated.toLocalDate(), LocalDate.now());
                LocalDateTime transactionDate = accountCreated.plusDays(random.nextInt((int) daysBetween + 1));

                // Случайный тип транзакции
                TransactionType[] types = TransactionType.values();
                TransactionType type = types[random.nextInt(types.length)];

                // Случайная сумма в зависимости от типа
                BigDecimal amount = generateAmountByType(type, random);

                // Случайная категория
                Category category = categories.get(random.nextInt(categories.size()));

                // Создаем транзакцию
                Transaction transaction = Transaction.builder()
                        .account(account)
                        .client(account.getClient())
                        .amount(amount)
                        .currencyCode(account.getCurrencyCode())
                        .type(type)
                        .category(category)
                        .description(generateDescription(type, category, amount))
                        .counterpartyAccount(random.nextBoolean() ?
                                "RU02" + String.format("%018d", random.nextLong(1000000000000000000L)) :
                                null)
                        .counterpartyName(random.nextBoolean() ? generateCounterpartyName(random) : null)
                        .status("COMPLETED")
                        .isSuspicious(random.nextDouble() < 0.05) // 5% подозрительных
                        .suspicionReason(random.nextBoolean() ? "Необычно крупная сумма" : null)
                        .createdAt(transactionDate)
                        .executedAt(transactionDate.plusMinutes(random.nextInt(60)))
                        .updatedAt(LocalDateTime.now())
                        .build();

                // Добавляем случайные теги (0-3 тега на транзакцию)
                int tagCount = random.nextInt(4);
                Set<Tag> transactionTags = new HashSet<>();
                for (int j = 0; j < tagCount; j++) {
                    transactionTags.add(tags.get(random.nextInt(tags.size())));
                }
                transaction.setTags(transactionTags);

                transactions.add(transaction);

                // Сохраняем пачками для производительности
                if (transactions.size() % 1000 == 0) {
                    transactionRepository.saveAll(transactions);
                    transactions.clear();
                    log.info("Создано {} транзакций...", i);
                }
            }
        }

        // Сохраняем оставшиеся транзакции
        if (!transactions.isEmpty()) {
            transactionRepository.saveAll(transactions);
        }

        return transactionRepository.findAll();
    }

    private BigDecimal generateAmountByType(TransactionType type, Random random) {
        return switch (type) {
            case DEPOSIT, SALARY, LOAN_DISBURSEMENT -> new BigDecimal(random.nextInt(50000) + 10000); // 10000-60000
            case WITHDRAWAL, PAYMENT, CARD_PURCHASE, LOAN_REPAYMENT ->
                    new BigDecimal(random.nextInt(10000) + 100).negate(); // -100 - -10100
            case TRANSFER, EXCHANGE -> new BigDecimal(random.nextInt(20000) + 1000)
                    .multiply(BigDecimal.valueOf(random.nextBoolean() ? 1 : -1));
            case CASHBACK, REFUND, INTEREST, DIVIDEND -> new BigDecimal(random.nextInt(5000) + 100); // 100-5100
            case FEE -> new BigDecimal(random.nextInt(500) + 10).negate(); // -10 - -510
            case ATM_WITHDRAWAL -> new BigDecimal(random.nextInt(20000) + 100).negate(); // -100 - -20100
        };
    }

    private String generateDescription(TransactionType type, Category category, BigDecimal amount) {
        return switch (type) {
            case DEPOSIT -> "Внесение наличных";
            case WITHDRAWAL -> "Снятие наличных";
            case TRANSFER -> amount.compareTo(BigDecimal.ZERO) > 0 ?
                    "Поступление перевода" : "Исходящий перевод";
            case PAYMENT -> "Оплата: " + category.getName();
            case CARD_PURCHASE -> "Оплата картой: " + category.getName();
            case ATM_WITHDRAWAL -> "Снятие в банкомате";
            case SALARY -> "Заработная плата";
            case CASHBACK -> "Кэшбэк за покупки";
            case REFUND -> "Возврат средств";
            case FEE -> "Банковская комиссия";
            case INTEREST -> "Проценты по вкладу";
            case DIVIDEND -> "Дивиденды";
            case LOAN_DISBURSEMENT -> "Выдача кредита";
            case LOAN_REPAYMENT -> "Погашение кредита";
            case EXCHANGE -> "Обмен валюты";
        };
    }

    private String generateCounterpartyName(Random random) {
        String[] companies = {
                "ООО 'Рога и копыта'", "АО 'Газпром'", "ПАО 'Сбербанк'",
                "ИП Иванов И.И.", "ООО 'Яндекс'", "АО 'ВТБ'",
                "ПАО 'Магнит'", "ООО 'Лукойл'", "ИП Петрова М.С.",
                "ООО 'М.Видео'", "ПАО 'МТС'", "АО 'Мегафон'"
        };
        return companies[random.nextInt(companies.length)];
    }

    private void updateAccountBalances(List<Account> accounts) {
        log.info("Обновление балансов счетов...");

        for (Account account : accounts) {
            List<Transaction> accountTransactions = transactionRepository.findByAccountId(account.getId());

            BigDecimal balance = accountTransactions.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            account.setBalance(balance);
            account.setUpdatedAt(LocalDateTime.now());
        }

        accountRepository.saveAll(accounts);
        log.info("Балансы счетов обновлены");
    }
}
