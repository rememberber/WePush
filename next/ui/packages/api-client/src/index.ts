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

export interface ResourceMetadata {
  id: string;
  workspaceId: string;
  name: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface Account extends ResourceMetadata {
  providerId: string;
  providerVersion: string;
  configuration: Record<string, unknown>;
  status: "ACTIVE" | "DISABLED" | "ARCHIVED";
}

export interface Message extends ResourceMetadata {
  providerId: string;
  providerVersion: string;
  revision: number;
  schemaVersion: string;
  content: Record<string, unknown>;
  contentHash: string;
  status: "ACTIVE" | "DISABLED" | "ARCHIVED";
}

export interface Audience extends ResourceMetadata {
  snapshotId: string;
  revision: number;
  recordCount: number;
  contentHash: string;
  status: "ACTIVE" | "DISABLED" | "ARCHIVED";
}

export interface Job extends ResourceMetadata {
  accountId: string;
  messageId: string;
  audienceId: string;
  policies: Record<string, unknown>;
  enabled: boolean;
}

export interface RunCounters {
  total: number;
  succeeded: number;
  failed: number;
  unknown: number;
  unsent: number;
  skipped: number;
  retried: number;
}

export interface Run {
  id: string;
  workspaceId: string;
  jobId: string;
  state: string;
  stateReason: string;
  dryRun: boolean;
  counters: RunCounters;
  createdAt: string;
  startedAt?: string;
  endedAt?: string;
  updatedAt: string;
  version: number;
  links: Record<string, string>;
}

export interface RunItemResult {
  runId: string;
  itemId: string;
  attempts: number;
  state: "SUCCEEDED" | "FAILED" | "UNKNOWN" | "UNSENT" | "SKIPPED";
  providerCode: string;
  diagnostic: string;
  externalRequestId: string;
  completedAt: string;
  metadata: Record<string, unknown>;
}

export interface RunItemResultPage {
  items: RunItemResult[];
  page: { nextCursor?: string; hasMore: boolean };
}

export interface RunCommandResult {
  commandId: string;
  type: "PAUSE" | "RESUME" | "CANCEL" | "CONCURRENCY";
  status: "ACCEPTED" | "REJECTED";
  code: string;
  message: string;
  acknowledgedAt: string;
  replayed: boolean;
}

export interface SecretMetadata {
  workspaceId: string;
  namespace: string;
  name: string;
  secretVersion: string;
  configured: boolean;
  recordVersion: number;
  createdAt: string;
  updatedAt: string;
}

export interface RecipientInput {
  itemId?: string;
  fields: Record<string, unknown>;
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

  accounts(workspaceId = "ws_default", signal?: AbortSignal): Promise<Account[]> {
    return this.getJson<Account[]>(this.workspacePath(workspaceId, "/accounts"), signal);
  }

  createAccount(request: {
    name: string;
    providerId: string;
    providerVersion: string;
    configuration: Record<string, unknown>;
  }, workspaceId = "ws_default", signal?: AbortSignal): Promise<Account> {
    return this.postJson<Account>(this.workspacePath(workspaceId, "/accounts"), request, undefined, signal);
  }

  messages(workspaceId = "ws_default", signal?: AbortSignal): Promise<Message[]> {
    return this.getJson<Message[]>(this.workspacePath(workspaceId, "/messages"), signal);
  }

  createMessage(request: {
    name: string;
    providerId: string;
    providerVersion: string;
    content: Record<string, unknown>;
  }, workspaceId = "ws_default", signal?: AbortSignal): Promise<Message> {
    return this.postJson<Message>(this.workspacePath(workspaceId, "/messages"), request, undefined, signal);
  }

  audiences(workspaceId = "ws_default", signal?: AbortSignal): Promise<Audience[]> {
    return this.getJson<Audience[]>(this.workspacePath(workspaceId, "/audiences"), signal);
  }

  createAudience(request: { name: string; recipients: RecipientInput[] },
    workspaceId = "ws_default", signal?: AbortSignal): Promise<Audience> {
    return this.postJson<Audience>(this.workspacePath(workspaceId, "/audiences"), request, undefined, signal);
  }

  jobs(workspaceId = "ws_default", signal?: AbortSignal): Promise<Job[]> {
    return this.getJson<Job[]>(this.workspacePath(workspaceId, "/jobs"), signal);
  }

  createJob(request: {
    name: string;
    accountId: string;
    messageId: string;
    audienceId: string;
    policies?: Record<string, unknown>;
    enabled?: boolean;
  }, workspaceId = "ws_default", signal?: AbortSignal): Promise<Job> {
    return this.postJson<Job>(this.workspacePath(workspaceId, "/jobs"), request, undefined, signal);
  }

  runs(workspaceId = "ws_default", signal?: AbortSignal): Promise<Run[]> {
    return this.getJson<Run[]>(this.workspacePath(workspaceId, "/runs"), signal);
  }

  run(runId: string, workspaceId = "ws_default", signal?: AbortSignal): Promise<Run> {
    return this.getJson<Run>(this.workspacePath(workspaceId, `/runs/${pathId(runId)}`), signal);
  }

