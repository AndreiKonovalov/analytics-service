package ru.analytics.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientReportDTO {

    private Long clientId;
    private String fullName;
    private String email;
    private String riskLevel;
    private String kycStatus;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private int totalAccounts;
    private BigDecimal totalBalance;
    private BigDecimal averageAccountBalance;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTransactionDate;

    private List<AccountReport> accounts;
    private List<SegmentDTO> segments;

    private MonthlyStats monthlyStats;
    private List<CategorySpending> topSpendingCategories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStats {
        private int transactionCount;
        private BigDecimal totalIncoming;
        private BigDecimal totalOutgoing;
        private BigDecimal netFlow;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySpending {
        private String categoryName;
        private BigDecimal totalAmount;
        private int transactionCount;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentDTO {
        private Long id;
        private String name;
        private String code;
    }
}
