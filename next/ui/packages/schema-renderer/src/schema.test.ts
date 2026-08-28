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

  it("resolves local definitions and builds nested provider examples", () => {
    const schema = {
      type: "object" as const,
      properties: { secret: { $ref: "#/$defs/secretRef" } },
      $defs: {
        secretRef: {
          type: "object" as const,
          properties: {
            namespace: { type: "string" as const, default: "smtp" },
            name: { type: "string" as const },
            version: { type: "string" as const, default: "v1" },
          },
        },
      },
    };

    expect(defaultsForSchema(schema)).toEqual({ secret: { namespace: "smtp", version: "v1" } });
  });
});
