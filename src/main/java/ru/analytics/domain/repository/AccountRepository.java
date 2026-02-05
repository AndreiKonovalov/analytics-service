package ru.analytics.domain.repository;

import jakarta.persistence.LockModeType;
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

}
