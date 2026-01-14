package ru.analytics.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import ru.analytics.domain.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Информация о транзакции")
public class TransactionDTO {

    @Schema(description = "ID транзакции", example = "1")
    private Long id;

    @Schema(description = "Сумма транзакции", example = "1000.50")
    private BigDecimal amount;

    @Schema(description = "Валюта", example = "RUB")
    private String currencyCode;

    @Schema(description = "Тип транзакции", example = "PAYMENT")
    private TransactionType type;

    @Schema(description = "Название категории", example = "Продукты")
    private String categoryName;

    @Schema(description = "Описание", example = "Оплата в супермаркете")
    private String description;

    @Schema(description = "Счет контрагента", example = "RU02987654321098765432")
    private String counterpartyAccount;

    @Schema(description = "Имя контрагента", example = "ООО 'Рога и копыта'")
    private String counterpartyName;

    @Schema(description = "Подозрительная транзакция", example = "false")
    private boolean suspicious;

    @Schema(description = "Статус", example = "COMPLETED")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Дата создания", example = "2024-01-14 11:30:00")
    private LocalDateTime createdAt;
}