package com.fangxuele.wepush.next.core.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ConfigDocumentTest {
    @Test
    void contentIsDefensivelyCopiedAndNotPrinted() {
        byte[] input = "secret-content".getBytes();
        ConfigDocument document = new ConfigDocument("account", "1", input);

        input[0] = 0;
        byte[] firstRead = document.canonicalContent();
        firstRead[1] = 0;

        assertArrayEquals("secret-content".getBytes(), document.canonicalContent());
        assertNotEquals(-1, document.toString().indexOf("contentBytes="));
        assertEquals(-1, document.toString().indexOf("secret-content"));
    }
}
