package ru.analytics.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.analytics.domain.model.Client;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByEmail(String email);

    Optional<Client> findByTaxIdentificationNumber(String taxId);

    List<Client> findByRiskLevel(String riskLevel);

    List<Client> findByKycStatus(String kycStatus);

    @EntityGraph(
            attributePaths = {
                    "accounts",
                    "accounts.transactions",
                    "accounts.transactions.category",
                    "segments"
            }
    )
    @Query("SELECT c FROM Client c WHERE c.id IN :ids")
    List<Client> findAllWithDetailsEntityGraph(@Param("ids") List<Long> ids);


//    @EntityGraph(attributePaths = {"accounts", "segments"})
//    @Query("SELECT c FROM Client c WHERE c.id IN :ids")
//    List<Client> findAllWithDetailsEntityGraph(@Param("ids") List<Long> ids);


//    @EntityGraph("Client.withFullDetails")
//    @Query("SELECT c FROM Client c WHERE c.id IN :ids")
//    List<Client> findAllWithDetailsEntityGraph(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"accounts"})
    @Query("SELECT c FROM Client c WHERE c.id = :id")
    Optional<Client> findByIdWithAccounts(@Param("id") Long id);

    @Query("SELECT DISTINCT c FROM Client c " +
            "LEFT JOIN FETCH c.accounts a " +
            "LEFT JOIN FETCH c.segments s " +
            "WHERE c.kycStatus = 'VERIFIED' " +
            "ORDER BY c.lastName, c.firstName")
    List<Client> findVerifiedClientsWithDetails();

    @Query(value = """
        SELECT c.* FROM clients c
        WHERE EXISTS (
            SELECT 1 FROM accounts a 
            WHERE a.client_id = c.id 
            AND a.balance > :minAccountBalance
        )
        AND c.date_of_birth <= :maxBirthDate
        """, nativeQuery = true)
    Page<Client> findEligibleClientsForPromotion(
            @Param("minAccountBalance") Double minAccountBalance,
            @Param("maxBirthDate") LocalDate maxBirthDate,
            Pageable pageable
    );

    @Query("SELECT c, COUNT(a) as accountCount, SUM(a.balance) as totalBalance " +
            "FROM Client c " +
            "LEFT JOIN c.accounts a " +
            "WHERE c.riskLevel = :riskLevel " +
            "GROUP BY c " +
            "HAVING COUNT(a) >= :minAccounts " +
            "ORDER BY totalBalance DESC")
    List<Object[]> findClientsByRiskLevelWithAggregates(
            @Param("riskLevel") String riskLevel,
            @Param("minAccounts") int minAccounts
    );

    @Query(value = """
        WITH client_stats AS (
            SELECT 
                c.id,
                c.first_name,
                c.last_name,
                COUNT(DISTINCT a.id) as num_accounts,
                AVG(a.balance) as avg_balance,
                COUNT(t.id) as num_transactions_last_month
            FROM clients c
            LEFT JOIN accounts a ON a.client_id = c.id
            LEFT JOIN transactions t ON t.account_id = a.id 
                AND t.created_at >= NOW() - INTERVAL '30 days'
            GROUP BY c.id, c.first_name, c.last_name
        )
        SELECT * FROM client_stats
        WHERE num_transactions_last_month > :minTransactions
        ORDER BY avg_balance DESC
        """, nativeQuery = true)
    List<Object[]> getClientStatistics(@Param("minTransactions") int minTransactions);

    // Оптимизированный запрос для получения клиентов с деталями
    @Query("SELECT DISTINCT c FROM Client c " +
            "LEFT JOIN FETCH c.transactions " +
            "LEFT JOIN FETCH c.accounts " +
            "WHERE c.id IN :ids")
    List<Client> findAllWithDetails(@Param("ids") List<Long> ids);

    // Получение только ID клиентов (для оптимизации)
    @Query("SELECT c.id FROM Client c")
    List<Long> findAllIds();
}
