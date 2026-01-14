package ru.analytics.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Краткая информация о счете")
public class AccountSummaryDTO {

    @Schema(description = "ID счета", example = "1")
    private Long id;

    @Schema(description = "Номер счета", example = "RU02123456789012345678")
    private String accountNumber;

    @Schema(description = "Баланс", example = "10000.00")
    private BigDecimal balance;

    @Schema(description = "Валюта", example = "RUB")
    private String currencyCode;

    @Schema(description = "Тип счета", example = "CHECKING")
    private String type;

    @Schema(description = "Активен ли счет", example = "true")
    private boolean active;

    @Schema(description = "Количество транзакций", example = "5")
    private Long transactionCount;
}