  createRun(jobId: string, request: {
    dryRun?: boolean;
    policyOverrides?: Record<string, unknown>;
    reason?: string;
  }, idempotencyKey: string, workspaceId = "ws_default", signal?: AbortSignal): Promise<Run> {
    return this.postJson<Run>(this.workspacePath(workspaceId, `/jobs/${pathId(jobId)}/runs`),
      request, idempotencyKey, signal);
  }

  runEventsUrl(runId: string, workspaceId = "ws_default"): string {
    return this.resolve(this.workspacePath(workspaceId, `/runs/${pathId(runId)}/events`));
  }

  runItems(runId: string, cursor?: string, limit = 100,
    workspaceId = "ws_default", signal?: AbortSignal): Promise<RunItemResultPage> {
    if (!Number.isInteger(limit) || limit < 1 || limit > 500) {
      throw new Error("Result page limit must be between 1 and 500");
    }
    const query = new URLSearchParams({ limit: String(limit) });
    if (cursor) query.set("cursor", cursor);
    return this.getJson<RunItemResultPage>(
      this.workspacePath(workspaceId, `/runs/${pathId(runId)}/items?${query}`), signal,
    );
  }

  pauseRun(runId: string, idempotencyKey: string, workspaceId = "ws_default",
    signal?: AbortSignal): Promise<RunCommandResult> {
    return this.runCommand(runId, "pause", {}, idempotencyKey, workspaceId, signal);
  }

  resumeRun(runId: string, idempotencyKey: string, workspaceId = "ws_default",
    signal?: AbortSignal): Promise<RunCommandResult> {
    return this.runCommand(runId, "resume", {}, idempotencyKey, workspaceId, signal);
  }

  cancelRun(runId: string, reason: string, idempotencyKey: string,
    workspaceId = "ws_default", signal?: AbortSignal): Promise<RunCommandResult> {
    return this.runCommand(runId, "cancel", { reason }, idempotencyKey, workspaceId, signal);
  }

  changeRunConcurrency(runId: string, target: number, idempotencyKey: string,
    workspaceId = "ws_default", signal?: AbortSignal): Promise<RunCommandResult> {
    return this.runCommand(runId, "concurrency", { target }, idempotencyKey, workspaceId, signal);
  }

  replaceSecret(namespace: string, name: string, version: string, value: string,
    workspaceId = "ws_default", signal?: AbortSignal): Promise<SecretMetadata> {
    return this.putJson<SecretMetadata>(this.secretPath(workspaceId, namespace, name, version),
      { value }, signal);
  }

  secretMetadata(namespace: string, name: string, version: string,
    workspaceId = "ws_default", signal?: AbortSignal): Promise<SecretMetadata> {
    return this.getJson<SecretMetadata>(this.secretPath(workspaceId, namespace, name, version), signal);
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

  private async postJson<T>(path: string, value: unknown, idempotencyKey?: string,
    signal?: AbortSignal): Promise<T> {
    const headers: Record<string, string> = {
      Accept: "application/json",
      "Content-Type": "application/json",
    };
    if (idempotencyKey) headers["Idempotency-Key"] = idempotencyKey;
    const response = await fetch(this.resolve(path), {
      method: "POST",
      headers,
      body: JSON.stringify(value),
      signal,
    });
    const body = await response.text();
    if (!response.ok) throw new ApiError(response.status, body);
    return JSON.parse(body) as T;
  }

  private async putJson<T>(path: string, value: unknown, signal?: AbortSignal): Promise<T> {
    const response = await fetch(this.resolve(path), {
      method: "PUT",
      headers: { Accept: "application/json", "Content-Type": "application/json" },
      body: JSON.stringify(value),
      signal,
    });
    const body = await response.text();
    if (!response.ok) throw new ApiError(response.status, body);
    return JSON.parse(body) as T;
  }

  private runCommand(runId: string, command: string, value: unknown, idempotencyKey: string,
    workspaceId: string, signal?: AbortSignal): Promise<RunCommandResult> {
    return this.postJson<RunCommandResult>(
      this.workspacePath(workspaceId, `/runs/${pathId(runId)}/commands/${command}`),
      value, idempotencyKey, signal,
    );
  }

  private workspacePath(workspaceId: string, suffix: string): string {
    return `/api/v1/workspaces/${pathId(workspaceId)}${suffix}`;
  }

  private secretPath(workspaceId: string, namespace: string, name: string, version: string): string {
    return this.workspacePath(workspaceId,
      `/secrets/${pathId(namespace)}/${pathId(name)}/versions/${pathId(version)}`);
  }

  private resolve(path: string): string {
    if (/^https?:\/\//.test(path)) {
      return path;
    }
    return `${this.baseUrl}${path.startsWith("/") ? path : `/${path}`}`;
  }
}

function pathId(value: string): string {
  if (!/^[A-Za-z0-9._-]+$/.test(value)) throw new Error("Resource ID contains unsupported path characters");
  return value;
}

function defaultBaseUrl(): string {
  if (typeof window !== "undefined" && window.location.protocol === "file:") {
    return "http://127.0.0.1:18990";
  }
  return "";
}
