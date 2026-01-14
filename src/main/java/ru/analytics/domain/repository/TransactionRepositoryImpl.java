package ru.analytics.domain.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.analytics.application.dto.TransactionAggregate;
import ru.analytics.domain.model.QTransaction;
import ru.analytics.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private final EntityManager em;

    @Override
    public List<TransactionAggregate> aggregateByCategory(LocalDateTime from, LocalDateTime to) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QTransaction t = QTransaction.transaction;
        ru.analytics.domain.model.QCategory c =
                ru.analytics.domain.model.QCategory.category;

        return queryFactory
                .select(Projections.constructor(
                        TransactionAggregate.class,
                        c.name,
                        t.amount.sum(),
                        t.count(),
                        t.amount.avg(),
                        t.amount.min(),
                        t.amount.max()
                ))
                .from(t)
                .join(t.category, c)
                .where(t.createdAt.between(from, to))
                .groupBy(c.name)
                .orderBy(t.amount.sum().desc())
                .fetch();
    }

    @Override
    public List<Transaction> findTransactionsByCriteria(
            Long accountId,
            LocalDateTime from,
            LocalDateTime to,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            List<String> categories,
            boolean suspiciousOnly
    ) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QTransaction t = QTransaction.transaction;
        ru.analytics.domain.model.QCategory c =
                ru.analytics.domain.model.QCategory.category;

        BooleanBuilder predicate = new BooleanBuilder();

        if (accountId != null) {
            predicate.and(t.account.id.eq(accountId));
        }

        if (from != null && to != null) {
            predicate.and(t.createdAt.between(from, to));
        } else if (from != null) {
            predicate.and(t.createdAt.after(from));
        } else if (to != null) {
            predicate.and(t.createdAt.before(to));
        }

        if (minAmount != null) {
            predicate.and(t.amount.abs().goe(minAmount));
        }

        if (maxAmount != null) {
            predicate.and(t.amount.abs().loe(maxAmount));
        }

        if (categories != null && !categories.isEmpty()) {
            predicate.and(c.name.in(categories));
        }

        if (suspiciousOnly) {
            predicate.and(t.isSuspicious.isTrue());
        }

        return queryFactory
                .selectFrom(t)
                .join(t.category, c).fetchJoin()
                .where(predicate)
                .orderBy(t.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Object[]> findClientSpendingPatterns(Long clientId, int months) {
        QTransaction t = QTransaction.transaction;
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        ru.analytics.domain.model.QAccount a =
                ru.analytics.domain.model.QAccount.account;
        ru.analytics.domain.model.QCategory c =
                ru.analytics.domain.model.QCategory.category;

        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(months);

        List<com.querydsl.core.Tuple> results = queryFactory
                .select(
                        Expressions.stringTemplate("DATE_TRUNC('month', {0})", t.createdAt),
                        c.name,
                        t.amount.sum(),
                        t.count()
                )
                .from(t)
                .join(t.account, a)
                .join(t.category, c)
                .where(a.client.id.eq(clientId)
                        .and(t.createdAt.after(cutoffDate))
                        .and(t.amount.lt(BigDecimal.ZERO)))
                .groupBy(
                        Expressions.stringTemplate("DATE_TRUNC('month', {0})", t.createdAt),
                        c.name
                )
                .orderBy(
                        Expressions.stringTemplate("DATE_TRUNC('month', {0})", t.createdAt).desc(),
                        t.amount.sum().asc()
                )
                .fetch();

        // Конвертируем Tuple в Object[]
        return results.stream()
                .map(tuple -> new Object[]{
                        tuple.get(0, Object.class),
                        tuple.get(1, String.class),
                        tuple.get(2, BigDecimal.class),
                        tuple.get(3, Long.class)
                })
                .toList();
    }

    @Override
    public List<Transaction> findSimilarTransactions(Transaction transaction, int similarityThreshold) {
        QTransaction t = QTransaction.transaction;
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        BooleanExpression similarityCondition = t.account.id.eq(transaction.getAccount().getId())
                .and(t.id.ne(transaction.getId()))
                .and(t.amount.abs()
                        .subtract(transaction.getAmount().abs())
                        .abs()
                        .divide(transaction.getAmount().abs())
                        .multiply(100)
                        .loe(similarityThreshold))
                .and(t.createdAt.between(
                        transaction.getCreatedAt().minusDays(7),
                        transaction.getCreatedAt().plusDays(7)
                ));

        if (transaction.getCounterpartyAccount() != null) {
            similarityCondition = similarityCondition
                    .and(t.counterpartyAccount.eq(transaction.getCounterpartyAccount()));
        }

        if (transaction.getCategory() != null) {
            similarityCondition = similarityCondition
                    .and(t.category.id.eq(transaction.getCategory().getId()));
        }

        return queryFactory
                .selectFrom(t)
                .where(similarityCondition)
                .orderBy(
                        t.amount.abs()
                                .subtract(transaction.getAmount().abs())
                                .abs()
                                .asc(),
                        t.createdAt.desc()
                )
                .limit(10)
                .fetch();
    }
}
