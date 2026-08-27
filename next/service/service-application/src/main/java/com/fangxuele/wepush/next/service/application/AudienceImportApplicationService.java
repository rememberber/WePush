package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceImportRepository;
import com.fangxuele.wepush.next.service.domain.AudienceImportRow;
import com.fangxuele.wepush.next.service.domain.AudienceImportSession;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.JsonDocument;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Streams uploaded CSV/TXT rows into database staging before creating an immutable snapshot. */
public final class AudienceImportApplicationService {
    private static final int BATCH_SIZE = 250;
    private static final long MAXIMUM_ROWS = 5_000_000;

    private final WorkspaceRepository workspaces;
    private final AudienceRepository audiences;
    private final AudienceImportRepository imports;
    private final JsonCodec json;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;

    public AudienceImportApplicationService(WorkspaceRepository workspaces, AudienceRepository audiences,
                                            AudienceImportRepository imports, JsonCodec json,
                                            ResourceIdGenerator ids, TransactionRunner transactions,
                                            Clock clock) {
        this.workspaces = workspaces;
        this.audiences = audiences;
        this.imports = imports;
        this.json = json;
        this.ids = ids;
        this.transactions = transactions;
        this.clock = clock;
    }

    public Preview upload(WorkspaceId workspaceId, Upload command, Reader reader) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        if (reader == null) throw bad("FIELD_REQUIRED", "Import file is required");
        String format = ApplicationSupport.text(command.format(), "format").toUpperCase(Locale.ROOT);
        if (!format.equals("CSV") && !format.equals("TXT")) throw bad("IMPORT_FORMAT_INVALID",
                "Audience import format must be CSV or TXT");
        if (command.audienceId() != null) audiences.findById(workspaceId, command.audienceId()).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "AUDIENCE_NOT_FOUND",
                        "Audience was not found: " + command.audienceId()));
        Map<String, String> mapping = canonicalMapping(command.fieldMapping());
        String itemIdColumn = command.itemIdColumn() == null || command.itemIdColumn().isBlank()
                ? (format.equals("TXT") ? "value" : "itemId") : command.itemIdColumn().trim();
        Instant now = clock.instant();
        AudienceImportSession session = new AudienceImportSession(ids.next("audimp"), workspaceId,
                command.audienceId(), ApplicationSupport.text(command.name(), "name"), format,
                itemIdColumn, json.canonicalize(mapping), AudienceImportSession.Status.UPLOADING,
                0, 0, 0, 0, now, now);
        return transactions.required(() -> {
            imports.create(session);
            try {
                if (format.equals("CSV")) parseCsv(session, mapping, command.delimiter(), reader);
                else parseTxt(session, mapping, reader);
            } catch (IOException problem) {
                throw bad("IMPORT_FILE_INVALID", "Audience import file could not be parsed");
            }
            AudienceImportSession ready = imports.finalizePreview(session.id(), workspaceId);
            return preview(ready);
        });
    }

    public Preview get(WorkspaceId workspaceId, String importId) {
        return preview(require(workspaceId, importId));
    }

    public AudienceDefinition commit(WorkspaceId workspaceId, String importId) {
        return transactions.required(() -> {
            AudienceImportSession session = require(workspaceId, importId);
            if (session.status() == AudienceImportSession.Status.COMMITTED) {
                return audiences.findById(workspaceId, session.audienceId()).orElseThrow();
            }
            if (session.status() != AudienceImportSession.Status.PREVIEW_READY || session.acceptedRows() == 0) {
                throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "IMPORT_NOT_COMMITTABLE",
                        "Import must have a preview and at least one accepted row");
            }
            Instant now = clock.instant();
            String hash = imports.acceptedContentHash(workspaceId, importId);
            if (session.audienceId() == null) {
                AudienceDefinition created = new AudienceDefinition(ids.next("aud"), workspaceId, session.name(),
                        ids.next("audsnap"), 1, session.acceptedRows(), hash,
                        AudienceDefinition.Status.ACTIVE, now, now, 0);
                imports.commitNew(session, created);
                return created;
            }
            AudienceDefinition current = audiences.findById(workspaceId, session.audienceId()).orElseThrow();
            AudienceDefinition updated = new AudienceDefinition(current.id(), workspaceId, session.name(),
                    ids.next("audsnap"), current.revision() + 1, session.acceptedRows(), hash,
                    current.status(), current.createdAt(), now, current.version() + 1);
            if (!imports.commitRevision(session, updated, current.version())) {
                throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "RESOURCE_VERSION_CONFLICT",
                        "Audience changed after the import preview; upload the file again");
            }
            return updated;
        });
    }

    public List<AudienceImportRow> errorRows(WorkspaceId workspaceId, String importId,
                                             long afterSequence, int limit) {
        require(workspaceId, importId);
        if (limit < 1 || limit > 1000) throw bad("INVALID_PAGE_LIMIT",
                "Import error page limit must be between 1 and 1000");
        return imports.rows(workspaceId, importId, false, afterSequence, limit);
    }

    private void parseCsv(AudienceImportSession session, Map<String, String> mapping,
                          String delimiterText, Reader reader) throws IOException {
        char delimiter = delimiterText == null || delimiterText.isEmpty() ? ',' : delimiterText.charAt(0);
        if (delimiterText != null && delimiterText.length() != 1) throw bad("DELIMITER_INVALID",
                "CSV delimiter must be one character");
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter)
                .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).get();
        try (CSVParser parser = format.parse(reader)) {
            if (!parser.getHeaderMap().containsKey(session.itemIdColumn())) {
                throw bad("ITEM_ID_COLUMN_MISSING", "CSV does not contain itemId column: " + session.itemIdColumn());
            }
            for (String source : mapping.keySet()) if (!parser.getHeaderMap().containsKey(source)) {
                throw bad("MAPPED_COLUMN_MISSING", "CSV does not contain mapped column: " + source);
            }
            List<AudienceImportRow> batch = new ArrayList<>(BATCH_SIZE);
            long sequence = 0;
            for (CSVRecord record : parser) {
                if (++sequence > MAXIMUM_ROWS) throw bad("IMPORT_TOO_LARGE", "Audience import exceeds 5,000,000 rows");
                String itemId = record.get(session.itemIdColumn()).trim();
                Map<String, Object> fields = new LinkedHashMap<>();
                mapping.forEach((source, target) -> fields.put(target, record.get(source)));
                if (mapping.isEmpty()) record.toMap().forEach((source, value) -> {
                    if (!source.equals(session.itemIdColumn())) fields.put(source, value);
                });
                batch.add(row(sequence, itemId, fields, record.toString()));
                flush(session, batch);
            }
            flushAll(session, batch);
        }
    }

    private void parseTxt(AudienceImportSession session, Map<String, String> mapping, Reader reader) throws IOException {
        String target = mapping.getOrDefault("value", "recipient");
        List<AudienceImportRow> batch = new ArrayList<>(BATCH_SIZE);
        try (BufferedReader lines = new BufferedReader(reader)) {
            String line; long sequence = 0;
            while ((line = lines.readLine()) != null) {
                if (++sequence > MAXIMUM_ROWS) throw bad("IMPORT_TOO_LARGE", "Audience import exceeds 5,000,000 rows");
                String value = line.trim();
                batch.add(row(sequence, value, Map.of(target, value), line));
                flush(session, batch);
            }
            flushAll(session, batch);
        }
    }

    private AudienceImportRow row(long sequence, String itemId, Map<String, Object> fields, String raw) {
        boolean accepted = itemId != null && !itemId.isBlank();
        return new AudienceImportRow(sequence, accepted ? itemId : "invalid_" + sequence,
                json.canonicalize(fields), raw == null ? "" : raw, accepted,
                accepted ? "" : "ITEM_ID_REQUIRED", accepted ? "" : "itemId is blank");
    }

    private void flush(AudienceImportSession session, List<AudienceImportRow> batch) {
        if (batch.size() >= BATCH_SIZE) flushAll(session, batch);
    }

    private void flushAll(AudienceImportSession session, List<AudienceImportRow> batch) {
        if (batch.isEmpty()) return;
        imports.append(session.id(), session.workspaceId(), List.copyOf(batch));
        batch.clear();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> canonicalMapping(Object input) {
        if (input == null) return Map.of();
        JsonDocument document = json.canonicalize(input);
        Map<?, ?> raw = json.read(document, Map.class);
        Map<String, String> mapping = new LinkedHashMap<>();
        raw.forEach((source, target) -> mapping.put(ApplicationSupport.text(String.valueOf(source), "mapping source"),
                ApplicationSupport.text(String.valueOf(target), "mapping target")));
        return Map.copyOf(mapping);
    }

    private AudienceImportSession require(WorkspaceId workspaceId, String importId) {
        return imports.findById(workspaceId, importId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "AUDIENCE_IMPORT_NOT_FOUND",
                        "Audience import was not found: " + importId));
    }

    private Preview preview(AudienceImportSession session) {
        return new Preview(session, imports.rows(session.workspaceId(), session.id(), true, 0, 20),
                imports.rows(session.workspaceId(), session.id(), false, 0, 20));
    }

    private static ApplicationProblem bad(String code, String message) {
        return new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, code, message);
    }

    public record Upload(String name, String audienceId, String format, String itemIdColumn,
                         Object fieldMapping, String delimiter) { }
    public record Preview(AudienceImportSession session, List<AudienceImportRow> acceptedPreview,
                          List<AudienceImportRow> errorPreview) { }
}
