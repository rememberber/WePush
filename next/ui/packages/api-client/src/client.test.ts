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

  it("routes packaged Desktop requests to the local Service", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      product: "WePush Next", version: "0.1.0", mode: "standalone", serverTime: "now",
    })));
    vi.stubGlobal("window", { location: { protocol: "wepush:" } });
    vi.stubGlobal("fetch", fetchMock);

    await new WePushClient().systemInfo();

    expect(fetchMock).toHaveBeenCalledWith(
      "http://127.0.0.1:18990/api/v1/system/info",
      expect.any(Object),
    );
  });

  it("preserves the error response for diagnostics", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("not found", { status: 404 })));

    const error = await new WePushClient().providers().catch((caught: unknown) => caught);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 404, responseBody: "not found" });
  });

  it("never sends the bearer token to a cross-origin debug URL", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    const error = await new WePushClient("http://127.0.0.1:18990", "secret-token")
      .debugRequest("https://example.invalid/collect")
      .catch((caught: unknown) => caught);

    expect(error).toBeInstanceOf(Error);
    expect(String(error)).toContain("Cross-origin");
    expect(fetchMock).not.toHaveBeenCalled();
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

  it("creates and downloads result exports with authentication", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({
        id: "artifact_1", type: "RUN_RESULTS_CSV", state: "READY",
      }), { status: 201 }))
      .mockResolvedValueOnce(new Response("item,state\nalice,SUCCEEDED\n", {
        headers: { "Content-Type": "text/csv" },
      }));
    vi.stubGlobal("fetch", fetchMock);

    const client = new WePushClient("http://localhost:18990/", "secret-token");
    const artifact = await client.createResultExport("run_1");
    const download = await client.downloadArtifact(artifact.id);

    expect(artifact.id).toBe("artifact_1");
    expect(await download.text()).toBe("item,state\nalice,SUCCEEDED\n");
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:18990/api/v1/workspaces/ws_default/runs/run_1/artifacts/result-export",
      expect.objectContaining({
        method: "POST",
        body: "{}",
        headers: expect.objectContaining({ Authorization: "Bearer secret-token" }),
      }),
    );
    expect(fetchMock).toHaveBeenLastCalledWith(
      "http://localhost:18990/api/v1/workspaces/ws_default/artifacts/artifact_1/content",
      expect.objectContaining({
        headers: {
          Accept: "application/octet-stream",
          Authorization: "Bearer secret-token",
        },
      }),
    );
  });
});
