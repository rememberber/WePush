package com.fangxuele.wepush.next.agent.protocol;

public record LeaseFence(String leaseId, String runId, long epoch, String fencingToken) {
    public LeaseFence {
        if (leaseId == null || leaseId.isBlank() || runId == null || runId.isBlank()
                || fencingToken == null || fencingToken.isBlank()) {
            throw new IllegalArgumentException("lease fence values must not be blank");
        }
        if (epoch < 1) {
            throw new IllegalArgumentException("lease epoch must be positive");
        }
    }

    public boolean sameAuthority(LeaseFence candidate) {
        return candidate != null
                && leaseId.equals(candidate.leaseId)
                && runId.equals(candidate.runId)
                && epoch == candidate.epoch
                && fencingToken.equals(candidate.fencingToken);
    }

    @Override
    public String toString() {
        return "LeaseFence[leaseId=%s, runId=%s, epoch=%d, fencingToken=***]"
                .formatted(leaseId, runId, epoch);
    }
}
