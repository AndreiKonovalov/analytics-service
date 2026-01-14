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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReport {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;

    private ReportPeriod period;

    // Статистика по клиентам
    private int totalClients;
    private int activeClients;
    private int newClientsThisMonth;
    private Map<String, Long> clientsByRiskLevel;
    private Map<String, Long> clientsByKycStatus;

    // Статистика по счетам
    private int totalAccounts;
    private BigDecimal totalBalance;
    private BigDecimal averageBalancePerAccount;
    private Map<String, Long> accountsByType;

    // Статистика по транзакциям
    private long totalTransactions;
    private BigDecimal totalTransactionVolume;
    private BigDecimal averageTransactionAmount;
    private long suspiciousTransactions;

    // Топ категории расходов
    private List<CategorySpending> topSpendingCategories;

    // Аномалии и паттерны
    private List<AnomalyDetection> detectedAnomalies;
    private List<SpendingPattern> spendingPatterns;

    // Производительность системы
    private PerformanceMetrics performanceMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportPeriod {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySpending {
        private String category;
        private BigDecimal totalAmount;
        private long transactionCount;
        private BigDecimal percentageOfTotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalyDetection {
        private String type;
        private String description;
        private int affectedAccounts;
        private BigDecimal totalAmount;
        private LocalDateTime detectedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpendingPattern {
        private String patternType;
        private String description;
        private int affectedClients;
        private BigDecimal averageAmount;
        private String recommendedAction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private double averageQueryTimeMs;
        private int maxQueryTimeMs;
        private int slowQueriesCount;
        private int nPlusOneQueriesDetected;
        private Map<String, Integer> queryTypeDistribution;
    }
}
