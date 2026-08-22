package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.application.TransactionRunner;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Supplier;

public final class SpringTransactionRunner implements TransactionRunner {
    private final TransactionTemplate transactions;

    public SpringTransactionRunner(TransactionTemplate transactions) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public <T> T required(Supplier<T> work) {
        return transactions.execute(status -> work.get());
    }
}
