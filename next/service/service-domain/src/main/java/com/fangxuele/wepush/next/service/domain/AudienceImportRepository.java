package com.fangxuele.wepush.next.service.domain;

import java.util.List;
import java.util.Optional;

public interface AudienceImportRepository {
    void create(AudienceImportSession session);
    void append(String importId, WorkspaceId workspaceId, List<AudienceImportRow> rows);
    AudienceImportSession finalizePreview(String importId, WorkspaceId workspaceId);
    Optional<AudienceImportSession> findById(WorkspaceId workspaceId, String importId);
    List<AudienceImportRow> rows(WorkspaceId workspaceId, String importId,
                                 Boolean accepted, long afterSequence, int limit);
    String acceptedContentHash(WorkspaceId workspaceId, String importId);
    void commitNew(AudienceImportSession session, AudienceDefinition audience);
    boolean commitRevision(AudienceImportSession session, AudienceDefinition audience, long expectedVersion);
}
