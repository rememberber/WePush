// Runs only against a new temporary Service/database and a local HTTP fixture.
import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { createServer } from "node:http";
import { randomUUID } from "node:crypto";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { resolve, join } from "node:path";
import { setTimeout as delay } from "node:timers/promises";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const root = await mkdtemp(join(tmpdir(), "wepush-ai-service-"));
const token = randomUUID() + randomUUID();
let delivered = 0;
const provider = createServer((_request, response) => { delivered++; response.end("{}"); });
await new Promise((done) => provider.listen(0, "127.0.0.1", done));
const portReservation = createServer();
await new Promise((done) => portReservation.listen(0, "127.0.0.1", done));
const port = portReservation.address().port;
await new Promise((done) => portReservation.close(done));
const url = `http://127.0.0.1:${port}`;
const env = Object.fromEntries(Object.entries(process.env).filter(([key]) => !key.startsWith("WEPUSH_")));
const service = spawn("java", ["-jar", resolve("../../../service/service-app/target/wepush-next-service.jar"), "--server.shutdown=immediate"], {
  cwd: root, env: { ...env, WEPUSH_PORT: String(port), WEPUSH_AGENT_GRPC_PORT: "0", WEPUSH_SECURITY_ENABLED: "true", WEPUSH_BOOTSTRAP_TOKEN: token },
  stdio: ["ignore", "pipe", "pipe"],
});
const exited = new Promise((done) => service.once("close", done));
let logs = "";
service.stdout.on("data", (data) => { logs = (logs + data).slice(-20000); });
service.stderr.on("data", (data) => { logs = (logs + data).slice(-20000); });
service.on("error", (error) => { logs += error.message; });
const client = new Client({ name: "wepush-service-smoke", version: "1" });
try {
  let ready = false;
  for (let attempt = 0; attempt < 60; attempt++) {
    if (service.exitCode !== null) throw new Error(`Service exited: ${logs}`);
    try { ready = (await fetch(`${url}/actuator/health`, { signal: AbortSignal.timeout(1000) })).ok; } catch { /* wait for startup */ }
    if (ready) break;
    await delay(500);
  }
  assert.ok(ready, `Service not ready: ${logs}`);
  await client.connect(new StdioClientTransport({ command: process.execPath, args: [resolve("dist/wepush-ai.mjs"), "mcp"],
    env: { WEPUSH_SERVICE_URL: url, WEPUSH_API_TOKEN: token }, stderr: "pipe" }));
  const call = async (name, args = {}) => {
    const result = await client.callTool({ name, arguments: args });
    assert.notEqual(result.isError, true, JSON.stringify(result));
    return JSON.parse(result.content[0].text);
  };
  const providers = await call("wepush_list_providers");
  const http = providers.find((item) => item.providerId === "wepush.http");
  assert.ok(http);
  const identity = { providerId: http.providerId, providerVersion: http.implementationVersion };
  assert.equal((await call("wepush_provider_schema", { ...identity, kind: "message" })).type, "object");
  const accountResponse = await fetch(`${url}/api/v1/workspaces/ws_default/accounts`, {
    method: "POST", headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ ...identity, name: "AI smoke fixture", configuration: {
      baseUrl: `http://127.0.0.1:${provider.address().port}`, allowPrivateAddresses: true, auth: { type: "NONE" },
    } }),
  });
  assert.equal(accountResponse.status, 201);
  const account = await accountResponse.json();
  const message = await call("wepush_create_message", { ...identity, name: "AI smoke", content: { method: "POST", path: "/notify", bodyTemplate: "{{text}}" } });
  const audience = await call("wepush_create_audience", { name: "AI smoke", recipients: [{ itemId: "item_1", fields: { text: "fixture" } }] });
  const job = await call("wepush_create_job", { name: "AI smoke", accountId: account.id, messageId: message.id, audienceId: audience.id });
  const preview = await call("wepush_prepare_run", { jobId: job.id });
  assert.equal(preview.audienceCount, 1);
  assert.ok(preview.confirmationToken);
  const key = randomUUID();
  const run = await call("wepush_dry_run", { jobId: job.id, idempotencyKey: key });
  assert.equal((await call("wepush_dry_run", { jobId: job.id, idempotencyKey: key })).id, run.id);
  let state;
  for (let attempt = 0; attempt < 40; attempt++) {
    state = await call("wepush_get_resource", { resource: "runs", id: run.id });
    if (state.endedAt) break;
    await delay(250);
  }
  assert.equal(state.counters.succeeded, 1, JSON.stringify(state));
  const results = await call("wepush_run_results", { runId: run.id });
  assert.equal(results.items[0].state, "SUCCEEDED");
  assert.equal(delivered, 0, "Dry Run must not contact the delivery endpoint");
  console.log("Real Service MCP smoke passed: authenticated discovery, drafts, preview, idempotent Dry Run and results; zero deliveries.");
} finally {
  await client.close();
  service.kill("SIGTERM");
  const killed = setTimeout(() => service.kill("SIGKILL"), 5000);
  await exited;
  clearTimeout(killed);
  await new Promise((done) => provider.close(done));
  await rm(root, { recursive: true, force: true });
}
