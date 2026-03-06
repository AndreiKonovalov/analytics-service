package ru.analytics.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.analytics.domain.model.Transaction;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountId(Long accountId);

}
