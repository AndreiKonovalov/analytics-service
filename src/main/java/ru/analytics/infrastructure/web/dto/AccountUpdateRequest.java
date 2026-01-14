package ru.analytics.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Запрос на обновление счета")
public class AccountUpdateRequest {

    @Schema(description = "Новый тип счета", example = "SAVINGS")
    private String accountType;

    @Schema(description = "Новая валюта счета", example = "USD")
    @Size(min = 3, max = 3, message = "Код валюты должен состоять из 3 символов")
    private String currencyCode;

    @Schema(description = "Новый кредитный лимит", example = "1000.00")
    private BigDecimal creditLimit;

    @Schema(description = "Статус активности", example = "true")
    private Boolean isActive;
}