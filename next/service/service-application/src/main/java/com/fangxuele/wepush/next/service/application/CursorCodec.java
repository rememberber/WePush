package com.fangxuele.wepush.next.service.application;

public interface CursorCodec {
    String encode(String purpose, String value);

    String decode(String purpose, String cursor);
}
