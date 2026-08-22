package com.fangxuele.wepush.next.core.api;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InMemorySecretValue implements SecretValue {
    private static final String MASK = "********";

    private final AtomicBoolean closed = new AtomicBoolean();
    private char[] chars;

    private InMemorySecretValue(char[] chars) {
        this.chars = chars.clone();
    }

    public static InMemorySecretValue of(char[] chars) {
        if (chars == null) {
            throw new IllegalArgumentException("chars must not be null");
        }
        return new InMemorySecretValue(chars);
    }

    public static InMemorySecretValue of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        return new InMemorySecretValue(value.toCharArray());
    }

    @Override
    public synchronized char[] copyChars() {
        ensureOpen();
        return chars.clone();
    }

    @Override
    public synchronized byte[] copyBytes() {
        ensureOpen();
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(chars));
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            if (encoded.hasArray()) {
                Arrays.fill(encoded.array(), (byte) 0);
            }
            return bytes;
        } catch (CharacterCodingException exception) {
            throw new IllegalStateException("Secret cannot be encoded as UTF-8", exception);
        }
    }

    @Override
    public synchronized void close() {
        if (closed.compareAndSet(false, true)) {
            Arrays.fill(chars, '\0');
            chars = new char[0];
        }
    }

    @Override
    public String toString() {
        return MASK;
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("SecretValue is closed");
        }
    }
}
