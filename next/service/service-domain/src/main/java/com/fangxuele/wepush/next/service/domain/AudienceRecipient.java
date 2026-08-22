package com.fangxuele.wepush.next.service.domain;

public record AudienceRecipient(long sequence, String itemId, JsonDocument fields) {
    public AudienceRecipient {
        DomainChecks.nonNegative(sequence, "recipient sequence");
        itemId = DomainChecks.text(itemId, "recipient item id");
        if (fields == null) {
            throw new IllegalArgumentException("recipient fields are required");
        }
    }
}
