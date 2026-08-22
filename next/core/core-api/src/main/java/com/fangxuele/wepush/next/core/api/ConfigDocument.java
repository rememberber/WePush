package com.fangxuele.wepush.next.core.api;

import java.util.Arrays;

public record ConfigDocument(
        String schemaId,
        String schemaVersion,
        String mediaType,
        byte[] canonicalContent
) {
    public static final String JSON_MEDIA_TYPE = "application/json";

    public ConfigDocument {
        schemaId = ApiChecks.notBlank(schemaId, "schemaId");
        schemaVersion = ApiChecks.notBlank(schemaVersion, "schemaVersion");
        mediaType = ApiChecks.notBlank(mediaType, "mediaType");
        if (canonicalContent == null) {
            throw new IllegalArgumentException("canonicalContent must not be null");
        }
        canonicalContent = canonicalContent.clone();
    }

    public ConfigDocument(String schemaId, String schemaVersion, byte[] canonicalContent) {
        this(schemaId, schemaVersion, JSON_MEDIA_TYPE, canonicalContent);
    }

    @Override
    public byte[] canonicalContent() {
        return canonicalContent.clone();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ConfigDocument that
                && schemaId.equals(that.schemaId)
                && schemaVersion.equals(that.schemaVersion)
                && mediaType.equals(that.mediaType)
                && Arrays.equals(canonicalContent, that.canonicalContent);
    }

    @Override
    public int hashCode() {
        int result = schemaId.hashCode();
        result = 31 * result + schemaVersion.hashCode();
        result = 31 * result + mediaType.hashCode();
        return 31 * result + Arrays.hashCode(canonicalContent);
    }

    @Override
    public String toString() {
        return "ConfigDocument[schemaId=%s, schemaVersion=%s, mediaType=%s, contentBytes=%d]"
                .formatted(schemaId, schemaVersion, mediaType, canonicalContent.length);
    }
}
