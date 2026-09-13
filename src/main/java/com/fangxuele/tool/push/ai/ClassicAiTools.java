package com.fangxuele.tool.push.ai;

import com.google.gson.*;

import java.util.Set;

/** Shared by the desktop bridge and the headless stdio client; never loads App or its database. */
public final class ClassicAiTools {
    public static final String PREFIX = "wepush_classic_";
    private static final JsonArray TOOLS = new JsonArray();

    static {
        add("system_info", "查询正在运行的 WePush Classic 与接入能力。", true, props(), "");
        add("list_tasks", "分页列出 Classic 已保存的任务。", true, pageProps(), "");
        add("get_task", "查看任务、消息模板、账号名称和人群概览，不返回账号凭据。", true, taskProps(), "taskId");
        add("prepare_run", "准备正式发送：返回任务预览及有效期 10 分钟的确认令牌，不发送消息。", true, taskProps(), "taskId");
        add("dry_run", "空跑已保存的手动固定线程任务，复用 Classic 渠道校验，不投递消息。用 UUID 防止重复启动。", false,
                runProps(false), "taskId,idempotencyKey");
        add("send_run", "真实发送：先 prepare_run，确保用户授权覆盖预览内容和人群，再传入确认令牌及 userConfirmed=true。相同 UUID 重试不重复发送。", false,
                runProps(true), "taskId,idempotencyKey,confirmationToken,userConfirmed");
        JsonObject run = props();
        run.add("runId", AiJson.object("type", "string", "format", "uuid"));
        add("get_run", "查询运行进度。UNKNOWN 表示上次进程退出后结果不明，禁止自动换 UUID 重发。", true, run, "runId");
        JsonObject history = pageProps();
        history.add("taskId", taskProps().get("taskId"));
        add("list_history", "分页查询指定任务的 Classic 历史记录。", true, history, "taskId");
    }

    private ClassicAiTools() { }

    public static JsonArray list() {
        return TOOLS.deepCopy();
    }

    static String validate(String name, JsonObject args) {
        JsonObject tool = null;
        for (JsonElement candidate : TOOLS) {
            if (candidate.getAsJsonObject().get("name").getAsString().equals(name)) {
                tool = candidate.getAsJsonObject();
                break;
            }
        }
        if (tool == null) throw new IllegalArgumentException("未知工具：" + name);
        JsonObject schema = tool.getAsJsonObject("inputSchema");
        JsonObject properties = schema.getAsJsonObject("properties");
        for (String key : args.keySet()) {
            if (!properties.has(key)) throw new IllegalArgumentException("不支持参数：" + key);
            JsonElement value = args.get(key);
            if (!value.isJsonPrimitive()) throw new IllegalArgumentException("参数类型错误：" + key);
            String type = properties.getAsJsonObject(key).get("type").getAsString();
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            switch (type) {
                case "integer" -> {
                    if (!primitive.isNumber()) throw new IllegalArgumentException("参数必须为整数：" + key);
                    int number;
                    try { number = primitive.getAsBigDecimal().intValueExact(); }
                    catch (ArithmeticException e) { throw new IllegalArgumentException("参数必须为整数：" + key); }
                    int minimum = key.equals("offset") ? 0 : 1;
                    if (number < minimum || (key.equals("limit") && number > 100)) {
                        throw new IllegalArgumentException("参数超出范围：" + key);
                    }
                }
                case "boolean" -> {
                    if (!primitive.isBoolean()) throw new IllegalArgumentException("参数必须为布尔值：" + key);
                }
                default -> {
                    if (!primitive.isString() || value.getAsString().isBlank() || value.getAsString().length() > 128) {
                        throw new IllegalArgumentException("参数必须为有效字符串：" + key);
                    }
                    if (Set.of("runId", "idempotencyKey", "confirmationToken").contains(key)) {
                        uuid(value.getAsString());
                    }
                }
            }
        }
        for (JsonElement key : schema.getAsJsonArray("required")) {
            if (!args.has(key.getAsString())) throw new IllegalArgumentException("缺少参数：" + key.getAsString());
        }
        return name.substring(PREFIX.length());
    }

    static String uuid(String value) {
        if (!java.util.UUID.fromString(value).toString().equals(value)) {
            throw new IllegalArgumentException("请使用小写标准 UUID");
        }
        return value;
    }

    private static JsonObject props() { return new JsonObject(); }

    private static JsonObject taskProps() {
        return AiJson.object("taskId", AiJson.object("type", "integer", "minimum", 1));
    }

    private static JsonObject pageProps() {
        return AiJson.object("offset", AiJson.object("type", "integer", "minimum", 0, "default", 0),
                "limit", AiJson.object("type", "integer", "minimum", 1, "maximum", 100, "default", 30));
    }

    private static JsonObject runProps(boolean send) {
        JsonObject result = taskProps();
        result.add("idempotencyKey", AiJson.object("type", "string", "format", "uuid"));
        if (send) {
            result.add("confirmationToken", AiJson.object("type", "string", "format", "uuid"));
            result.add("userConfirmed", AiJson.object("type", "boolean", "const", true));
        }
        return result;
    }

    private static void add(String name, String description, boolean readOnly, JsonObject properties, String required) {
        JsonArray requiredKeys = new JsonArray();
        if (!required.isEmpty()) for (String key : required.split(",")) requiredKeys.add(key);
        TOOLS.add(AiJson.object("name", PREFIX + name, "description", description,
                "inputSchema", AiJson.object("type", "object", "properties", properties,
                        "required", requiredKeys, "additionalProperties", false),
                "annotations", AiJson.object("readOnlyHint", readOnly, "destructiveHint", name.equals("send_run"),
                        "idempotentHint", true, "openWorldHint", !readOnly)));
    }
}
