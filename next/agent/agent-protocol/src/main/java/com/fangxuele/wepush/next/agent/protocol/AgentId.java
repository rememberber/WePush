package com.fangxuele.wepush.next.agent.protocol;

public record AgentId(String value) {
    public AgentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("agent id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
