package com.fangxuele.tool.push.ai;

import com.google.gson.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ClassicAiServiceTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    static final class FakeGateway implements ClassicAiService.Gateway {
        String fingerprint = "original";
        final AtomicInteger starts = new AtomicInteger();
        @Override public JsonElement listTasks(int offset, int limit) { return new JsonArray(); }
        @Override public ClassicAiService.Snapshot snapshot(int taskId) {
            return new ClassicAiService.Snapshot(fingerprint, AiJson.object("taskId", taskId, "recipientCount", 2));
        }
        @Override public ClassicAiService.Operation prepare(int taskId, String expected, boolean dryRun) {
            if (!fingerprint.equals(expected)) throw new IllegalArgumentException("changed");
            return new ClassicAiService.Operation() {
                @Override public void run() { starts.incrementAndGet(); }
                @Override public JsonObject progress() { return AiJson.object("totalCount", 2, "successCount", 2); }
            };
        }
        @Override public JsonElement history(int taskId, int offset, int limit) { return new JsonArray(); }
    }

    @Test public void confirmationDoesNotSendAndCannotBeReusedWithANewKey() throws Exception {
        FakeGateway gateway = new FakeGateway();
        ClassicAiService service = service(gateway, new ArrayList<>());
        String token = preview(service);
        assertEquals(0, gateway.starts.get());
        JsonObject args = sendArgs(token);
        JsonObject running = service.call("wepush_classic_send_run", args).getAsJsonObject();
        assertEquals("RUNNING", running.get("state").getAsString());
        assertEquals(running, service.call("wepush_classic_send_run", args));
        args.addProperty("idempotencyKey", UUID.randomUUID().toString());
        assertThrows(IllegalArgumentException.class, () -> service.call("wepush_classic_send_run", args));
    }

    @Test public void rejectsChangedSnapshotFalseConfirmationAndExpiredToken() throws Exception {
        FakeGateway gateway = new FakeGateway();
        List<Runnable> queue = new ArrayList<>();
        ClassicAiService service = service(gateway, queue);
        JsonObject args = sendArgs(preview(service));
        args.addProperty("userConfirmed", false);
        assertThrows(IllegalArgumentException.class, () -> service.call("wepush_classic_send_run", args));
        args.addProperty("userConfirmed", true);
        gateway.fingerprint = "changed";
        assertThrows(IllegalArgumentException.class, () -> service.call("wepush_classic_send_run", args));
        assertEquals(0, queue.size());
        long[] time = {0};
        Clock clock = new Clock() {
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return Instant.ofEpochMilli(time[0]); }
        };
        ClassicAiService expiring = new ClassicAiService(gateway, temporary.newFolder().toPath(), clock, queue::add);
        JsonObject expired = sendArgs(preview(expiring));
        time[0] = 600_000;
        assertThrows(IllegalArgumentException.class, () -> expiring.call("wepush_classic_send_run", expired));
    }

    @Test public void durableRequestSurvivesRestartWithoutReplayAndRejectsChangedIdentity() throws Exception {
        FakeGateway gateway = new FakeGateway();
        Path directory = temporary.newFolder().toPath();
        List<Runnable> queue = new ArrayList<>();
        ClassicAiService first = new ClassicAiService(gateway, directory, Clock.systemUTC(), queue::add);
        JsonObject args = sendArgs(preview(first));
        first.call("wepush_classic_send_run", args);
        assertEquals(1, queue.size());
        ClassicAiService recovered = new ClassicAiService(gateway, directory, Clock.systemUTC(), queue::add);
        assertEquals("UNKNOWN", recovered.call("wepush_classic_send_run", args).getAsJsonObject().get("state").getAsString());
        assertEquals(1, queue.size());
        args.addProperty("taskId", 2);
        assertThrows(IllegalArgumentException.class, () -> recovered.call("wepush_classic_send_run", args));
        assertEquals(0, gateway.starts.get());
    }

    @Test public void concurrentDuplicateRequestsStartOnlyOnceAndCompletionIsPersistent() throws Exception {
        FakeGateway gateway = new FakeGateway();
        Path directory = temporary.newFolder().toPath();
        List<Runnable> queue = new ArrayList<>();
        ClassicAiService service = new ClassicAiService(gateway, directory, Clock.systemUTC(), queue::add);
        JsonObject args = AiJson.object("taskId", 1, "idempotencyKey", UUID.randomUUID().toString());
        try (ExecutorService threads = Executors.newFixedThreadPool(8)) {
            List<Future<JsonElement>> calls = new ArrayList<>();
            for (int i = 0; i < 20; i++) calls.add(threads.submit(() -> service.call("wepush_classic_dry_run", args)));
            for (Future<JsonElement> call : calls) assertEquals("RUNNING", call.get().getAsJsonObject().get("state").getAsString());
        }
        assertEquals(1, queue.size());
        queue.getFirst().run();
        assertEquals(1, gateway.starts.get());
        ClassicAiService recovered = new ClassicAiService(gateway, directory);
        JsonObject result = recovered.call("wepush_classic_dry_run", args).getAsJsonObject();
        assertEquals("FINISHED", result.get("state").getAsString());
        assertFalse(result.has("identity"));
    }

    @Test public void failedJournalWriteCannotStartATaskAndInputsAreStrict() throws Exception {
        FakeGateway gateway = new FakeGateway();
        Path directory = temporary.newFolder().toPath();
        List<Runnable> queue = new ArrayList<>();
        ClassicAiService service = new ClassicAiService(gateway, directory, Clock.systemUTC(), queue::add);
        Files.delete(directory.resolve("requests"));
        Files.writeString(directory.resolve("requests"), "blocked");
        JsonObject args = AiJson.object("taskId", 1, "idempotencyKey", UUID.randomUUID().toString());
        assertThrows(java.io.IOException.class, () -> service.call("wepush_classic_dry_run", args));
        assertTrue(queue.isEmpty());
        for (JsonObject invalid : List.of(AiJson.object("taskId", 1.5), AiJson.object("taskId", "1"),
                AiJson.object("taskId", 1, "password", "x"), AiJson.object("taskId", 0))) {
            assertThrows(IllegalArgumentException.class, () -> service.call("wepush_classic_prepare_run", invalid));
        }
    }

    private ClassicAiService service(FakeGateway gateway, List<Runnable> queue) throws Exception {
        return new ClassicAiService(gateway, temporary.newFolder().toPath(), Clock.systemUTC(), queue::add);
    }

    static String preview(ClassicAiService service) throws Exception {
        return service.call("wepush_classic_prepare_run", AiJson.object("taskId", 1)).getAsJsonObject().get("confirmationToken").getAsString();
    }

    static JsonObject sendArgs(String token) {
        return AiJson.object("taskId", 1, "idempotencyKey", UUID.randomUUID().toString(), "confirmationToken", token, "userConfirmed", true);
    }
}
