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

async function respond(route: Route): Promise<void> {
  const url = new URL(route.request().url());
  const path = url.pathname;
  let body: unknown;
  if (path === "/api/v1/system/info") {
    body = { product: "WePush Next", version: "0.1.0-beta.1", mode: "standalone", serverTime: now };
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
