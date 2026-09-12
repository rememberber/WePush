import { randomUUID } from "node:crypto";
import { z } from "zod";
import { connection } from "./config.mjs";

const id = z.string().regex(/^[A-Za-z0-9_-][A-Za-z0-9._-]{0,127}$/);
const name = z.string().trim().min(1).max(200);
const document = z.record(z.string(), z.unknown());
const resource = z.enum(["accounts", "messages", "audiences", "jobs", "runs"]);
const page = { cursor: z.string().max(2048).optional(), limit: z.number().int().min(1).max(100).default(25) };
const key = z.string().min(1).max(128).regex(/^[\x21-\x7e]+$/)
  .describe("为本次操作生成唯一键（例如 UUID）；超时后重试必须复用原键，不得换键重新发送。");

export function createTools(options, fetcher = fetch) {
  const config = connection(options);
  const workspacePath = `/api/v1/workspaces/${config.workspace}`;
  const tools = new Map();
  async function request(path, method = "GET", body, idempotencyKey) {
    const headers = { Accept: "application/json, application/schema+json" };
    if (options.token) headers.Authorization = `Bearer ${options.token}`;
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (idempotencyKey) headers["Idempotency-Key"] = idempotencyKey;
    let response;
    try {
      response = await fetcher(`${config.url}${path}`, { method, headers, redirect: "error",
        body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(30_000) });
    } catch {
      throw new Error(method === "GET" ? "无法连接 WePush Service；请检查地址和服务状态。"
        : "WePush 请求未取得响应，操作结果未知。先查询状态；重试发送必须使用同一 Idempotency-Key。");
    }
    // Keep credentials, channel configuration and server stack traces out of error text.
    if (!response.ok) {
      const hint = response.status === 401 ? "请在 AI 客户端进程环境中设置 WEPUSH_API_TOKEN。"
        : response.status === 403 ? "当前 Token 没有此 Workspace 的操作权限。"
          : response.status === 409 ? "资源状态或幂等键冲突；请查询已有操作结果。"
            : "请在 WePush 中检查资源、参数和确认令牌；不要自动重复创建或发送。";
      await response.body?.cancel();
      throw new Error(`WePush HTTP ${response.status}。${hint}`);
    }
    if (response.status === 204) return { ok: true };
    return response.json();
  }
  const ws = (path, method, body, idempotencyKey) => request(workspacePath + path, method, body, idempotencyKey);
  function add(name, description, shape, readOnly, handler, destructive = false) {
    const schema = z.strictObject(shape);
    tools.set(name, { name, description, schema, annotations: { readOnlyHint: readOnly,
      destructiveHint: destructive, idempotentHint: readOnly, openWorldHint: true },
      call: (args) => handler(schema.parse(args)) });
  }
  add("wepush_system_info", "检查 WePush Service 连接和版本，返回当前连接的 Workspace。", {}, true,
    async () => ({ ...await request("/api/v1/system/info"), workspaceId: config.workspace }));
  add("wepush_list_providers", "列出可用渠道、精确实现版本和 Schema 地址。", {}, true,
    () => request("/api/v1/providers"));
  add("wepush_provider_schema", "创建资源前读取渠道的 account/message/recipient Schema，不要猜测字段或版本。",
    { providerId: id, providerVersion: id, kind: z.enum(["account", "message", "recipient"]) }, true,
    (a) => request(`/api/v1/providers/${a.providerId}/versions/${a.providerVersion}/schemas/${a.kind}`));
  add("wepush_list_resources", "分页查询当前 Workspace 的账号、消息、受众、任务或运行；保留 nextCursor 继续翻页。",
    { resource, ...page, name: z.string().max(200).optional(), status: z.string().max(40).optional() }, true,
    ({ resource, ...filters }) => ws(`/${resource}?${new URLSearchParams(Object.entries(filters).filter(([, v]) => v !== undefined))}`));
  add("wepush_get_resource", "按 ID 读取资源。发送前核对 job 的 messageId、accountId、audienceId 和消息内容。",
    { resource, id }, true, (a) => ws(`/${a.resource}/${a.id}`));
  add("wepush_create_message", "创建消息草稿，不发送。content 必须符合 Provider Schema。",
    { name, providerId: id, providerVersion: id, content: document }, false, (a) => ws("/messages", "POST", a));
  add("wepush_create_audience", "创建最多 1000 条的受众，不发送。大批量导入请使用 WePush 的 CSV/TXT 导入。",
    { name, recipients: z.array(z.strictObject({ itemId: id, fields: document })).min(1).max(1000) }, false,
    (a) => ws("/audiences", "POST", a));
  add("wepush_create_job", "使用已配置的渠道账号、消息和受众创建任务，不自动发送或调度。",
    { name, accountId: id, messageId: id, audienceId: id, policies: document.optional(), enabled: z.boolean().default(true) }, false,
    (a) => ws("/jobs", "POST", a));
  add("wepush_dry_run", "执行 Dry Run，不实际发送。返回 Run 后查询状态与结果。",
    { jobId: id, idempotencyKey: key }, false,
    (a) => ws(`/jobs/${a.jobId}/runs`, "POST", { dryRun: true, reason: "ai-dry-run" }, a.idempotencyKey));
  add("wepush_prepare_run", "预览正式发送：返回账号、受众规模、策略、有效期和确认令牌，不发送。核对消息内容和用户授权后才可调用 send_run。",
    { jobId: id }, false, async (a) => ({ ...await ws(`/jobs/${a.jobId}/run-confirmation`, "POST", {}), idempotencyKey: randomUUID() }));
  add("wepush_send_run", "正式向收件人发送消息。仅用于用户已明确授权的内容、渠道和受众；先 prepare_run。超时不得换幂等键重试。",
    { jobId: id, confirmationToken: z.string().min(1).max(8192), idempotencyKey: key,
      userConfirmed: z.literal(true).describe("用户已明确授权本次实际发送；预览、测试或准备请求不构成发送授权。") }, false,
    (a) => ws(`/jobs/${a.jobId}/runs`, "POST", { dryRun: false, reason: "ai-confirmed-send", confirmationToken: a.confirmationToken }, a.idempotencyKey), true);
  add("wepush_run_results", "分页查询 Run 的逐条结果；UNKNOWN 表示渠道结果未知，不等同失败，不自动重发。",
    { runId: id, ...page }, true, ({ runId, ...filters }) => ws(`/runs/${runId}/items?${new URLSearchParams(Object.entries(filters).filter(([, v]) => v !== undefined))}`));
  add("wepush_run_command", "暂停、恢复或取消指定 Run。仅执行用户要求的操作，取消不撤回已经发送的消息。",
    { runId: id, command: z.enum(["pause", "resume", "cancel"]), idempotencyKey: key }, false,
    (a) => ws(`/runs/${a.runId}/commands/${a.command}`, "POST", {}, a.idempotencyKey), true);
  return tools;
}
