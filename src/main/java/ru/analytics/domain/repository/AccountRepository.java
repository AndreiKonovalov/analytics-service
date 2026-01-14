package ru.analytics.domain.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.analytics.domain.model.Account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByClientId(Long clientId);

    List<Account> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"client", "transactions"})
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithDetails(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);

    @Query(value = """
        SELECT a.* FROM accounts a 
        WHERE a.balance < :threshold 
        AND a.is_active = true
        FOR UPDATE SKIP LOCKED
        LIMIT :batchSize
        """, nativeQuery = true)
    List<Account> findAccountsBelowThresholdForUpdate(
            @Param("threshold") BigDecimal threshold,
            @Param("batchSize") int batchSize
    );

    @Query("SELECT a FROM Account a " +
            "JOIN FETCH a.client c " +
            "WHERE a.balance > :minBalance " +
            "ORDER BY a.balance DESC")
    List<Account> findHighBalanceAccounts(@Param("minBalance") BigDecimal minBalance);

    @Query(value = """
        SELECT 
            a.account_number as accountNumber,
            a.balance,
            c.first_name || ' ' || c.last_name as clientName,
            COUNT(t.id) as transactionCount,
            SUM(CASE WHEN t.amount < 0 THEN t.amount * -1 ELSE 0 END) as totalDebit,
            SUM(CASE WHEN t.amount > 0 THEN t.amount ELSE 0 END) as totalCredit
        FROM accounts a
        JOIN clients c ON a.client_id = c.id
        LEFT JOIN transactions t ON t.account_id = a.id 
            AND t.created_at >= CURRENT_DATE - INTERVAL '30 days'
        WHERE a.is_active = true
        GROUP BY a.id, a.account_number, a.balance, c.first_name, c.last_name
        HAVING COUNT(t.id) > :minTransactions
        """,
            countQuery = """
        SELECT COUNT(DISTINCT a.id) 
        FROM accounts a
        JOIN clients c ON a.client_id = c.id
        LEFT JOIN transactions t ON t.account_id = a.id 
            AND t.created_at >= CURRENT_DATE - INTERVAL '30 days'
        WHERE a.is_active = true
        GROUP BY a.id
        HAVING COUNT(t.id) > :minTransactions
        """,
            nativeQuery = true)
    Page<Object[]> findAccountActivitySummary(
            @Param("minTransactions") int minTransactions,
            Pageable pageable
    );

    @Query("SELECT a.client.id, SUM(a.balance) as totalBalance " +
            "FROM Account a " +
            "WHERE a.isActive = true " +
            "GROUP BY a.client.id " +
            "HAVING SUM(a.balance) > :minTotalBalance")
    List<Object[]> findClientsWithHighTotalBalance(@Param("minTotalBalance") BigDecimal minTotalBalance);
}
