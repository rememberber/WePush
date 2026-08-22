package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.api.ControlPlaneApi;
import com.fangxuele.wepush.next.service.application.ApplicationProblem;
import com.fangxuele.wepush.next.service.application.ArtifactApplicationService;
import com.fangxuele.wepush.next.service.domain.ArtifactDefinition;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
final class ArtifactController {
    private final ArtifactApplicationService artifacts;

    ArtifactController(ArtifactApplicationService artifacts) {
        this.artifacts = artifacts;
    }

    @PostMapping("/runs/{runId}/artifacts/result-export")
    ResponseEntity<ControlPlaneApi.ArtifactResponse> createResultExport(
            @PathVariable String workspaceId, @PathVariable String runId) {
        ArtifactApplicationService.CreationResult result = artifacts.createResultExport(
                new WorkspaceId(workspaceId), runId);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, links(result.artifact()).get("self"))
                .body(response(result.artifact()));
    }

    @GetMapping("/runs/{runId}/artifacts")
    List<ControlPlaneApi.ArtifactResponse> listRunArtifacts(
            @PathVariable String workspaceId, @PathVariable String runId) {
        return artifacts.listForRun(new WorkspaceId(workspaceId), runId).stream()
                .map(ArtifactController::response).toList();
    }

    @GetMapping("/artifacts/{artifactId}")
    ControlPlaneApi.ArtifactResponse getArtifact(
            @PathVariable String workspaceId, @PathVariable String artifactId) {
        return response(artifacts.get(new WorkspaceId(workspaceId), artifactId));
    }

    @GetMapping("/artifacts/{artifactId}/content")
    void downloadArtifact(@PathVariable String workspaceId, @PathVariable String artifactId,
                          @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
                          HttpServletResponse servletResponse) throws IOException {
        WorkspaceId workspace = new WorkspaceId(workspaceId);
        ArtifactDefinition metadata = artifacts.get(workspace, artifactId);
        ByteRange range = range(rangeHeader, metadata.size());
        try (ArtifactApplicationService.Download download = artifacts.open(
                workspace, artifactId, range.offset(), range.length())) {
            servletResponse.setStatus(range.partial() ? HttpStatus.PARTIAL_CONTENT.value() : HttpStatus.OK.value());
            servletResponse.setContentType(metadata.contentType());
            servletResponse.setContentLengthLong(range.length());
            servletResponse.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            servletResponse.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store");
            servletResponse.setHeader(HttpHeaders.ETAG, "\"" + metadata.sha256() + "\"");
            servletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + metadata.originalName() + "\"");
            servletResponse.setHeader("X-Content-Type-Options", "nosniff");
            if (range.partial()) {
                long end = range.offset() + range.length() - 1;
                servletResponse.setHeader(HttpHeaders.CONTENT_RANGE,
                        "bytes " + range.offset() + "-" + end + "/" + metadata.size());
            }
            download.content().transferTo(servletResponse.getOutputStream());
        }
    }

    private static ByteRange range(String header, long size) {
        if (header == null || header.isBlank()) {
            return new ByteRange(0, size, false);
        }
        try {
            if (!header.startsWith("bytes=") || header.indexOf(',') >= 0) {
                throw invalidRange();
            }
            String value = header.substring("bytes=".length()).trim();
            int dash = value.indexOf('-');
            if (dash < 0 || dash != value.lastIndexOf('-')) throw invalidRange();
            String startText = value.substring(0, dash).trim();
            String endText = value.substring(dash + 1).trim();
            long offset;
            long length;
            if (startText.isEmpty()) {
                long suffix = Long.parseLong(endText);
                if (suffix < 1 || size == 0) throw invalidRange();
                length = Math.min(suffix, size);
                offset = size - length;
            } else {
                offset = Long.parseLong(startText);
                if (offset < 0 || offset >= size) throw invalidRange();
                long end = endText.isEmpty() ? size - 1 : Long.parseLong(endText);
                if (end < offset) throw invalidRange();
                end = Math.min(end, size - 1);
                length = end - offset + 1;
            }
            return new ByteRange(offset, length, true);
        } catch (NumberFormatException exception) {
            throw invalidRange();
        }
    }

    private static ApplicationProblem invalidRange() {
        return new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "INVALID_RANGE",
                "Only one satisfiable HTTP byte range is supported");
    }

    private static ControlPlaneApi.ArtifactResponse response(ArtifactDefinition value) {
        return new ControlPlaneApi.ArtifactResponse(value.id(), value.workspaceId().value(), value.runId(),
                value.type(), value.backend(), value.originalName(), value.contentType(), value.size(),
                value.sha256(), value.state().name(), value.expiresAt(), value.pinned(), value.legalHold(),
                value.createdAt(), value.readyAt(), value.deletedAt(), value.version(), links(value));
    }

    private static Map<String, String> links(ArtifactDefinition value) {
        String base = "/api/v1/workspaces/" + value.workspaceId().value() + "/artifacts/" + value.id();
        return Map.of("self", base, "content", base + "/content");
    }

    private record ByteRange(long offset, long length, boolean partial) {
    }
}
