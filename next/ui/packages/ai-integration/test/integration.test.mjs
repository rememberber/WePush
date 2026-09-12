import { after, before, test } from "node:test";
import assert from "node:assert/strict";
import { createServer } from "node:http";
import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { mkdtemp, mkdir, readFile, rm, writeFile, stat } from "node:fs/promises";
import { tmpdir } from "node:os";
import { resolve, join } from "node:path";
import { parse } from "smol-toml";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { createTools } from "../src/tools.mjs";
import { connection } from "../src/config.mjs";
import { codexConfig, install } from "../src/install.mjs";

const execute = promisify(execFile);
const requests = [];
let server, url, root, skill;
const entry = resolve("dist/wepush-ai.mjs");
before(async () => {
  await execute(process.execPath, ["build.mjs"]);
  root = await mkdtemp(join(tmpdir(), "wepush-ai-test-"));
  skill = await readFile("skill/wepush/SKILL.md", "utf8");
  server = createServer(async (req, res) => {
    let body = "";
    for await (const chunk of req) body += chunk;
    requests.push({ path: req.url, method: req.method, headers: req.headers, body: body ? JSON.parse(body) : undefined });
    res.setHeader("Content-Type", "application/json");
    if (req.url.endsWith("/run-confirmation")) res.end(JSON.stringify({ jobId: "job_1", confirmationToken: "confirmation", audienceCount: 2 }));
    else if (req.url.endsWith("/jobs/job_1/runs")) res.end(JSON.stringify({ id: "run_1", dryRun: JSON.parse(body).dryRun, state: "QUEUED" }));
    else if (req.url.includes("/items?")) res.end(JSON.stringify({ items: [{ state: "UNKNOWN" }], page: { hasMore: false } }));
    else if (req.url.includes("/messages?")) res.end(JSON.stringify({ items: [{ id: "msg_1" }], page: { hasMore: true, nextCursor: "next+page" } }));
    else res.end(JSON.stringify({ product: "WePush Next", version: "1.1.0" }));
  });
  await new Promise((done) => server.listen(0, "127.0.0.1", done));
  url = `http://127.0.0.1:${server.address().port}`;
});
after(async () => {
  await new Promise((done) => server.close(done));
  await rm(root, { recursive: true, force: true });
});

test("MCP SDK client negotiates stdio, discovers tools and performs preview/send/results", async () => {
  const client = new Client({ name: "wepush-test", version: "1" });
  const transport = new StdioClientTransport({ command: process.execPath, args: [entry, "mcp"],
    env: { WEPUSH_SERVICE_URL: url, WEPUSH_WORKSPACE_ID: "ws_test", WEPUSH_API_TOKEN: "private-token" }, stderr: "pipe" });
  try {
    await client.connect(transport);
    const catalog = await client.listTools();
    assert.equal(catalog.tools.length, 13);
    assert.equal(catalog.tools.find((t) => t.name === "wepush_send_run").annotations.destructiveHint, true);
    const call = async (name, args = {}) => {
      const result = await client.callTool({ name, arguments: args });
      assert.notEqual(result.isError, true, JSON.stringify(result));
      return JSON.parse(result.content[0].text);
    };
    assert.equal((await call("wepush_system_info")).workspaceId, "ws_test");
    const page = await call("wepush_list_resources", { resource: "messages", limit: 1, cursor: "a/b+=" });
    assert.equal(page.page.nextCursor, "next+page");
    assert.match(requests.at(-1).path, /cursor=a%2Fb%2B%3D/);
    await call("wepush_dry_run", { jobId: "job_1", idempotencyKey: "dry-1" });
    assert.equal(requests.at(-1).body.dryRun, true);
    const preview = await call("wepush_prepare_run", { jobId: "job_1" });
    assert.equal(preview.audienceCount, 2);
    const args = { jobId: "job_1", confirmationToken: preview.confirmationToken, idempotencyKey: preview.idempotencyKey, userConfirmed: true };
    const beforeInvalid = requests.length;
    assert.equal((await client.callTool({ name: "wepush_send_run", arguments: { ...args, userConfirmed: false } })).isError, true);
    assert.equal(requests.length, beforeInvalid);
    assert.equal((await call("wepush_send_run", args)).dryRun, false);
    await call("wepush_send_run", args);
    assert.equal(requests.at(-1).headers["idempotency-key"], preview.idempotencyKey);
    assert.equal(requests.at(-1).headers.authorization, "Bearer private-token");
    assert.equal(requests.at(-1).path, "/api/v1/workspaces/ws_test/jobs/job_1/runs");
    assert.equal((await call("wepush_run_results", { runId: "run_1" })).items[0].state, "UNKNOWN");
    await call("wepush_create_message", { name: "notification", providerId: "http", providerVersion: "1.1.0", content: { text: "hello" } });
    assert.deepEqual(requests.at(-1).body.content, { text: "hello" });
    await call("wepush_create_audience", { name: "recipients", recipients: [{ itemId: "item_1", fields: { address: "example" } }] });
    assert.match(requests.at(-1).path, /\/audiences$/);
    await call("wepush_create_job", { name: "draft", accountId: "acc_1", messageId: "msg_1", audienceId: "aud_1" });
    assert.equal(requests.at(-1).body.enabled, true);
  } finally { await client.close(); }
});

