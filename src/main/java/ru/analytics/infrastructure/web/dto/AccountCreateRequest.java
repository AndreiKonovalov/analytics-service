package ru.analytics.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Запрос на создание счета")
public class AccountCreateRequest {

    @NotNull(message = "ID клиента обязательно")
    @Schema(description = "ID клиента", example = "1")
    private Long clientId;

    @NotBlank(message = "Номер счета обязателен")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$",
            message = "Некорректный формат номера счета (IBAN)")
    @Schema(description = "Номер счета (IBAN)", example = "RU02123456789012345678")
    private String accountNumber;

    @Schema(description = "Тип счета", example = "CHECKING",
            allowableValues = {"CHECKING", "SAVINGS", "INVESTMENT", "CREDIT"})
    private String accountType = "CHECKING";

    @Schema(description = "Валюта счета", example = "RUB")
    @Size(min = 3, max = 3, message = "Код валюты должен состоять из 3 символов")
    private String currencyCode = "RUB";

    @Schema(description = "Начальный баланс", example = "0.00")
    private BigDecimal initialBalance = BigDecimal.ZERO;

    @Schema(description = "Кредитный лимит", example = "0.00")
    private BigDecimal creditLimit = BigDecimal.ZERO;
}