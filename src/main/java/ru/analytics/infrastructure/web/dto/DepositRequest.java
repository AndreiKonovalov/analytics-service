package ru.analytics.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Запрос на пополнение счета")
public class DepositRequest {

    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    @Schema(description = "Сумма пополнения", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "Описание операции", example = "Пополнение через банкомат")
    private String description = "Deposit";
}