test("validates paths, bounds pages and redacts authentication failures without retries", async () => {
  for (const value of ["file:///tmp/x", "https://user:pass@example.com", "http://remote.example", "https://example.com?token=secret"]) {
    assert.throws(() => connection({ url: value }));
  }
  assert.throws(() => connection({ workspace: "../outside" }));
  let count = 0;
  const tools = createTools({ url, token: "secret" }, async (_url, options) => {
    count++;
    assert.equal(options.redirect, "error");
    assert.ok(options.signal);
    return new Response("server secret=do-not-print", { status: 401 });
  });
  await assert.rejects(async () => tools.get("wepush_get_resource").call({ resource: "jobs", id: ".." }));
  await assert.rejects(async () => tools.get("wepush_list_resources").call({ resource: "jobs", limit: 101 }));
  assert.equal(count, 0);
  await assert.rejects(() => tools.get("wepush_system_info").call({}), (error) => {
    assert.match(error.message, /401/); assert.doesNotMatch(error.message, /do-not-print/); return true;
  });
  assert.equal(count, 1);
});

test("rejects redirects rather than forwarding credentials", async () => {
  let forwarded = 0;
  const redirect = createServer((req, res) => {
    if (req.url === "/destination") { forwarded++; res.end("{}"); }
    else { res.writeHead(302, { Location: "/destination" }); res.end(); }
  });
  await new Promise((done) => redirect.listen(0, "127.0.0.1", done));
  try {
    const tools = createTools({ url: `http://127.0.0.1:${redirect.address().port}`, token: "private" });
    await assert.rejects(() => tools.get("wepush_system_info").call({}));
    assert.equal(forwarded, 0);
  } finally { await new Promise((done) => redirect.close(done)); }
});

test("one-click install preserves Codex settings, backs up and can update its own connection", async () => {
  const home = join(root, "home with spaces ' 引号");
  const codexHome = join(home, "custom-codex");
  await mkdir(codexHome, { recursive: true });
  const existing = '# user comment\nmodel = "custom"\n[mcp_servers.other]\ncommand = "keep-me"\n';
  await writeFile(join(codexHome, "config.toml"), existing);
  const result = await install({ entry, skill, home, codexHome, url, workspace: "ws_test" });
  assert.equal(await readFile(result.backup, "utf8"), existing);
  const config = await readFile(result.configPath, "utf8");
  assert.ok(config.startsWith(existing));
  assert.equal(parse(config).mcp_servers.other.command, "keep-me");
  assert.deepEqual(parse(config).mcp_servers.wepush.env_vars, ["WEPUSH_API_TOKEN"]);
  assert.doesNotMatch(config, /private-token/);
  await install({ entry, skill, home, codexHome, url, workspace: "ws_changed" });
  assert.equal(parse(await readFile(result.configPath, "utf8")).mcp_servers.wepush.env.WEPUSH_WORKSPACE_ID, "ws_changed");
  const repeated = await install({ entry, skill, home, codexHome, url, workspace: "ws_changed" });
  assert.equal(repeated.backup, undefined);
  // The installed Skill works without MCP, including a home path with spaces and quotes.
  const args = process.platform === "win32"
    ? ["powershell.exe", ["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", join(result.destination, "scripts/wepush-ai.ps1"), "call", "wepush_system_info"]]
    : [join(result.destination, "scripts/wepush-ai.sh"), ["call", "wepush_system_info"]];
  const child = execFile(args[0], args[1]);
  child.stdin.end("{}");
  const output = await new Promise((done, fail) => {
    let text = "", errors = "";
    child.stdout.on("data", (data) => { text += data; });
    child.stderr.on("data", (data) => { errors += data; });
    child.on("error", fail);
    child.on("close", (code) => code === 0 ? done(text) : fail(new Error(errors)));
  });
  assert.equal(JSON.parse(output).workspaceId, "ws_changed");
  if (process.platform !== "win32") assert.equal((await stat(result.configPath)).mode & 0o777, 0o600);
});

test("refuses foreign Skill/config and preserves unrelated settings inside damaged managed blocks", async () => {
  const home = join(root, "foreign");
  const folder = join(home, ".agents/skills/wepush");
  await mkdir(folder, { recursive: true });
  await writeFile(join(folder, "SKILL.md"), "custom skill");
  await assert.rejects(() => install({ entry, skill, home, codexHome: join(home, ".codex") }), /不会覆盖/);
  assert.equal(await readFile(join(folder, "SKILL.md"), "utf8"), "custom skill");
  assert.throws(() => codexConfig('[mcp_servers.wepush]\ncommand="custom"\n', { command: "new" }), /手动配置/);
  assert.throws(() => codexConfig('# BEGIN WEPUSH MANAGED MCP\n[mcp_servers.wepush]\ncommand="old"\n[mcp_servers.other]\ncommand="keep"\n# END WEPUSH MANAGED MCP', { command: "new" }), /其他 Codex 设置/);
});

test("generic export and Skill-only installation do not create Codex MCP settings", async () => {
  const home = join(root, "generic");
  const codexHome = join(home, ".codex");
  const result = await install({ entry, skill, home, codexHome, target: "generic", url });
  const config = JSON.parse(await readFile(join(result.destination, "mcp.json"), "utf8"));
  assert.equal(config.mcpServers.wepush.command, process.execPath);
  await install({ entry, skill, home, codexHome, target: "skill", url });
  await assert.rejects(() => stat(join(codexHome, "config.toml")), { code: "ENOENT" });
});
