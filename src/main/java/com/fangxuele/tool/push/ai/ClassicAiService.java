package com.fangxuele.tool.push.ai;

import com.google.gson.*;

import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.Executor;

/** Authorization and durable deduplication, independent of Swing and provider SDKs. */
final class ClassicAiService {
    interface Gateway {
        JsonElement listTasks(int offset, int limit);
        Snapshot snapshot(int taskId);
        Operation prepare(int taskId, String fingerprint, boolean dryRun);
        JsonElement history(int taskId, int offset, int limit);
    }

    record Snapshot(String fingerprint, JsonObject preview) { }

    interface Operation {
        void run();
        JsonObject progress();
    }

    private record Confirmation(int taskId, String fingerprint, long expiresAt) { }

    private final Gateway gateway;
    private final Path requests;
    private final Clock clock;
    private final Executor executor;
    private final Map<String, Confirmation> confirmations = new HashMap<>();
    private final Map<String, Operation> active = new HashMap<>();

    ClassicAiService(Gateway gateway, Path directory) throws IOException {
        this(gateway, directory, Clock.systemUTC(), command -> Thread.ofVirtual().name("classic-ai-run").start(command));
    }

    ClassicAiService(Gateway gateway, Path directory, Clock clock, Executor executor) throws IOException {
        this.gateway = gateway;
        this.clock = clock;
        this.executor = executor;
        this.requests = directory.resolve("requests");
        AiJson.privateDirectory(requests);
    }

    synchronized JsonElement call(String tool, JsonObject args) throws IOException {
        String name = ClassicAiTools.validate(tool, args);
        int offset = args.has("offset") ? args.get("offset").getAsInt() : 0;
        int limit = args.has("limit") ? args.get("limit").getAsInt() : 30;
        return switch (name) {
            case "system_info" -> AiJson.object("product", "WePush Classic", "bridgeVersion", 1,
                    "executionScope", "已保存的手动固定线程任务；结果邮件提醒需在 Classic 中关闭后再接入执行",
                    "tools", ClassicAiTools.list());
            case "list_tasks" -> gateway.listTasks(offset, limit);
            case "get_task" -> gateway.snapshot(args.get("taskId").getAsInt()).preview();
            case "prepare_run" -> confirm(args.get("taskId").getAsInt());
            case "dry_run", "send_run" -> start(args, name.equals("dry_run"));
            case "get_run" -> getRun(args.get("runId").getAsString());
            case "list_history" -> gateway.history(args.get("taskId").getAsInt(), offset, limit);
            default -> throw new IllegalArgumentException("未知工具");
        };
    }

    private JsonObject confirm(int taskId) {
        confirmations.values().removeIf(value -> value.expiresAt() <= clock.millis());
        if (confirmations.size() >= 256) throw new IllegalStateException("未使用的发送预览过多，请稍后重试");
        Snapshot snapshot = gateway.snapshot(taskId);
        String token = UUID.randomUUID().toString();
        long expiresAt = clock.millis() + 600_000;
        confirmations.put(token, new Confirmation(taskId, snapshot.fingerprint(), expiresAt));
        return AiJson.object("preview", snapshot.preview(), "confirmationToken", token,
                "expiresAt", java.time.Instant.ofEpochMilli(expiresAt).toString(), "sendsMessages", false);
    }

    private JsonObject start(JsonObject args, boolean dryRun) throws IOException {
        int taskId = args.get("taskId").getAsInt();
        String runId = args.get("idempotencyKey").getAsString();
        String token = dryRun ? "" : args.get("confirmationToken").getAsString();
        if (!dryRun && !args.get("userConfirmed").getAsBoolean()) {
            throw new IllegalArgumentException("正式发送必须有用户授权并设置 userConfirmed=true");
        }
        String identity = AiJson.hash(taskId + ":" + dryRun + ":" + token);
        Path file = requests.resolve(runId + ".json");
        if (Files.exists(file)) {
            JsonObject previous = read(file);
            if (!previous.get("identity").getAsString().equals(identity)) {
                throw new IllegalArgumentException("此 idempotencyKey 已用于另一个请求，请查询原运行记录");
            }
            return getRun(runId);
        }
        if (active.size() >= 4) throw new IllegalStateException("已有 4 个 AI 任务运行，请等待完成");
        String fingerprint;
        if (dryRun) {
            fingerprint = gateway.snapshot(taskId).fingerprint();
        } else {
            Confirmation confirmation = confirmations.get(token);
            if (confirmation == null || confirmation.taskId() != taskId || confirmation.expiresAt() <= clock.millis()) {
                throw new IllegalArgumentException("发送预览已失效或不匹配，请重新 prepare_run");
            }
            fingerprint = confirmation.fingerprint();
        }
        Operation operation = gateway.prepare(taskId, fingerprint, dryRun);
        JsonObject record = AiJson.object("runId", runId, "identity", identity, "taskId", taskId,
                "dryRun", dryRun, "state", "STARTING", "createdAt", clock.instant().toString());
        // Never start a delivery unless its request is already durable. Recovered STARTING/RUNNING
        // records are UNKNOWN, never silently replayed, even if the process died before delivery.
        AiJson.write(file, record.toString());
        if (!dryRun) confirmations.remove(token);
        active.put(runId, operation);
        try {
            executor.execute(() -> finish(runId, record, operation));
        } catch (RuntimeException e) {
            active.remove(runId);
            throw e;
        }
        return getRun(runId);
    }

    private void finish(String runId, JsonObject record, Operation operation) {
        String state = "FINISHED";
        try { operation.run(); }
        catch (Throwable e) {
            // Provider exceptions can contain credentials/recipient contents; keep them out of MCP.
            state = "UNKNOWN";
        }
        synchronized (this) {
            try {
                record.addProperty("state", state);
                record.addProperty("finishedAt", clock.instant().toString());
                record.add("result", operation.progress());
                AiJson.write(requests.resolve(runId + ".json"), record.toString());
            } catch (Exception ignored) {
                // Original durable record remains; the next query conservatively reports UNKNOWN.
            } finally {
                active.remove(runId);
            }
        }
    }

    private JsonObject getRun(String runId) throws IOException {
        Path file = requests.resolve(runId + ".json");
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("运行记录不存在");
        JsonObject result = read(file);
        result.remove("identity");
        Operation operation = active.get(runId);
        if (operation != null) {
            result.addProperty("state", "RUNNING");
            result.add("result", operation.progress());
        } else if (Set.of("STARTING", "RUNNING").contains(result.get("state").getAsString())) {
            result.addProperty("state", "UNKNOWN");
        }
        if (result.get("state").getAsString().equals("UNKNOWN")) {
            result.addProperty("notice", "进程退出或执行异常导致结果不明。请核对 Classic 历史及渠道结果，禁止自动换 UUID 重发。");
        }
        return result;
    }

    private static JsonObject read(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }
}
