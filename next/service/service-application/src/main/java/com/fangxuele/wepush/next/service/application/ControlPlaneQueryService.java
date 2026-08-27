package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.AccountDefinition;
import com.fangxuele.wepush.next.service.domain.AccountRepository;
import com.fangxuele.wepush.next.service.domain.AudienceDefinition;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.AuditEventRepository;
import com.fangxuele.wepush.next.service.domain.JobDefinition;
import com.fangxuele.wepush.next.service.domain.JobRepository;
import com.fangxuele.wepush.next.service.domain.MessageDefinition;
import com.fangxuele.wepush.next.service.domain.MessageRepository;
import com.fangxuele.wepush.next.service.domain.ResourcePageQuery;
import com.fangxuele.wepush.next.service.domain.RunDefinition;
import com.fangxuele.wepush.next.service.domain.RunOverview;
import com.fangxuele.wepush.next.service.domain.RunRepository;
import com.fangxuele.wepush.next.service.domain.ScheduleDefinition;
import com.fangxuele.wepush.next.service.domain.ScheduleRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.time.DateTimeException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Bounded resource queries shared by the HTTP API and future SDK surfaces. */
public final class ControlPlaneQueryService {
    private final WorkspaceRepository workspaces;
    private final AccountRepository accounts;
    private final MessageRepository messages;
    private final AudienceRepository audiences;
    private final JobRepository jobs;
    private final RunRepository runs;
    private final ScheduleRepository schedules;
    private final AuditEventRepository audits;
    private final CursorCodec cursors;
    private final Clock clock;

    public ControlPlaneQueryService(WorkspaceRepository workspaces, AccountRepository accounts,
                                    MessageRepository messages, AudienceRepository audiences,
                                    JobRepository jobs, RunRepository runs,
                                    ScheduleRepository schedules, AuditEventRepository audits,
                                    CursorCodec cursors, Clock clock) {
        this.workspaces = workspaces;
        this.accounts = accounts;
        this.messages = messages;
        this.audiences = audiences;
        this.jobs = jobs;
        this.runs = runs;
        this.schedules = schedules;
        this.audits = audits;
        this.cursors = cursors;
        this.clock = clock;
    }

    public Page<AccountDefinition> accounts(WorkspaceId workspace, Filters filters) {
        return page(workspace, "accounts-v1", filters, accounts::page,
                AccountDefinition::createdAt, AccountDefinition::id);
    }

    public Page<MessageDefinition> messages(WorkspaceId workspace, Filters filters) {
        return page(workspace, "messages-v1", filters, messages::page,
                MessageDefinition::createdAt, MessageDefinition::id);
    }

    public Page<AudienceDefinition> audiences(WorkspaceId workspace, Filters filters) {
        return page(workspace, "audiences-v1", filters, audiences::page,
                AudienceDefinition::createdAt, AudienceDefinition::id);
    }

    public Page<JobDefinition> jobs(WorkspaceId workspace, Filters filters) {
        return page(workspace, "jobs-v1", filters, jobs::page,
                JobDefinition::createdAt, JobDefinition::id);
    }

    public Page<RunDefinition> runs(WorkspaceId workspace, Filters filters) {
        return page(workspace, "runs-v1", filters, runs::page,
                RunDefinition::createdAt, RunDefinition::id);
    }

    public Page<ScheduleDefinition> schedules(WorkspaceId workspace, Filters filters) {
        return page(workspace, "schedules-v1", filters, schedules::page,
                ScheduleDefinition::createdAt, ScheduleDefinition::id);
    }

    public Page<AuditEventRepository.AuditEvent> audits(WorkspaceId workspace, Filters filters) {
        ApplicationSupport.requireWorkspace(workspaces, workspace);
        validate(filters);
        Position position = position("audits-v1", filters);
        ResourcePageQuery query = query(filters, position);
        List<AuditEventRepository.AuditEvent> loaded = audits.page(workspace.value(), query);
        return finish("audits-v1", filters, loaded, AuditEventRepository.AuditEvent::occurredAt,
                AuditEventRepository.AuditEvent::id);
    }

    public RunOverview overview(WorkspaceId workspace) {
        ApplicationSupport.requireWorkspace(workspaces, workspace);
        return runs.overview(workspace, clock.instant().minus(13, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS));
    }

    public List<RunDefinition> activeRuns(WorkspaceId workspace, int limit) {
        ApplicationSupport.requireWorkspace(workspaces, workspace);
        return runs.active(workspace, limit);
    }

    private <T> Page<T> page(WorkspaceId workspace, String purpose, Filters filters,
                             BiFunction<WorkspaceId, ResourcePageQuery, List<T>> loader,
                             Function<T, Instant> createdAt, Function<T, String> id) {
        ApplicationSupport.requireWorkspace(workspaces, workspace);
        validate(filters);
        Position position = position(purpose, filters);
        List<T> loaded = loader.apply(workspace, query(filters, position));
        return finish(purpose, filters, loaded, createdAt, id);
    }

    private static ResourcePageQuery query(Filters filters, Position position) {
        return new ResourcePageQuery(filters.name(), filters.status(), filters.from(), filters.to(),
                position.createdAt(), position.id(), filters.limit() + 1);
    }

    private <T> Page<T> finish(String purpose, Filters filters, List<T> loaded,
                               Function<T, Instant> createdAt, Function<T, String> id) {
        boolean hasMore = loaded.size() > filters.limit();
        List<T> items = hasMore ? List.copyOf(loaded.subList(0, filters.limit())) : List.copyOf(loaded);
        String next = null;
        if (hasMore && !items.isEmpty()) {
            T last = items.getLast();
            next = cursors.encode(purpose, fingerprint(filters) + "\0" + createdAt.apply(last) + "\0" + id.apply(last));
        }
        return new Page<>(items, next, hasMore);
    }

    private Position position(String purpose, Filters filters) {
        if (filters.cursor() == null || filters.cursor().isBlank()) return new Position(null, null);
        try {
            String decoded = cursors.decode(purpose, filters.cursor());
            String[] fields = decoded.split("\0", -1);
            if (fields.length != 3 || !fields[0].equals(fingerprint(filters)) || fields[2].isBlank()) {
                throw new IllegalArgumentException("cursor payload");
            }
            return new Position(Instant.parse(fields[1]), fields[2]);
        } catch (IllegalArgumentException | DateTimeException problem) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "INVALID_CURSOR",
                    "Resource cursor is invalid, modified, or belongs to different filters");
        }
    }

    private static void validate(Filters filters) {
        if (filters == null || filters.limit() < 1 || filters.limit() > 100) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "INVALID_PAGE_LIMIT",
                    "Resource page limit must be between 1 and 100");
        }
        if (filters.from() != null && filters.to() != null && filters.from().isAfter(filters.to())) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "INVALID_TIME_RANGE",
                    "Resource page time range is invalid");
        }
    }

    private static String fingerprint(Filters filters) {
        return ApplicationSupport.sha256(String.join("\0",
                value(filters.name()), value(filters.status()),
                value(filters.from()), value(filters.to())));
    }

    private static String value(Object value) { return value == null ? "" : value.toString(); }

    public record Filters(String cursor, int limit, String name, String status, Instant from, Instant to) { }
    public record Page<T>(List<T> items, String nextCursor, boolean hasMore) { }
    private record Position(Instant createdAt, String id) { }
}
