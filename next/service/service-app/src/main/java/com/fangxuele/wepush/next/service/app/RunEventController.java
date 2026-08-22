package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.RunApplicationService;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/runs/{runId}/events")
final class RunEventController {
    private final LocalRunEventHub eventHub;
    private final RunApplicationService runs;

    RunEventController(LocalRunEventHub eventHub, RunApplicationService runs) {
        this.eventHub = eventHub;
        this.runs = runs;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter events(@PathVariable String workspaceId, @PathVariable String runId,
                      @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        long afterSequence = lastEventId == null || lastEventId.isBlank() ? 0 : parse(lastEventId);
        return eventHub.subscribe(new WorkspaceId(workspaceId), runId, afterSequence, runs);
    }

    private static long parse(String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Last-Event-ID must be a non-negative event sequence");
        }
    }
}
