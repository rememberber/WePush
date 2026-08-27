package com.fangxuele.wepush.next.service.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.service.api.ControlPlaneApi;
import com.fangxuele.wepush.next.service.application.ApplicationProblem;
import com.fangxuele.wepush.next.service.application.AudienceImportApplicationService;
import com.fangxuele.wepush.next.service.application.JsonCodec;
import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceImportRow;
import com.fangxuele.wepush.next.service.domain.AudienceImportSession;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/audience-imports")
final class AudienceImportController {
    private final AudienceImportApplicationService imports;
    private final ObjectMapper mapper;
    private final JsonCodec json;

    AudienceImportController(AudienceImportApplicationService imports, ObjectMapper mapper, JsonCodec json) {
        this.imports = imports;
        this.mapper = mapper;
        this.json = json;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ControlPlaneApi.AudienceImportResponse upload(@PathVariable String workspaceId, @RequestParam MultipartFile file,
                          @RequestParam String name, @RequestParam(required = false) String audienceId,
                          @RequestParam(defaultValue = "CSV") String format,
                          @RequestParam(required = false) String itemIdColumn,
                          @RequestParam(defaultValue = "{}") String fieldMapping,
                          @RequestParam(required = false) String delimiter) {
        Map<String, String> mapping = mapping(fieldMapping);
        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return response(imports.upload(new WorkspaceId(workspaceId),
                    new AudienceImportApplicationService.Upload(name, audienceId, format, itemIdColumn,
                            mapping, delimiter), reader));
        } catch (IOException problem) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "IMPORT_FILE_INVALID",
                    "Audience import file could not be read");
        }
    }

    @GetMapping("/{importId}")
    ControlPlaneApi.AudienceImportResponse get(@PathVariable String workspaceId, @PathVariable String importId) {
        return response(imports.get(new WorkspaceId(workspaceId), importId));
    }

    @PostMapping("/{importId}/commit")
    ControlPlaneApi.AudienceResponse commit(@PathVariable String workspaceId, @PathVariable String importId) {
        AudienceDefinition value = imports.commit(new WorkspaceId(workspaceId), importId);
        return new ControlPlaneApi.AudienceResponse(value.id(), value.workspaceId().value(), value.name(),
                value.snapshotId(), value.revision(), value.recordCount(), value.contentHash(),
                value.status().name(), value.createdAt(), value.updatedAt(), value.version());
    }

    @GetMapping(value = "/{importId}/errors.csv", produces = "text/csv")
    void errors(@PathVariable String workspaceId, @PathVariable String importId,
                HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=audience-import-errors.csv");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                response.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write("sequence,itemId,errorCode,errorMessage,rawLine\n");
            long after = 0;
            while (true) {
                List<AudienceImportRow> page = imports.errorRows(new WorkspaceId(workspaceId), importId, after, 1000);
                for (AudienceImportRow row : page) {
                    writer.write(row.sequence() + "," + csv(row.itemId()) + "," + csv(row.errorCode()) + ","
                            + csv(row.errorMessage()) + "," + csv(row.rawLine()) + "\n");
                    after = row.sequence();
                }
                if (page.size() < 1000) break;
            }
        }
    }

    private ControlPlaneApi.AudienceImportResponse response(AudienceImportApplicationService.Preview preview) {
        AudienceImportSession value = preview.session();
        return new ControlPlaneApi.AudienceImportResponse(value.id(), value.workspaceId().value(), value.audienceId(), value.name(),
                value.format(), value.itemIdColumn(), json.read(value.fieldMapping(), Object.class),
                value.status().name(), value.totalRows(), value.acceptedRows(), value.rejectedRows(),
                value.duplicateRows(), preview.acceptedPreview().stream().map(this::row).toList(),
                preview.errorPreview().stream().map(this::row).toList(), value.createdAt(), value.updatedAt(),
                "/api/v1/workspaces/" + value.workspaceId().value() + "/audience-imports/" + value.id()
                        + "/errors.csv");
    }

    private Map<String, String> mapping(String fieldMapping) {
        try {
            return mapper.readValue(fieldMapping, new TypeReference<>() { });
        } catch (IOException problem) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "FIELD_MAPPING_INVALID",
                    "Audience fieldMapping must be a JSON object with string values");
        }
    }

    private ControlPlaneApi.AudienceImportRowResponse row(AudienceImportRow value) {
        return new ControlPlaneApi.AudienceImportRowResponse(value.sequence(), value.itemId(),
                json.read(value.fields(), Object.class), value.rawLine(), value.accepted(),
                value.errorCode(), value.errorMessage());
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

}
