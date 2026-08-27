package com.fangxuele.wepush.next.service.domain;

import java.time.LocalDate;
import java.util.List;

public record RunOverview(long activeRuns, long totalRuns, long succeededRuns, long problemRuns,
                          List<TrendPoint> trend) {
    public record TrendPoint(LocalDate day, long total, long succeeded, long problem) { }
}
