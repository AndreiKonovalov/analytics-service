package ru.analytics.domain.repository;

import ru.analytics.application.dto.TransactionAggregate;
import ru.analytics.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepositoryCustom {

    List<TransactionAggregate> aggregateByCategory(LocalDateTime from, LocalDateTime to);

    List<Transaction> findTransactionsByCriteria(
            Long accountId,
            LocalDateTime from,
            LocalDateTime to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            List<String> categories,
            boolean suspiciousOnly
    );

    List<Object[]> findClientSpendingPatterns(Long clientId, int months);

    List<Transaction> findSimilarTransactions(Transaction transaction, int similarityThreshold);

}
