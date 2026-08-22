package com.fangxuele.wepush.next.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemorySecretValueTest {
    @Test
    void masksValueAndRejectsAccessAfterClose() {
        InMemorySecretValue value = InMemorySecretValue.of("top-secret");

        assertEquals("********", value.toString());
        assertEquals("top-secret", new String(value.copyChars()));

        value.close();
        assertThrows(IllegalStateException.class, value::copyChars);
    }
}
