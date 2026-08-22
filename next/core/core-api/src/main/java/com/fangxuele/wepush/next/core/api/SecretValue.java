package com.fangxuele.wepush.next.core.api;

public interface SecretValue extends AutoCloseable {
    char[] copyChars();

    byte[] copyBytes();

    @Override
    void close();
}
