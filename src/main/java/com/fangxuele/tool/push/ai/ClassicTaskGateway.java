package com.fangxuele.tool.push.ai;

import com.fangxuele.tool.push.dao.*;
import com.fangxuele.tool.push.domain.*;
import com.fangxuele.tool.push.logic.*;
import com.fangxuele.tool.push.logic.msgsender.*;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.google.gson.*;

import java.util.*;

final class ClassicTaskGateway implements ClassicAiService.Gateway {
    private record Resources(TTask task, TMsg message, TAccount account, TPeople people, List<TPeopleData> rows) {
        String fingerprint() { return AiJson.hash(AiJson.GSON.toJson(this)); }
    }

    private static <T> T mapper(Class<T> type) { return MybatisUtil.getSqlSession().getMapper(type); }

    @Override
    public JsonElement listTasks(int offset, int limit) {
        return MybatisUtil.withSessionLock(() -> {
            List<TTask> tasks = mapper(TTaskMapper.class).selectAll();
            return page(tasks.stream().map(ClassicTaskGateway::taskSummary).toList(), offset, limit);
        });
    }

    @Override
    public ClassicAiService.Snapshot snapshot(int taskId) {
        return MybatisUtil.withSessionLock(() -> {
            Resources r = resources(taskId);
            JsonObject preview = taskSummary(r.task());
            preview.add("account", AiJson.object("id", r.account().getId(), "name", r.account().getAccountName()));
            preview.add("message", AiJson.object("id", r.message().getId(), "name", r.message().getMsgName(),
                    "template", redact(JsonParser.parseString(r.message().getContent()))));
            preview.add("audience", AiJson.object("id", r.people().getId(), "name", r.people().getPeopleName(),
                    "recipientCount", r.rows().size()));
            preview.addProperty("threadCount", r.task().getThreadCnt());
            preview.addProperty("intervalPush", r.task().getIntervalPush());
            preview.addProperty("intervalSeconds", r.task().getIntervalTime());
            preview.addProperty("executionIssue", executionIssue(r.task()));
            return new ClassicAiService.Snapshot(r.fingerprint(), preview);
        });
    }

    @Override
    public ClassicAiService.Operation prepare(int taskId, String fingerprint, boolean dryRun) {
        return MybatisUtil.withSessionLock(() -> {
            Resources r = resources(taskId);
            if (!r.fingerprint().equals(fingerprint)) {
                throw new IllegalArgumentException("任务、消息、账号或人群已变化，请重新 prepare_run 并核对预览");
            }
            String issue = executionIssue(r.task());
            if (issue != null) throw new IllegalArgumentException(issue);
            if (r.rows().isEmpty()) throw new IllegalArgumentException("目标人群为空，请先在 Classic 中导入");
            List<String[]> recipients = r.rows().stream()
                    .map(row -> AiJson.GSON.fromJson(row.getVarData(), String[].class)).toList();
            if (recipients.stream().anyMatch(row -> row == null || row.length == 0)) {
                throw new IllegalArgumentException("人群存在无效记录，请在 Classic 中修正");
            }
            List<IMsgSender> senders = new ArrayList<>();
            int count = Math.min(r.task().getThreadCnt(), recipients.size());
            for (int i = 0; i < count; i++) {
                IMsgSender sender;
                try {
                    sender = MsgSenderFactory.getMsgSender(r.message().getId(), dryRun ? 1 : 0);
                } catch (RuntimeException e) {
                    // Provider validation errors may embed account values; do not expose them through MCP.
                    throw new IllegalStateException("无法准备渠道发送器，请在 Classic 中检查账号和消息配置", e);
                }
                if (sender == null) throw new IllegalArgumentException("暂不支持此消息渠道");
                senders.add(sender);
            }
            TaskRunThread runner = new TaskRunThread(r.task(), r.message(), recipients, senders, dryRun ? 1 : 0);
            return new ClassicAiService.Operation() {
                @Override public void run() { runner.run(); }
                @Override public JsonObject progress() {
                    TTaskHis history = runner.getTaskHis();
                    if (history == null) return AiJson.object("totalCount", recipients.size(), "successCount", 0, "failCount", 0);
                    return historySummary(history);
                }
            };
        });
    }

    @Override
    public JsonElement history(int taskId, int offset, int limit) {
        return MybatisUtil.withSessionLock(() -> page(mapper(TTaskHisMapper.class).selectByTaskId(taskId)
                .stream().map(ClassicTaskGateway::historySummary).toList(), offset, limit));
    }

