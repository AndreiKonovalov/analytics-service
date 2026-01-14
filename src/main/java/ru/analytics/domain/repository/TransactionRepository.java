package ru.analytics.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.analytics.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId);

    List<Transaction> findByAccountIdAndCreatedAtBetween(
            Long accountId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Transaction> findByIsSuspiciousTrue();

    @EntityGraph(attributePaths = {"account", "account.client", "category", "tags"})
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT t FROM Transaction t " +
            "JOIN FETCH t.account a " +
            "JOIN FETCH a.client c " +
            "WHERE t.createdAt >= :startDate " +
            "AND t.createdAt <= :endDate " +
            "AND t.amount > :minAmount " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findLargeTransactionsInPeriod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minAmount") BigDecimal minAmount
    );

    @Query(value = """
        SELECT 
            DATE(t.created_at) as transaction_date,
            COUNT(*) as transaction_count,
            SUM(CASE WHEN t.amount < 0 THEN t.amount * -1 ELSE 0 END) as total_debit,
            SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END) as total_credit,
            AVG(t.amount) as average_amount
        FROM transactions t
        WHERE t.created_at >= :startDate 
        AND t.created_at <= :endDate
        GROUP BY DATE(t.created_at)
        ORDER BY transaction_date DESC
        """, nativeQuery = true)
    List<Object[]> getDailyTransactionStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.account.id = :accountId " +
            "AND t.createdAt >= :startDate " +
            "AND ABS(t.amount) > :threshold " +
            "AND t.isSuspicious = false " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findPotentiallySuspiciousTransactions(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("threshold") BigDecimal threshold,
            Pageable pageable
    );

    @Query("SELECT t.category.id, c.name, COUNT(t), SUM(ABS(t.amount)) " +
            "FROM Transaction t " +
            "JOIN t.category c " +
            "WHERE t.createdAt >= :startDate " +
            "AND t.createdAt <= :endDate " +
            "GROUP BY t.category.id, c.name " +
            "ORDER BY SUM(ABS(t.amount)) DESC")
    List<Object[]> getCategorySpendingAnalysis(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query(value = """
        WITH ranked_transactions AS (
            SELECT 
                t.*,
                ROW_NUMBER() OVER (PARTITION BY t.account_id ORDER BY t.created_at DESC) as rn
            FROM transactions t
            WHERE t.created_at >= :startDate
        )
        SELECT * FROM ranked_transactions
        WHERE rn <= :limitPerAccount
        """, nativeQuery = true)
    List<Transaction> findRecentTransactionsPerAccount(
            @Param("startDate") LocalDateTime startDate,
            @Param("limitPerAccount") int limitPerAccount
    );
}
