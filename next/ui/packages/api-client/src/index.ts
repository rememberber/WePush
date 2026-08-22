export interface SystemInfo {
  product: string;
  version: string;
  mode: string;
  serverTime: string;
}

export interface ProviderLinks {
  accountSchema: string;
  messageSchema: string;
  recipientSchema: string;
}

export interface ProviderSummary {
  providerId: string;
  displayName: string;
  implementationVersion: string;
  capabilities: string[];
  maximumConcurrency: number;
  links: ProviderLinks;
}

export interface HealthResponse {
  status: string;
}

export interface DebugResponse {
  status: number;
  statusText: string;
  durationMs: number;
  headers: Record<string, string>;
  body: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly responseBody: string;

  constructor(status: number, responseBody: string) {
    super(`WePush Service returned HTTP ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.responseBody = responseBody;
  }
}

export class WePushClient {
  readonly baseUrl: string;

  constructor(baseUrl = defaultBaseUrl()) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
  }

  systemInfo(signal?: AbortSignal): Promise<SystemInfo> {
    return this.getJson<SystemInfo>("/api/v1/system/info", signal);
  }

  health(signal?: AbortSignal): Promise<HealthResponse> {
    return this.getJson<HealthResponse>("/actuator/health", signal);
  }

  providers(signal?: AbortSignal): Promise<ProviderSummary[]> {
    return this.getJson<ProviderSummary[]>("/api/v1/providers", signal);
  }

  providerSchema(path: string, signal?: AbortSignal): Promise<Record<string, unknown>> {
    return this.getJson<Record<string, unknown>>(path, signal);
  }

  openApi(signal?: AbortSignal): Promise<string> {
    return this.requestText("/openapi.yaml", signal);
  }

  async debugGet(path: string, signal?: AbortSignal): Promise<DebugResponse> {
    const startedAt = performance.now();
    const response = await fetch(this.resolve(path), {
      method: "GET",
      headers: { Accept: "application/json, text/plain, */*" },
      signal,
    });
    const body = await response.text();
    return {
      status: response.status,
      statusText: response.statusText,
      durationMs: Math.round(performance.now() - startedAt),
      headers: Object.fromEntries(response.headers.entries()),
      body,
    };
  }

  private async getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
    const response = await fetch(this.resolve(path), {
      headers: { Accept: "application/json" },
      signal,
    });
    const body = await response.text();
    if (!response.ok) {
      throw new ApiError(response.status, body);
    }
    return JSON.parse(body) as T;
  }

  private async requestText(path: string, signal?: AbortSignal): Promise<string> {
    const response = await fetch(this.resolve(path), { signal });
    const body = await response.text();
    if (!response.ok) {
      throw new ApiError(response.status, body);
    }
    return body;
  }

  private resolve(path: string): string {
    if (/^https?:\/\//.test(path)) {
      return path;
    }
    return `${this.baseUrl}${path.startsWith("/") ? path : `/${path}`}`;
  }
}

function defaultBaseUrl(): string {
  if (typeof window !== "undefined" && window.location.protocol === "file:") {
    return "http://127.0.0.1:18990";
  }
  return "";
}
