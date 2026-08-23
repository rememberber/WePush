package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.ScheduleApplicationService;
import org.springframework.scheduling.annotation.Scheduled;

final class ScheduleScanner {
    private final ScheduleApplicationService schedules;
    private final ScheduleLeadership leadership;

    ScheduleScanner(ScheduleApplicationService schedules, ScheduleLeadership leadership) {
        this.schedules = schedules;
        this.leadership = leadership;
    }

    @Scheduled(fixedDelayString = "${wepush.schedule.scan-interval:PT5S}")
    void scan() {
        if (leadership.isLeader()) schedules.fireDue(100);
    }
}
