package ru.analytics.infrastructure.web.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.analytics.application.dto.AccountSummaryDTO;
import ru.analytics.application.dto.ClientDetailsDTO;
import ru.analytics.application.dto.ClientResponseDTO;
import ru.analytics.domain.model.Account;
import ru.analytics.domain.model.Client;
import ru.analytics.domain.model.Segment;
import ru.analytics.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    // Базовый маппинг Client -> ClientResponseDTO
    @Mapping(target = "fullName", ignore = true)
    ClientResponseDTO toResponseDTO(Client client);

    List<ClientResponseDTO> toResponseDTOList(List<Client> clients);

    // Детальный маппинг Client -> ClientDetailsDTO
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "accountCount", ignore = true)
    @Mapping(target = "totalBalance", ignore = true)
    @Mapping(target = "transactionCount", ignore = true)
    @Mapping(target = "segmentNames", ignore = true)
    @Mapping(target = "accounts", ignore = true)
    @Mapping(target = "lastTransactionDate", ignore = true)
    ClientDetailsDTO toDetailsDTO(Client client);

    List<ClientDetailsDTO> toDetailsDTOList(List<Client> clients);

    // Маппинг Account -> AccountSummaryDTO
    @Mapping(target = "type", source = "type", qualifiedByName = "mapAccountType")
    @Mapping(target = "transactionCount", ignore = true)
    AccountSummaryDTO toAccountSummaryDTO(Account account);

    List<AccountSummaryDTO> toAccountSummaryDTOList(List<Account> accounts);

    // Методы для преобразования Enum в String
    @Named("mapAccountType")
    default String mapAccountType(ru.analytics.domain.model.enums.AccountType type) {
        return type != null ? type.name() : null;
    }

    // Методы обогащения DTO после основного маппинга
    @AfterMapping
    default void enrichResponseDTO(@MappingTarget ClientResponseDTO dto, Client client) {
        dto.setFullName(client.getFullName());
    }

    @AfterMapping
    default void enrichDetailsDTO(@MappingTarget ClientDetailsDTO dto, Client client) {
        // Полное имя
        dto.setFullName(client.getFullName());

        // Количество счетов
        dto.setAccountCount(client.getAccounts() != null ? client.getAccounts().size() : 0);

        // Общий баланс
        BigDecimal totalBalance = BigDecimal.ZERO;
        if (client.getAccounts() != null && !client.getAccounts().isEmpty()) {
            totalBalance = client.getAccounts().stream()
                    .map(Account::getBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        dto.setTotalBalance(totalBalance);

        // Общее количество транзакций
        long transactionCount = 0L;
        if (client.getAccounts() != null) {
            transactionCount = client.getAccounts().stream()
                    .mapToLong(account -> account.getTransactions() != null ? account.getTransactions().size() : 0)
                    .sum();
        }
        dto.setTransactionCount(transactionCount);

        // Названия сегментов
        if (client.getSegments() != null) {
            List<String> segmentNames = client.getSegments().stream()
                    .map(Segment::getName)
                    .filter(name -> name != null && !name.isEmpty())
                    .toList();
            dto.setSegmentNames(segmentNames);
        }

        // Счета (преобразованные в DTO)
        if (client.getAccounts() != null) {
            List<AccountSummaryDTO> accounts = client.getAccounts().stream()
                    .map(this::toEnrichedAccountSummaryDTO)
                    .toList();
            dto.setAccounts(accounts);
        }

        // Дата последней транзакции
        LocalDateTime lastTransactionDate = null;
        if (client.getAccounts() != null) {
            lastTransactionDate = client.getAccounts().stream()
                    .flatMap(account -> account.getTransactions() != null ?
                            account.getTransactions().stream() :
                            java.util.stream.Stream.empty())
                    .map(Transaction::getCreatedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        }
        dto.setLastTransactionDate(lastTransactionDate);
    }

    // Вспомогательный метод для обогащения AccountSummaryDTO
    private AccountSummaryDTO toEnrichedAccountSummaryDTO(Account account) {
        AccountSummaryDTO dto = toAccountSummaryDTO(account);
        if (dto != null) {
            dto.setTransactionCount(account.getTransactions() != null ?
                    (long) account.getTransactions().size() : 0L);
        }
        return dto;
    }

    // После маппинга Account -> AccountSummaryDTO
    @AfterMapping
    default void enrichAccountSummaryDTO(@MappingTarget AccountSummaryDTO dto, Account account) {
        if (dto != null && account != null) {
            dto.setTransactionCount(account.getTransactions() != null ?
                    (long) account.getTransactions().size() : 0L);
        }
    }
}