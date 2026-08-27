package com.fangxuele.wepush.next.service.domain;

import java.time.Instant;

public record AudienceImportSession(String id, WorkspaceId workspaceId, String audienceId, String name,
                                    String format, String itemIdColumn, JsonDocument fieldMapping,
                                    Status status, long totalRows, long acceptedRows, long rejectedRows,
                                    long duplicateRows, Instant createdAt, Instant updatedAt) {
    public AudienceImportSession {
        id = DomainChecks.text(id, "audience import id");
        name = DomainChecks.text(name, "audience import name");
        format = DomainChecks.text(format, "audience import format");
        itemIdColumn = DomainChecks.text(itemIdColumn, "audience import item id column");
        if (workspaceId == null || fieldMapping == null || status == null || createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("audience import session is incomplete");
        }
        DomainChecks.nonNegative(totalRows, "import total rows");
        DomainChecks.nonNegative(acceptedRows, "import accepted rows");
        DomainChecks.nonNegative(rejectedRows, "import rejected rows");
        DomainChecks.nonNegative(duplicateRows, "import duplicate rows");
    }

    public enum Status { UPLOADING, PREVIEW_READY, COMMITTED, FAILED }
}
