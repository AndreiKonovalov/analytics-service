package ru.analytics.domain.specification;

import com.querydsl.core.types.dsl.BooleanExpression;
import ru.analytics.domain.model.QTransaction;

import java.time.LocalDateTime;

public class TransactionSpecifications {

    public static BooleanExpression forLastMonth() {
        QTransaction transaction = QTransaction.transaction;
        return transaction.createdAt.after(LocalDateTime.now().minusMonths(1));
    }

    public static BooleanExpression withCategory(String category) {
        QTransaction transaction = QTransaction.transaction;
        return transaction.category.name.eq(category);
    }

}
