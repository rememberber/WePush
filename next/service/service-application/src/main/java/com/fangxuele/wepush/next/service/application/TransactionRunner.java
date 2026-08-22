package com.fangxuele.wepush.next.service.application;

import java.util.function.Supplier;

public interface TransactionRunner {
    <T> T required(Supplier<T> work);

    default void required(Runnable work) {
        required(() -> {
            work.run();
            return null;
        });
    }
}
