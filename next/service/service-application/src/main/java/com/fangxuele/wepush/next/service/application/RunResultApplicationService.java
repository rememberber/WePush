package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.RunItemResultRecord;
import com.fangxuele.wepush.next.service.domain.RunRepository;
import com.fangxuele.wepush.next.service.domain.RunResultRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;

public final class RunResultApplicationService {
    private static final String CURSOR_PURPOSE = "run-item-results-v1";

    private final RunRepository runs;
    private final RunResultRepository results;
    private final CursorCodec cursors;

    public RunResultApplicationService(RunRepository runs, RunResultRepository results, CursorCodec cursors) {
        this.runs = runs;
        this.results = results;
        this.cursors = cursors;
    }

    public Page page(WorkspaceId workspaceId, String runId, String cursor, int limit) {
        if (limit < 1 || limit > 500) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "INVALID_PAGE_LIMIT",
                    "Result page limit must be between 1 and 500");
        }
        if (runs.findById(workspaceId, runId).isEmpty()) {
            throw new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "RUN_NOT_FOUND",
                    "Run was not found: " + runId);
        }
        Position position = position(cursor);
        List<RunItemResultRecord> loaded = results.page(workspaceId, runId,
                position.completedAt(), position.itemId(), limit + 1);
        boolean hasMore = loaded.size() > limit;
        List<RunItemResultRecord> items = hasMore ? List.copyOf(loaded.subList(0, limit)) : List.copyOf(loaded);
        String nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            RunItemResultRecord last = items.getLast();
            nextCursor = cursors.encode(CURSOR_PURPOSE,
                    last.completedAt() + "\u0000" + last.itemId());
        }
        return new Page(items, nextCursor, hasMore);
    }

    private Position position(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new Position(null, null);
        }
        try {
            String decoded = cursors.decode(CURSOR_PURPOSE, cursor);
            int separator = decoded.indexOf('\0');
            if (separator < 1 || separator == decoded.length() - 1) {
                throw new IllegalArgumentException("cursor payload");
            }
            return new Position(Instant.parse(decoded.substring(0, separator)),
                    decoded.substring(separator + 1));
        } catch (IllegalArgumentException | DateTimeException exception) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "INVALID_CURSOR",
                    "Result cursor is invalid or has been modified");
        }
    }

    public record Page(List<RunItemResultRecord> items, String nextCursor, boolean hasMore) {
    }

    private record Position(Instant completedAt, String itemId) {
    }
}