    private static Resources resources(int id) {
        TTask task = copy(required(mapper(TTaskMapper.class).selectByPrimaryKey(id), "任务"), TTask.class);
        TMsg message = copy(required(mapper(TMsgMapper.class).selectByPrimaryKey(task.getMessageId()), "消息"), TMsg.class);
        TAccount account = copy(required(mapper(TAccountMapper.class).selectByPrimaryKey(task.getAccountId()), "账号"), TAccount.class);
        TPeople people = copy(required(mapper(TPeopleMapper.class).selectByPrimaryKey(task.getPeopleId()), "人群"), TPeople.class);
        if (!Objects.equals(task.getAccountId(), message.getAccountId()) || !Objects.equals(task.getMsgType(), message.getMsgType())
                || !Objects.equals(task.getAccountId(), people.getAccountId()) || !Objects.equals(task.getMsgType(), people.getMsgType())) {
            throw new IllegalArgumentException("任务引用的消息、人群与账号不匹配，请在 Classic 中修正");
        }
        List<TPeopleData> rows = mapper(TPeopleDataMapper.class).selectByPeopleId(task.getPeopleId()).stream()
                .map(row -> copy(row, TPeopleData.class)).sorted(Comparator.comparing(TPeopleData::getId)).toList();
        return new Resources(task, message, account, people, rows);
    }

    private static String executionIssue(TTask task) {
        if (!Objects.equals(task.getTaskMode(), TaskModeEnum.FIX_THREAD_TASK_CODE)
                || !Objects.equals(task.getTaskPeriod(), TaskTypeEnum.MANUAL_TASK_CODE)) {
            return "AI 接入目前仅执行已保存的手动固定线程任务，其他任务请在 Classic 中运行";
        }
        if (Objects.equals(task.getResultAlert(), 1)) {
            return "请先在 Classic 中关闭此任务的结果邮件提醒；AI 接入暂不执行额外的提醒邮件";
        }
        if (task.getThreadCnt() == null || task.getThreadCnt() < 1 || task.getThreadCnt() > 100) {
            return "AI 接入的任务线程数须为 1～100，请在 Classic 中调整";
        }
        if (Objects.equals(task.getIntervalPush(), 1) && (task.getIntervalTime() == null || task.getIntervalTime() < 0)) {
            return "任务间隔时间无效，请在 Classic 中调整";
        }
        return null;
    }

    private static JsonObject taskSummary(TTask task) {
        return AiJson.object("taskId", task.getId(), "title", task.getTitle(), "channel", MessageTypeEnum.getName(task.getMsgType()),
                "taskMode", task.getTaskMode(), "taskPeriod", task.getTaskPeriod(), "executionIssue", executionIssue(task));
    }

    private static JsonObject historySummary(TTaskHis his) {
        return AiJson.object("historyId", his.getId(), "taskId", his.getTaskId(), "dryRun", his.getDryRun(),
                "status", his.getStatus(), "totalCount", his.getTotalCnt(), "successCount", his.getSuccessCnt(),
                "failCount", his.getFailCnt(), "startTime", his.getStartTime(), "endTime", his.getEndTime());
    }

    private static JsonObject page(List<JsonObject> all, int offset, int limit) {
        int start = Math.min(offset, all.size());
        int end = (int) Math.min((long) start + limit, all.size());
        return AiJson.object("items", all.subList(start, end), "total", all.size(), "offset", offset,
                "nextOffset", end < all.size() ? end : null);
    }

    private static JsonElement redact(JsonElement source) {
        if (source.isJsonObject()) {
            JsonObject result = new JsonObject();
            source.getAsJsonObject().entrySet().forEach(entry -> result.add(entry.getKey(),
                    entry.getKey().toLowerCase(Locale.ROOT).matches(".*(password|secret|token|authorization|cookie|header|webhook|credential).*" )
                            ? new JsonPrimitive("[已隐藏]") : redact(entry.getValue())));
            return result;
        }
        if (source.isJsonArray()) {
            JsonArray result = new JsonArray();
            source.getAsJsonArray().forEach(value -> result.add(redact(value)));
            return result;
        }
        return source.deepCopy();
    }

    private static <T> T copy(T value, Class<T> type) { return AiJson.GSON.fromJson(AiJson.GSON.toJson(value), type); }
    private static <T> T required(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + "不存在");
        return value;
    }
}
