package com.fangxuele.wepush.next.core.api;

public record ArtifactRef(String artifactId, String type, String sha256, long size) {
    public ArtifactRef {
        artifactId = ApiChecks.notBlank(artifactId, "artifactId");
        type = ApiChecks.notBlank(type, "type");
        sha256 = ApiChecks.notBlank(sha256, "sha256");
        if (size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
    }
}
