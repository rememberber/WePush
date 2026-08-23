package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.api.ControlPlaneApi;
import com.fangxuele.wepush.next.service.application.JsonCodec;
import com.fangxuele.wepush.next.service.application.RunApplicationService;
import com.fangxuele.wepush.next.service.application.RunEventPublisher;
import com.fangxuele.wepush.next.service.domain.RunEventRecord;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

final class LocalRunEventHub implements RunEventPublisher {
    private static final long STREAM_TIMEOUT_MILLIS = 30L * 60L * 1_000L;

    private final ConcurrentHashMap<RunKey, Channel> channels = new ConcurrentHashMap<>();
    private final JsonCodec json;

    LocalRunEventHub(JsonCodec json) {
        this.json = json;
    }

    SseEmitter subscribe(WorkspaceId workspaceId, String runId, long afterSequence,
                         RunApplicationService runs) {
        runs.get(workspaceId, runId);
        RunKey key = new RunKey(workspaceId.value(), runId);
        Channel channel = channels.computeIfAbsent(key, ignored -> new Channel());
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        Subscriber subscriber = new Subscriber(emitter, afterSequence);
        Runnable cleanup = () -> {
            channel.subscribers.remove(subscriber);
            if (channel.subscribers.isEmpty()) {
                channels.remove(key, channel);
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        synchronized (channel) {
            channel.subscribers.add(subscriber);
            List<RunEventRecord> replay = runs.eventsAfter(workspaceId, runId, afterSequence, 1_000);
            replay.forEach(event -> send(channel, subscriber, event));
        }
        return emitter;
    }

    @Override
    public void publish(RunEventRecord event) {
        RunKey key = new RunKey(event.workspaceId().value(), event.runId());
        Channel channel = channels.get(key);
        if (channel == null) {
            return;
        }
        synchronized (channel) {
            channel.subscribers.forEach(subscriber -> send(channel, subscriber, event));
        }
    }

    /**
     * Replays database events into locally connected SSE clients. This deliberately polls the
     * durable event log so an event committed by another Service instance is still observed.
     */
    void poll(RunApplicationService runs) {
        channels.forEach((key, channel) -> {
            synchronized (channel) {
                if (channel.subscribers.isEmpty()) return;
                long after = channel.subscribers.stream()
                        .mapToLong(subscriber -> subscriber.lastSequence)
                        .min().orElse(0L);
                WorkspaceId workspaceId = new WorkspaceId(key.workspaceId());
                List<RunEventRecord> replay = runs.eventsAfter(workspaceId, key.runId(), after, 1_000);
                replay.forEach(event -> channel.subscribers
                        .forEach(subscriber -> send(channel, subscriber, event)));
            }
        });
    }

    private void send(Channel channel, Subscriber subscriber, RunEventRecord event) {
        if (event.sequence() <= subscriber.lastSequence) {
            return;
        }
        try {
            subscriber.emitter.send(SseEmitter.event()
                    .id(Long.toString(event.sequence()))
                    .name(event.type())
                    .data(response(event)));
            subscriber.lastSequence = event.sequence();
        } catch (IOException | IllegalStateException exception) {
            channel.subscribers.remove(subscriber);
            subscriber.emitter.completeWithError(exception);
        }
    }

    private ControlPlaneApi.RunEventResponse response(RunEventRecord event) {
        return new ControlPlaneApi.RunEventResponse(event.runId(), event.sequence(), event.type(),
                event.occurredAt(), json.read(event.payload(), Object.class), event.severity().name());
    }

    private record RunKey(String workspaceId, String runId) {
        private RunKey {
            Objects.requireNonNull(workspaceId);
            Objects.requireNonNull(runId);
        }
    }

    private static final class Channel {
        private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    }

    private static final class Subscriber {
        private final SseEmitter emitter;
        private long lastSequence;

        private Subscriber(SseEmitter emitter, long lastSequence) {
            this.emitter = emitter;
            this.lastSequence = lastSequence;
        }
    }
}
