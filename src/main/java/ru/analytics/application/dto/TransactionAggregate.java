package ru.analytics.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAggregate {

    private String categoryName;
    private BigDecimal totalAmount;
    private Long transactionCount;
    private BigDecimal averageAmount;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    public BigDecimal getAbsoluteTotalAmount() {
        return totalAmount.abs();
    }
}
