package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.JobDefinition;
import com.fangxuele.wepush.next.service.domain.JobRepository;
import com.fangxuele.wepush.next.service.domain.ScheduleDefinition;
import com.fangxuele.wepush.next.service.domain.ScheduleRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public final class ScheduleApplicationService {
    private final WorkspaceRepository workspaces;
    private final JobRepository jobs;
    private final ScheduleRepository schedules;
    private final RunApplicationService runs;
    private final ScheduleCalculator calculator;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;

    public ScheduleApplicationService(WorkspaceRepository workspaces, JobRepository jobs,
                                      ScheduleRepository schedules, RunApplicationService runs,
                                      ScheduleCalculator calculator, ResourceIdGenerator ids,
                                      TransactionRunner transactions, Clock clock) {
        this.workspaces = workspaces;
        this.jobs = jobs;
        this.schedules = schedules;
        this.runs = runs;
        this.calculator = calculator;
        this.ids = ids;
        this.transactions = transactions;
        this.clock = clock;
    }

    public ScheduleDefinition create(WorkspaceId workspaceId, CreateSchedule command) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        JobDefinition job = jobs.findById(workspaceId, command.jobId()).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "JOB_NOT_FOUND",
                        "Job was not found: " + command.jobId()));
        if (!job.enabled()) {
            throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "JOB_DISABLED",
                    "Disabled Job cannot be scheduled");
        }
        Instant now = clock.instant();
        Instant next = calculator.next(command.cronExpression(), command.timezone(), now);
        ScheduleDefinition created = new ScheduleDefinition(ids.next("schedule"), workspaceId,
                job.id(), ApplicationSupport.text(command.name(), "name"), command.cronExpression(),
                command.timezone(), command.misfirePolicy(), command.enabled(), next,
                null, now, now, 0);
        transactions.required(() -> schedules.create(created));
        return created;
    }

    public List<ScheduleDefinition> list(WorkspaceId workspaceId) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        return schedules.list(workspaceId);
    }

    public ScheduleDefinition setEnabled(WorkspaceId workspaceId, String scheduleId, boolean enabled) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        ScheduleDefinition schedule = schedules.findById(workspaceId, scheduleId).orElseThrow(() ->
                new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "SCHEDULE_NOT_FOUND",
                        "Schedule was not found: " + scheduleId));
        Instant now = clock.instant();
        Instant next = enabled ? calculator.next(schedule.cronExpression(), schedule.timezone(), now)
                : schedule.nextFireAt();
        transactions.required(() -> schedules.setEnabled(workspaceId, scheduleId, enabled, next, now));
        return schedules.findById(workspaceId, scheduleId).orElseThrow();
    }

    public ScheduleDefinition update(WorkspaceId workspaceId, String scheduleId, UpdateSchedule command) {
        return transactions.required(() -> {
            ApplicationSupport.requireWorkspace(workspaces, workspaceId);
            ScheduleDefinition current = schedules.findById(workspaceId, scheduleId).orElseThrow(() ->
                    new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "SCHEDULE_NOT_FOUND",
                            "Schedule was not found: " + scheduleId));
            String jobId = command.jobId() == null ? current.jobId() : command.jobId();
            JobDefinition job = jobs.findById(workspaceId, jobId).orElseThrow(() ->
                    new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "JOB_NOT_FOUND",
                            "Job was not found: " + jobId));
            boolean enabled = command.enabled() == null ? current.enabled() : command.enabled();
            if (enabled && (!job.enabled() || job.archived())) {
                throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "JOB_DISABLED",
                        "Disabled or archived Job cannot be scheduled");
            }
            String cron = command.cronExpression() == null ? current.cronExpression() : command.cronExpression();
            String timezone = command.timezone() == null ? current.timezone() : command.timezone();
            Instant now = clock.instant();
            Instant next = calculator.next(cron, timezone, now);
            ScheduleDefinition updated = new ScheduleDefinition(current.id(), workspaceId, jobId,
                    command.name() == null ? current.name() : ApplicationSupport.text(command.name(), "name"),
                    cron, timezone, command.misfirePolicy() == null ? current.misfirePolicy()
                    : command.misfirePolicy(), enabled, next, current.lastFireAt(), current.createdAt(),
                    now, current.version() + 1);
            if (!schedules.update(updated, current.version())) {
                throw new ApplicationProblem(ApplicationProblem.Kind.CONFLICT, "RESOURCE_VERSION_CONFLICT",
                        "Schedule was changed concurrently: " + scheduleId);
            }
            return updated;
        });
    }

    public void delete(WorkspaceId workspaceId, String scheduleId) {
        ApplicationSupport.requireWorkspace(workspaces, workspaceId);
        if (!transactions.required(() -> schedules.delete(workspaceId, scheduleId))) {
            throw new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "SCHEDULE_NOT_FOUND",
                    "Schedule was not found: " + scheduleId);
        }
    }

    public FireResult fireDue(int limit) {
        Instant now = clock.instant();
        List<ScheduleDefinition> due = schedules.listDue(now, limit);
        int fired = 0;
        int skipped = 0;
        for (ScheduleDefinition schedule : due) {
            boolean oldMisfire = schedule.nextFireAt().plusSeconds(60).isBefore(now);
            if (!(oldMisfire && schedule.misfirePolicy() == ScheduleDefinition.MisfirePolicy.SKIP)) {
                runs.createScheduled(schedule.workspaceId(), schedule.jobId(),
                        "schedule:" + schedule.id() + ":" + schedule.nextFireAt(),
                        new RunApplicationService.CreateRun(false, java.util.Map.of(),
                                "schedule:" + schedule.id(), null));
                fired++;
            } else {
                skipped++;
            }
            Instant next = calculator.next(schedule.cronExpression(), schedule.timezone(), now);
            transactions.required(() -> schedules.advance(schedule.id(), schedule.version(),
                    schedule.nextFireAt(), next, now));
        }
        return new FireResult(due.size(), fired, skipped);
    }

    public record CreateSchedule(String name, String jobId, String cronExpression, String timezone,
                                 ScheduleDefinition.MisfirePolicy misfirePolicy, boolean enabled) {
    }

    public record UpdateSchedule(String name, String jobId, String cronExpression, String timezone,
                                 ScheduleDefinition.MisfirePolicy misfirePolicy, Boolean enabled) { }

    public record FireResult(int due, int fired, int skipped) {
    }

    @FunctionalInterface
    public interface ScheduleCalculator {
        Instant next(String cronExpression, String timezone, Instant afterExclusive);
    }
}
