package com.fangxuele.wepush.next.service.domain;

public record AudienceImportRow(long sequence, String itemId, JsonDocument fields, String rawLine,
                                boolean accepted, String errorCode, String errorMessage) { }
