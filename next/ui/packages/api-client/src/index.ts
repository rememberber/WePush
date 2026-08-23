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

export interface AgentProvider {
  providerId: string;
  implementationVersion: string;
  spiMajor: number;
  maximumConcurrency: number;
}

export interface Agent {
  id: string;
  status: "ONLINE" | "DRAINING" | "DEGRADED" | "OFFLINE";
  agentVersion: string;
  protocolVersion: number;
  operatingSystem: string;
  architecture: string;
  javaVersion: string;
  maximumRuns: number;
  activeRuns: number;
  availableRuns: number;
  providers: AgentProvider[];
  sessionId: string;
  lastAgentSequence: number;
  lastServiceSequence: number;
  connectedAt: string;
  lastSeenAt: string;
  disconnectedAt?: string;
  version: number;
  links: Record<string, string>;
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

export interface Artifact {
  id: string;
  workspaceId: string;
  runId: string;
  type: "RUN_RESULTS_CSV" | string;
  backend: string;
  originalName: string;
  contentType: string;
  size: number;
  sha256: string;
  state: "UPLOADING" | "READY" | "DELETING" | "DELETED" | "FAILED";
  expiresAt: string;
  pinned: boolean;
  legalHold: boolean;
  createdAt: string;
  readyAt?: string;
  deletedAt?: string;
  version: number;
  links: Record<string, string>;
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

export interface Schedule {
  id: string;
  workspaceId: string;
  jobId: string;
  name: string;
  cronExpression: string;
  timezone: string;
  misfirePolicy: "FIRE_ONCE" | "SKIP";
  enabled: boolean;
  nextFireAt: string;
  lastFireAt?: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface ApiTokenSummary {
  tokenId: string;
  principalId: string;
  name: string;
  expiresAt: string;
  revokedAt?: string;
  createdAt: string;
  lastUsedAt?: string;
}

export interface IssuedApiToken {
  tokenId: string;
  principalId: string;
  token: string;
  expiresAt: string;
  workspaceId: string;
  role: "VIEWER" | "OPERATOR" | "ADMIN";
}

export interface EnrollmentToken { id: string; token: string; expiresAt: string; }

export interface AuditEvent {
  id: string;
  workspaceId?: string;
  actorType: string;
  actorId: string;
  action: string;
  resourceType: string;
  resourceId: string;
  result: string;
  detailsJson: string;
  occurredAt: string;
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
  private token: string;

  constructor(baseUrl = defaultBaseUrl(), token = defaultToken()) {
    this.baseUrl = baseUrl.replace(/\/$/, "");
    this.token = token;
  }

