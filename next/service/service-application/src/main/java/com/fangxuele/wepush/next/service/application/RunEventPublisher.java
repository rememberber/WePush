package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.RunEventRecord;

@FunctionalInterface
public interface RunEventPublisher {
    void publish(RunEventRecord event);
}
