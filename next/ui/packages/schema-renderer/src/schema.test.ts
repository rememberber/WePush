import { describe, expect, it } from "vitest";

import { defaultsForSchema } from "./index";

describe("defaultsForSchema", () => {
  it("returns defensive copies of declared defaults", () => {
    const schema = {
      type: "object" as const,
      properties: {
        timeout: { type: "string" as const, default: "PT5S" },
        headers: { type: "object" as const, default: { "X-App": "WePush" } },
        requiredWithoutDefault: { type: "string" as const },
      },
    };

    const first = defaultsForSchema(schema);
    (first.headers as Record<string, string>)["X-App"] = "changed";

    expect(defaultsForSchema(schema)).toEqual({ timeout: "PT5S", headers: { "X-App": "WePush" } });
  });
});
