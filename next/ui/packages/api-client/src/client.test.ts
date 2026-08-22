import { afterEach, describe, expect, it, vi } from "vitest";

import { ApiError, WePushClient } from "./index";

describe("WePushClient", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("normalizes the base URL and reads system information", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ product: "WePush Next", version: "0.1.0", mode: "standalone", serverTime: "now" })),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await new WePushClient("http://localhost:18990/").systemInfo();

    expect(result.product).toBe("WePush Next");
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:18990/api/v1/system/info",
      expect.objectContaining({ headers: { Accept: "application/json" } }),
    );
  });

  it("preserves the error response for diagnostics", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("not found", { status: 404 })));

    const error = await new WePushClient().providers().catch((caught: unknown) => caught);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 404, responseBody: "not found" });
  });

  it("sends idempotent run creation with the required header", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ id: "run_1" }), { status: 202 }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await new WePushClient("http://localhost:18990")
      .createRun("job_1", { dryRun: true }, "manual-1");

    expect(result.id).toBe("run_1");
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:18990/api/v1/workspaces/ws_default/jobs/job_1/runs",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ "Idempotency-Key": "manual-1" }),
      }),
    );
  });

  it("pages run item results with a cursor", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      items: [{ runId: "run_1", itemId: "alice", state: "SUCCEEDED" }],
      page: { nextCursor: "next.abc", hasMore: true },
    })));
    vi.stubGlobal("fetch", fetchMock);

    const result = await new WePushClient("http://localhost:18990")
      .runItems("run_1", "cursor.abc", 25);

    expect(result.items[0]?.itemId).toBe("alice");
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:18990/api/v1/workspaces/ws_default/runs/run_1/items?limit=25&cursor=cursor.abc",
      expect.objectContaining({ headers: { Accept: "application/json" } }),
    );
  });

  it("submits idempotent live run commands", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      commandId: "cmd_1", status: "ACCEPTED", code: "RUN_PAUSED",
    }), { status: 202 }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await new WePushClient("http://localhost:18990")
      .pauseRun("run_1", "pause-1");

    expect(result.code).toBe("RUN_PAUSED");
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:18990/api/v1/workspaces/ws_default/runs/run_1/commands/pause",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({ "Idempotency-Key": "pause-1" }),
      }),
    );
  });
});
