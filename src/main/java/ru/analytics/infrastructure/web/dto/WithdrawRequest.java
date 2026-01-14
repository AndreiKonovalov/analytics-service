package ru.analytics.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Запрос на снятие средств")
public class WithdrawRequest {

    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    @Schema(description = "Сумма снятия", example = "500.00")
    private BigDecimal amount;

    @Schema(description = "Описание операции", example = "Снятие наличных")
    private String description = "Withdrawal";
}