  setToken(token: string): void {
    this.token = token.trim();
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

  agents(signal?: AbortSignal): Promise<Agent[]> {
    return this.getJson<Agent[]>("/api/v1/agents", signal);
  }

  agent(agentId: string, signal?: AbortSignal): Promise<Agent> {
    return this.getJson<Agent>(`/api/v1/agents/${pathId(agentId)}`, signal);
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

  async streamRunEvents(runId: string, afterSequence: string,
    receive: (event: { id: string; type: string; data: string }) => void,
    workspaceId = "ws_default", signal?: AbortSignal): Promise<string> {
    const headers = this.headers({ Accept: "text/event-stream" });
    if (afterSequence) headers["Last-Event-ID"] = afterSequence;
    const response = await fetch(this.runEventsUrl(runId, workspaceId), { headers, signal });
    if (!response.ok || !response.body) throw new ApiError(response.status, await response.text());
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let lastSequence = afterSequence;
    while (true) {
      const { value, done } = await reader.read();
      buffer += decoder.decode(value, { stream: !done }).replace(/\r\n/g, "\n");
      let boundary = buffer.indexOf("\n\n");
      while (boundary >= 0) {
        const block = buffer.slice(0, boundary); buffer = buffer.slice(boundary + 2);
        let id = ""; let type = "message"; const data: string[] = [];
        for (const line of block.split("\n")) {
          if (line.startsWith("id:")) id = line.slice(3).trimStart();
          else if (line.startsWith("event:")) type = line.slice(6).trimStart();
          else if (line.startsWith("data:")) data.push(line.slice(5).trimStart());
        }
        if (id || data.length) { receive({ id, type, data: data.join("\n") }); if (id) lastSequence = id; }
        boundary = buffer.indexOf("\n\n");
      }
      if (done) return lastSequence;
    }
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

  runArtifacts(runId: string, workspaceId = "ws_default",
    signal?: AbortSignal): Promise<Artifact[]> {
    return this.getJson<Artifact[]>(
      this.workspacePath(workspaceId, `/runs/${pathId(runId)}/artifacts`), signal,
    );
  }

  createResultExport(runId: string, workspaceId = "ws_default",
    signal?: AbortSignal): Promise<Artifact> {
    return this.postJson<Artifact>(
      this.workspacePath(workspaceId, `/runs/${pathId(runId)}/artifacts/result-export`),
      {}, undefined, signal,
    );
  }

  artifact(artifactId: string, workspaceId = "ws_default",
    signal?: AbortSignal): Promise<Artifact> {
    return this.getJson<Artifact>(
      this.workspacePath(workspaceId, `/artifacts/${pathId(artifactId)}`), signal,
    );
  }

  artifactDownloadUrl(artifactId: string, workspaceId = "ws_default"): string {
    return this.resolve(this.workspacePath(workspaceId, `/artifacts/${pathId(artifactId)}/content`));
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

  schedules(workspaceId = "ws_default", signal?: AbortSignal): Promise<Schedule[]> {
    return this.getJson<Schedule[]>(this.workspacePath(workspaceId, "/schedules"), signal);
  }

  createSchedule(request: { name: string; jobId: string; cronExpression: string; timezone: string;
    misfirePolicy: "FIRE_ONCE" | "SKIP"; enabled?: boolean }, workspaceId = "ws_default",
    signal?: AbortSignal): Promise<Schedule> {
    return this.postJson<Schedule>(this.workspacePath(workspaceId, "/schedules"), request, undefined, signal);
  }

  setScheduleEnabled(scheduleId: string, enabled: boolean, workspaceId = "ws_default",
    signal?: AbortSignal): Promise<Schedule> {
    return this.patchJson<Schedule>(this.workspacePath(workspaceId, `/schedules/${pathId(scheduleId)}`),
      { enabled }, signal);
  }

  deleteSchedule(scheduleId: string, workspaceId = "ws_default", signal?: AbortSignal): Promise<void> {
    return this.deleteRequest(this.workspacePath(workspaceId, `/schedules/${pathId(scheduleId)}`), signal);
  }

  apiTokens(workspaceId = "ws_default", signal?: AbortSignal): Promise<ApiTokenSummary[]> {
    return this.getJson<ApiTokenSummary[]>(
      this.workspacePath(workspaceId, "/api-tokens"), signal);
  }

  createApiToken(request: { name: string; workspaceId: string; role: "VIEWER" | "OPERATOR" | "ADMIN";
    ttl: string }, signal?: AbortSignal): Promise<IssuedApiToken> {
    const { workspaceId, ...body } = request;
    return this.postJson<IssuedApiToken>(this.workspacePath(workspaceId, "/api-tokens"),
      body, undefined, signal);
  }

  revokeApiToken(tokenId: string, workspaceId = "ws_default", signal?: AbortSignal): Promise<void> {
    return this.deleteRequest(
      this.workspacePath(workspaceId, `/api-tokens/${pathId(tokenId)}`), signal);
  }

  createAgentEnrollmentToken(name: string, ttl = "PT15M", workspaceId = "ws_default",
    signal?: AbortSignal): Promise<EnrollmentToken> {
    return this.postJson<EnrollmentToken>(
      this.workspacePath(workspaceId, "/agent-enrollment-tokens"),
      { name, ttl }, undefined, signal);
  }

  auditEvents(limit = 100, workspaceId = "ws_default", signal?: AbortSignal): Promise<AuditEvent[]> {
    if (!Number.isInteger(limit) || limit < 1 || limit > 1000) throw new Error("Audit limit must be 1..1000");
    return this.getJson<AuditEvent[]>(this.workspacePath(workspaceId, `/audit-events?limit=${limit}`), signal);
  }

  providerSchema(path: string, signal?: AbortSignal): Promise<Record<string, unknown>> {
    return this.getJson<Record<string, unknown>>(path, signal);
  }

  openApi(signal?: AbortSignal): Promise<string> {
    return this.requestText("/openapi.yaml", signal);
  }

  async debugRequest(path: string, method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE" = "GET",
    requestBody?: string, signal?: AbortSignal): Promise<DebugResponse> {
    const startedAt = performance.now();
    const response = await fetch(this.resolve(path), {
      method,
      headers: this.headers({ Accept: "application/json, text/plain, */*",
        ...(requestBody ? { "Content-Type": "application/json" } : {}) }),
      body: requestBody && method !== "GET" ? requestBody : undefined,
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

  debugGet(path: string, signal?: AbortSignal): Promise<DebugResponse> {
    return this.debugRequest(path, "GET", undefined, signal);
  }

  private async getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
    const response = await fetch(this.resolve(path), {
      headers: this.headers({ Accept: "application/json" }),
      signal,
    });
    const body = await response.text();
    if (!response.ok) {
      throw new ApiError(response.status, body);
    }
    return JSON.parse(body) as T;
  }

  private async requestText(path: string, signal?: AbortSignal): Promise<string> {
    const response = await fetch(this.resolve(path), { headers: this.headers(), signal });
    const body = await response.text();
    if (!response.ok) {
      throw new ApiError(response.status, body);
    }
    return body;
  }

  private async postJson<T>(path: string, value: unknown, idempotencyKey?: string,
    signal?: AbortSignal): Promise<T> {
    const headers: Record<string, string> = this.headers({
      Accept: "application/json",
      "Content-Type": "application/json",
    });
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
      headers: this.headers({ Accept: "application/json", "Content-Type": "application/json" }),
      body: JSON.stringify(value),
      signal,
    });
    const body = await response.text();
    if (!response.ok) throw new ApiError(response.status, body);
    return JSON.parse(body) as T;
  }

  private async patchJson<T>(path: string, value: unknown, signal?: AbortSignal): Promise<T> {
    const response = await fetch(this.resolve(path), {
      method: "PATCH",
      headers: this.headers({ Accept: "application/json", "Content-Type": "application/json" }),
      body: JSON.stringify(value), signal,
    });
    const body = await response.text();
    if (!response.ok) throw new ApiError(response.status, body);
    return JSON.parse(body) as T;
  }

  private async deleteRequest(path: string, signal?: AbortSignal): Promise<void> {
    const response = await fetch(this.resolve(path), { method: "DELETE", headers: this.headers(), signal });
    if (!response.ok) throw new ApiError(response.status, await response.text());
  }

  private headers(values: Record<string, string> = {}): Record<string, string> {
    return this.token ? { ...values, Authorization: `Bearer ${this.token}` } : values;
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
    if (/^https?:\/\//i.test(path)) {
      if (!this.baseUrl) throw new Error("Absolute API URLs require an explicit trusted Service base URL");
      const target = new URL(path);
      const service = new URL(this.baseUrl);
      if (target.origin !== service.origin) {
        throw new Error("Cross-origin API URLs are not allowed");
      }
      return target.toString();
    }
    if (path.startsWith("//") || /^[A-Za-z][A-Za-z0-9+.-]*:/.test(path)) {
      throw new Error("API path must be relative to the configured Service");
    }
    return `${this.baseUrl}${path.startsWith("/") ? path : `/${path}`}`;
  }
}

function pathId(value: string): string {
  if (!/^[A-Za-z0-9._-]+$/.test(value)) throw new Error("Resource ID contains unsupported path characters");
  return value;
}

function defaultBaseUrl(): string {
  if (typeof window !== "undefined"
    && (window.location.protocol === "file:" || window.location.protocol === "wepush:")) {
    return "http://127.0.0.1:18990";
  }
  return "";
}

function defaultToken(): string {
  try { return typeof localStorage === "undefined" ? "" : localStorage.getItem("wepush.apiToken") ?? ""; }
  catch { return ""; }
}
