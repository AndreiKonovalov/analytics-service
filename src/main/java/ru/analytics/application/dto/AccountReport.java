package ru.analytics.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountReport {

    private Long accountId;
    private String accountNumber;
    private String accountType;
    private String currencyCode;
    private BigDecimal balance;
    private BigDecimal creditLimit;
    private boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private BigDecimal monthlyInflow;
    private BigDecimal monthlyOutflow;
    private int monthlyTransactionCount;

    private List<TransactionSummary> recentTransactions;
    private List<CategorySummary> spendingByCategory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionSummary {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime date;
        private BigDecimal amount;
        private String type;
        private String description;
        private String counterpartyName;
        private boolean suspicious;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private String categoryName;
        private BigDecimal totalAmount;
        private int count;
        private BigDecimal percentage;
    }
}
