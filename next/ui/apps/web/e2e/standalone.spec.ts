import { expect, test, type Route } from "@playwright/test";

const now = "2026-08-28T08:00:00Z";

test.beforeEach(async ({ page }) => {
  await page.route("**/api/v1/**", async (route) => respond(route));
});

test("loads the self-hosted workspace, discovers Providers and keeps browser tokens session-only", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByText("开始使用 WePush Next")).toBeVisible();
  await expect(page.getByText("Service 已连接")).toBeVisible();
  await expect(page.getByText("Standalone", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "Providers" }).click();
  await expect(page.getByRole("heading", { name: "Providers", level: 2 })).toBeVisible();
  await expect(page.getByText("Local HTTP", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("浏览器不会获得本机文件或服务管理权限")).toBeVisible();

  await page.getByRole("button", { name: "设置" }).click();
  await page.getByLabel("Bearer Token").fill("browser-session-token");
  await page.getByRole("button", { name: "保存并验证" }).click();
  await expect.poll(() => page.evaluate(() => sessionStorage.getItem("wepush.apiToken"))).toBe("browser-session-token");
  await expect.poll(() => page.evaluate(() => localStorage.getItem("wepush.apiToken"))).toBeNull();
  await page.reload();
  await page.getByRole("button", { name: "设置" }).click();
  await expect(page.getByLabel("Bearer Token")).toHaveValue("browser-session-token");
});

test("browser offers a downloadable AI installer and platform-specific command", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "设置" }).click();
  await expect(page.getByRole("heading", { name: "AI 助手接入" })).toBeVisible();
  await expect(page.getByRole("button", { name: "一键接入 Codex" })).toHaveCount(0);
  await page.getByLabel("AI 助手连接的 Service URL").fill("https://push.example.com/wepush");
  await expect(page.locator(".ai-integration-output")).toContainText("--url 'https://push.example.com/wepush' --workspace 'ws_default'");
  await page.getByRole("combobox", { name: "终端", exact: true }).selectOption("powershell");
  await expect(page.locator(".ai-integration-output")).toContainText("--target codex");
  const download = page.waitForEvent("download");
  await page.getByRole("link", { name: "下载安装器" }).click();
  expect((await download).suggestedFilename()).toBe("wepush-ai.mjs");
  const response = await page.request.get("/ai/wepush-ai.mjs");
  expect(response.status()).toBe(200);
  expect(await response.text()).toContain("wepush_send_run");
});

test("desktop one-click entry passes the selected connection and reports install failures", async ({ page }) => {
  await page.addInitScript(() => {
    const state = window as unknown as { wepushDesktop: unknown; aiInstallCalls: unknown[] };
    state.aiInstallCalls = [];
    state.wepushDesktop = {
      platform: "darwin", versions: { chrome: "test", electron: "test" },
      token: { load: async () => "", save: async () => {}, clear: async () => {} },
      service: { status: async () => ({ installed: true, running: true, platform: "darwin", detail: "test" }) },
      ai: { install: async (target: string, url: string, workspace: string) => {
        state.aiInstallCalls.push({ target, url, workspace });
        return target === "skill" ? { ok: false, message: "已存在自定义 Skill；不会覆盖", output: "" }
          : { ok: true, message: "已安装 Codex MCP 和 WePush Skill", output: "/test/.codex/config.toml" };
      } },
    };
  });
  await page.goto("/");
  await page.getByRole("button", { name: "设置" }).click();
  await page.getByLabel("AI 助手连接的 Service URL").fill("http://127.0.0.1:18990");
  await page.getByRole("button", { name: "一键接入 Codex" }).click();
  await expect(page.getByRole("status")).toContainText("已安装 Codex MCP 和 WePush Skill");
  expect(await page.evaluate(() => (window as unknown as { aiInstallCalls: unknown[] }).aiInstallCalls)).toEqual([
    { target: "codex", url: "http://127.0.0.1:18990", workspace: "ws_default" },
  ]);
  await page.getByRole("button", { name: "仅安装 Skill", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("已存在自定义 Skill；不会覆盖");
});

async function respond(route: Route): Promise<void> {
  const url = new URL(route.request().url());
  const path = url.pathname;
  let body: unknown;
  if (path === "/api/v1/system/info") {
    body = { product: "WePush Next", version: "1.0.0", mode: "standalone", serverTime: now };
  } else if (path === "/api/v1/providers") {
    body = [{ providerId: "http", displayName: "Local HTTP", implementationVersion: "1.0.0", capabilities: ["DRY_RUN"], maximumConcurrency: 16,
      links: { accountSchema: "/api/v1/schemas/http-account", messageSchema: "/api/v1/schemas/http-message", recipientSchema: "/api/v1/schemas/http-recipient" } }];
  } else if (path === "/api/v1/schemas/http-account") {
    body = { type: "object", properties: { baseUrl: { type: "string", title: "Base URL", default: "http://127.0.0.1:8080" } }, required: ["baseUrl"] };
  } else if (path === "/api/v1/agents") {
    body = [];
  } else if (path === "/api/v1/workspaces") {
    body = [{ id: "ws_default", name: "Local workspace", status: "ACTIVE", createdAt: now, version: 1 }];
  } else if (path.endsWith("/overview")) {
    body = { activeRuns: 0, totalRuns: 0, succeededRuns: 0, problemRuns: 0, active: [], recent: [], trend: [] };
  } else if (path.endsWith("/api-tokens")) {
    body = [];
  } else if (path.includes("/audit-events")) {
    body = { items: [], page: { hasMore: false } };
  } else {
    body = { items: [], page: { hasMore: false } };
  }
  await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(body) });
}
