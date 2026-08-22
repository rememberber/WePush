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
});
