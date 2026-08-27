package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.ScheduleApplicationService;
import com.fangxuele.wepush.next.service.application.ControlPlaneQueryService;
import com.fangxuele.wepush.next.service.api.ControlPlaneApi;
import com.fangxuele.wepush.next.service.domain.ScheduleDefinition;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/schedules")
final class ScheduleController {
    private final ScheduleApplicationService schedules;
    private final ControlPlaneQueryService queries;

    ScheduleController(ScheduleApplicationService schedules, ControlPlaneQueryService queries) {
        this.schedules = schedules;
        this.queries = queries;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ScheduleResponse create(@PathVariable String workspaceId, @RequestBody CreateScheduleRequest request) {
        return response(schedules.create(new WorkspaceId(workspaceId),
                new ScheduleApplicationService.CreateSchedule(request.name(), request.jobId(),
                        request.cronExpression(), request.timezone(),
                        ScheduleDefinition.MisfirePolicy.valueOf(request.misfirePolicy()),
                        request.enabled() == null || request.enabled())));
    }

    @GetMapping
    ControlPlaneApi.ResourcePageResponse<ScheduleResponse> list(@PathVariable String workspaceId,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String name, @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        var page = queries.schedules(new WorkspaceId(workspaceId),
                new ControlPlaneQueryService.Filters(cursor, limit, name, status, from, to));
        return new ControlPlaneApi.ResourcePageResponse<>(page.items().stream().map(ScheduleController::response).toList(),
                new ControlPlaneApi.CursorPage(page.nextCursor(), page.hasMore()));
    }

    @PatchMapping("/{scheduleId}")
    ScheduleResponse setEnabled(@PathVariable String workspaceId, @PathVariable String scheduleId,
                                @RequestBody UpdateScheduleRequest request) {
        ScheduleDefinition.MisfirePolicy misfire = request.misfirePolicy() == null ? null
                : ScheduleDefinition.MisfirePolicy.valueOf(request.misfirePolicy());
        return response(schedules.update(new WorkspaceId(workspaceId), scheduleId,
                new ScheduleApplicationService.UpdateSchedule(request.name(), request.jobId(),
                        request.cronExpression(), request.timezone(), misfire, request.enabled())));
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String workspaceId, @PathVariable String scheduleId) {
        schedules.delete(new WorkspaceId(workspaceId), scheduleId);
    }

    private static ScheduleResponse response(ScheduleDefinition value) {
        return new ScheduleResponse(value.id(), value.workspaceId().value(), value.jobId(), value.name(),
                value.cronExpression(), value.timezone(), value.misfirePolicy().name(), value.enabled(),
                value.nextFireAt(), value.lastFireAt(), value.createdAt(), value.updatedAt(), value.version());
    }

    record CreateScheduleRequest(String name, String jobId, String cronExpression,
                                 String timezone, String misfirePolicy, Boolean enabled) {
    }

    record UpdateScheduleRequest(String name, String jobId, String cronExpression,
                                 String timezone, String misfirePolicy, Boolean enabled) {
    }

    record ScheduleResponse(String id, String workspaceId, String jobId, String name,
                            String cronExpression, String timezone, String misfirePolicy,
                            boolean enabled, Instant nextFireAt, Instant lastFireAt,
                            Instant createdAt, Instant updatedAt, long version) {
    }
}
