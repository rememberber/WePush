package com.fangxuele.wepush.next.sdk;

@FunctionalInterface
public interface TokenProvider {
    String currentToken();

    static TokenProvider none() {
        return () -> "";
    }
}
