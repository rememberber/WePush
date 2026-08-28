import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";

import {
  type Account,
  type Agent,
  type Artifact,
  type Audience,
  type DebugResponse,
  type Job,
  type Message,
  type ProviderSummary,
  type Run,
  type RunItemResult,
  type Schedule,
  type ApiTokenSummary,
  type AuditEvent,
  type SystemInfo,
  type Workspace,
  type RunOverview,
  type AudienceImport,
  type LiveConfirmation,
  WePushClient,
} from "@wepush-next/api-client";
import { defaultsForSchema, SchemaForm, type JsonSchema } from "@wepush-next/schema-renderer";
import { Badge, Button, EmptyState, Spinner } from "@wepush-next/ui";

interface DesktopCommandResult { ok: boolean; message: string; output: string; name?: string }
interface DesktopServiceStatus { installed: boolean; running: boolean; platform: string; detail: string }
interface WePushDesktopBridge {
  platform: string;
  versions: { chrome: string; electron: string };
  token: { load(): Promise<string>; save(token: string): Promise<void>; clear(): Promise<void> };
  service: {
    status(): Promise<DesktopServiceStatus>;
    start(): Promise<DesktopCommandResult>;
    stop(): Promise<DesktopCommandResult>;
    logs(): Promise<DesktopCommandResult>;
    diagnose(): Promise<DesktopCommandResult>;
  };
  plugins: {
    selectAndStage(): Promise<DesktopCommandResult>;
    activate(name: string): Promise<DesktopCommandResult>;
    rollback(name: string): Promise<DesktopCommandResult>;
  };
}

declare global {
  interface Window { wepushDesktop?: WePushDesktopBridge }
}

type PageId =
  | "overview"
  | "providers"
  | "accounts"
  | "messages"
  | "audiences"
  | "jobs"
  | "runs"
  | "agents"
  | "docs"
  | "settings";

interface NavigationItem {
  id: PageId;
  label: string;
  icon: IconName;
}

type IconName = "home" | "plug" | "key" | "message" | "people" | "task" | "pulse" | "agent" | "code" | "settings";

const navigation: { label: string; items: NavigationItem[] }[] = [
  { label: "工作台", items: [{ id: "overview", label: "总览", icon: "home" }, { id: "runs", label: "运行中心", icon: "pulse" }] },
  {
    label: "配置",
    items: [
      { id: "providers", label: "Providers", icon: "plug" },
      { id: "accounts", label: "账号", icon: "key" },
      { id: "messages", label: "消息", icon: "message" },
      { id: "audiences", label: "受众", icon: "people" },
      { id: "jobs", label: "任务与调度", icon: "task" },
    ],
  },
  { label: "系统", items: [{ id: "agents", label: "Agents", icon: "agent" }, { id: "docs", label: "API 文档", icon: "code" }, { id: "settings", label: "设置", icon: "settings" }] },
];

const pageTitles: Record<PageId, string> = Object.fromEntries(
  navigation.flatMap((group) => group.items.map((item) => [item.id, item.label])),
) as Record<PageId, string>;
const implementedPages: readonly PageId[] = [
  "overview", "providers", "accounts", "messages", "audiences", "jobs", "runs", "agents", "docs", "settings",
];

export function WePushApp({ apiBaseUrl }: { apiBaseUrl?: string }) {
  const client = useMemo(() => new WePushClient(apiBaseUrl), [apiBaseUrl]);
  const [credentialReady, setCredentialReady] = useState(() => !window.wepushDesktop);
  const [activePage, setActivePage] = useState<PageId>("overview");
  const [system, setSystem] = useState<SystemInfo>();
  const [providers, setProviders] = useState<ProviderSummary[]>([]);
  const [agents, setAgents] = useState<Agent[]>([]);
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [workspaceId, setWorkspaceId] = useState("ws_default");
  const [overview, setOverview] = useState<RunOverview>();
  const [loading, setLoading] = useState(true);
  const [connectionError, setConnectionError] = useState<string>();

  const refresh = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    try {
      const nextSystem = await client.systemInfo(signal);
      const [nextProviders, nextAgents, nextWorkspaces] = await Promise.all([
        client.providers(signal),
        client.agents(signal),
        client.workspaces(signal),
      ]);
      const nextWorkspaceId = nextSystem.mode.toLowerCase() === "standalone" ? "ws_default"
        : nextWorkspaces.some((item) => item.id === workspaceId) ? workspaceId : nextWorkspaces[0]?.id ?? "ws_default";
      setSystem(nextSystem);
      setProviders(nextProviders);
      setAgents(nextAgents);
      setWorkspaces(nextWorkspaces);
      setWorkspaceId(nextWorkspaceId);
      setOverview(await client.overview(nextWorkspaceId, signal));
      setConnectionError(undefined);
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") return;
      setConnectionError(error instanceof Error ? error.message : "无法连接 Service");
    } finally {
      setLoading(false);
    }
  }, [client, workspaceId]);

  useEffect(() => {
    try { localStorage.removeItem("wepush.apiToken"); } catch { /* remove legacy persistent browser credential when possible */ }
    const bridge = window.wepushDesktop;
    if (!bridge) { setCredentialReady(true); return; }
    let active = true;
    void bridge.token.load().then((token) => {
      if (active) client.setToken(token);
    }).catch(() => {
      if (active) client.setToken("");
    }).finally(() => {
      if (active) setCredentialReady(true);
    });
    return () => { active = false; };
  }, [client]);

  useEffect(() => {
    if (!credentialReady) return;
    const controller = new AbortController();
    void refresh(controller.signal);
    return () => controller.abort();
  }, [credentialReady, refresh]);

  return (
    <div className="app-shell">
      <Sidebar activePage={activePage} onNavigate={setActivePage} connected={!connectionError && Boolean(system)}
        system={system} workspaces={workspaces} workspaceId={workspaceId} onWorkspaceChange={setWorkspaceId}
        activeRuns={overview?.activeRuns ?? 0} />
      <div className="app-workspace">
        <Topbar title={pageTitles[activePage]} connected={!connectionError && Boolean(system)} />
        <main className="app-content">
          {activePage === "overview" ? (
            <Overview
              system={system}
              providers={providers}
              agents={agents}
              overview={overview}
              workspaceId={workspaceId}
              loading={loading}
              error={connectionError}
              onRefresh={() => void refresh()}
              onNavigate={setActivePage}
            />
          ) : null}
          {activePage === "providers" ? <ProvidersPage key={workspaceId} client={client} workspaceId={workspaceId} providers={providers} loading={loading} /> : null}
          {activePage === "accounts" ? <AccountsPage key={workspaceId} client={client} workspaceId={workspaceId} onNavigate={setActivePage} /> : null}
          {activePage === "messages" ? <MessagesPage key={workspaceId} client={client} workspaceId={workspaceId} providers={providers} /> : null}
          {activePage === "audiences" ? <AudiencesPage key={workspaceId} client={client} workspaceId={workspaceId} /> : null}
          {activePage === "jobs" ? <JobsPage key={workspaceId} client={client} workspaceId={workspaceId} onNavigate={setActivePage} /> : null}
          {activePage === "runs" ? <RunsPage key={workspaceId} client={client} workspaceId={workspaceId} onNavigate={setActivePage} /> : null}
          {activePage === "agents" ? <AgentsPage client={client} /> : null}
          {activePage === "docs" ? <ApiDocsPage client={client} /> : null}
          {activePage === "settings" ? <SettingsPage key={workspaceId} client={client} workspaceId={workspaceId} /> : null}
          {!implementedPages.includes(activePage) ? (
            <ComingSoon page={activePage} onNavigate={setActivePage} />
          ) : null}
        </main>
      </div>
    </div>
  );
}

function Sidebar({ activePage, onNavigate, connected, system, workspaces, workspaceId, onWorkspaceChange,
  activeRuns }: { activePage: PageId; onNavigate: (page: PageId) => void; connected: boolean;
    system?: SystemInfo; workspaces: Workspace[]; workspaceId: string; onWorkspaceChange: (id: string) => void;
    activeRuns: number }) {
  const standalone = system?.mode.toLowerCase() === "standalone";
  const selected = workspaces.find((workspace) => workspace.id === workspaceId);
  return (
    <aside className="sidebar">
      <div className="brand-row">
        <div className="brand-mark" aria-hidden="true"><span /><span /><span /></div>
        <span>WePush</span>
        <Badge tone="neutral">Next</Badge>
      </div>
      <label className="workspace-switcher">
        <span className="workspace-avatar">L</span>
        <span><strong>{standalone ? "Local workspace" : selected?.name ?? workspaceId}</strong><small>{standalone ? "Standalone" : "Self-hosted Server"}</small></span>
        {!standalone ? <select aria-label="选择 Workspace" value={workspaceId}
          onChange={(event) => onWorkspaceChange(event.target.value)}>{workspaces.map((workspace) =>
            <option key={workspace.id} value={workspace.id}>{workspace.name}</option>)}</select> : null}
      </label>
      <nav className="sidebar-nav" aria-label="主导航">
        {navigation.map((group) => (
          <section key={group.label}>
            <h2>{group.label}</h2>
            {group.items.map((item) => (
              <button
                type="button"
                key={item.id}
                className={item.id === activePage ? "nav-item nav-item--active" : "nav-item"}
                onClick={() => onNavigate(item.id)}
              >
                <Icon name={item.icon} />
                <span>{item.label}</span>
                {item.id === "runs" ? <span className="nav-count">{activeRuns}</span> : null}
              </button>
            ))}
          </section>
        ))}
      </nav>
      <div className="sidebar-status">
        <span className={connected ? "status-dot status-dot--online" : "status-dot"} />
        <span><strong>{connected ? "Service 已连接" : "Service 未连接"}</strong><small>127.0.0.1:18990</small></span>
      </div>
      <button className="profile-row" type="button">
        <span className="profile-avatar">Z</span>
        <span><strong>本地管理员</strong><small>Owner</small></span>
        <span>•••</span>
      </button>
    </aside>
  );
}

function Topbar({ title, connected }: { title: string; connected: boolean }) {
  return (
    <header className="topbar">
      <div className="topbar-title">
        <div className="history-controls" aria-hidden="true"><span>‹</span><span>›</span></div>
        <h1>{title}</h1>
      </div>
      <div className="topbar-actions">
        <button className="command-button" type="button"><Icon name="search" /><span>搜索或跳转</span><kbd>⌘ K</kbd></button>
        <span className={connected ? "connection-pill connection-pill--online" : "connection-pill"}>
          <span className="status-dot status-dot--online" />{connected ? "Connected" : "Offline"}
        </span>
      </div>
    </header>
  );
}

interface OverviewProps {
  system?: SystemInfo;
  providers: ProviderSummary[];
  agents: Agent[];
  overview?: RunOverview;
  workspaceId: string;
  loading: boolean;
  error?: string;
  onRefresh: () => void;
  onNavigate: (page: PageId) => void;
}

function Overview({ system, providers, agents, overview, workspaceId, loading, error, onRefresh, onNavigate }: OverviewProps) {
  return (
    <div className="page page--overview">
      <section className="page-heading">
        <div><p className="eyebrow">{workspaceId}</p><h2>开始使用 WePush Next</h2><p>配置消息通道，创建发送任务，并在一个工作台中观察执行情况。</p></div>
        <Button variant="primary" onClick={() => onNavigate("providers")}><span>＋</span> 配置 Provider</Button>
      </section>

      {error ? (
        <div className="connection-banner" role="alert">
          <div className="banner-icon">!</div>
          <div><strong>无法连接本地 Service</strong><p>{error}。请先运行 <code>java -jar wepush-next-service.jar</code>。</p></div>
          <Button onClick={onRefresh}>重新连接</Button>
        </div>
      ) : null}

      <section className="metric-grid" aria-label="系统概况">
        <MetricCard label="Service" value={loading ? "—" : system ? "运行中" : "离线"} detail={system ? `${system.mode} · ${system.version}` : "等待连接"} tone={system ? "success" : "neutral"} />
        <MetricCard label="Providers" value={loading ? "—" : String(providers.length)} detail={providers.length ? `${providers[0]?.displayName ?? ""} 已就绪` : "尚未发现 Provider"} />
        <MetricCard label="活动 Runs" value={loading ? "—" : String(overview?.activeRuns ?? 0)} detail={`${overview?.totalRuns ?? 0} total runs`} />
        <MetricCard label="Agents" value={loading ? "—" : String(agents.length)} detail={`${agents.filter((agent) => agent.status === "ONLINE").length} online · ${agents.reduce((sum, agent) => sum + agent.activeRuns, 0)} active runs`} tone="info" />
      </section>

      <div className="dashboard-grid">
        <section className="panel activity-panel">
          <PanelHeader title="运行情况" description="最近 14 天" action={<button type="button" className="text-button" onClick={() => onNavigate("runs")}>查看全部 <span>→</span></button>} />
          <div className="activity-summary"><strong>{overview?.totalRuns ?? 0}</strong><span>次运行</span><span className="delta neutral">成功 {overview?.succeededRuns ?? 0} · 问题 {overview?.problemRuns ?? 0}</span></div>
          <div className="chart-empty" aria-label="运行趋势数据">
            <div className="chart-grid-lines"><i /><i /><i /><i /></div>
            <svg viewBox="0 0 640 120" preserveAspectRatio="none" role="img" aria-label="空趋势线">
              <polyline points={trendPoints(overview?.trend ?? [])} fill="none" stroke="currentColor" strokeWidth="2" />
            </svg>
            <div className="chart-labels"><span>00:00</span><span>06:00</span><span>12:00</span><span>18:00</span><span>现在</span></div>
          </div>
          <div className="chart-legend"><span><i className="legend-dot success" />成功 {overview?.succeededRuns ?? 0}</span><span><i className="legend-dot danger" />问题 {overview?.problemRuns ?? 0}</span></div>
        </section>

        <section className="panel quick-start-panel">
          <PanelHeader title="快速开始" description="完成第一条消息链路" />
          <ol className="step-list">
            <Step done={providers.length > 0} number="01" title="连接 Provider" detail={providers.length ? `${providers[0]?.displayName ?? "Provider"} 已发现` : "配置 API 端点和凭据"} onClick={() => onNavigate("providers")} />
            <Step number="02" title="创建消息模板" detail="定义内容与变量" onClick={() => onNavigate("messages")} />
            <Step number="03" title="导入受众" detail="添加第一批收件人" onClick={() => onNavigate("audiences")} />
            <Step number="04" title="发起运行" detail="Dry Run 后正式发送" onClick={() => onNavigate("jobs")} />
          </ol>
        </section>
      </div>

      <section className="panel recent-panel">
        <PanelHeader title="最近运行" description="任务执行历史与实时状态" action={<Button variant="ghost" onClick={() => onNavigate("runs")}>运行中心</Button>} />
        {overview?.recent.length ? <div className="resource-card-list">{overview.recent.map((run) => <article key={run.id}>
          <span className="resource-card-icon"><Icon name="pulse" /></span><div><strong>{run.jobName}</strong><small>{formatTime(run.createdAt)} · {run.counters.total} items</small><code>{shortId(run.id)}</code></div><Badge tone={runTone(run.state)}>{run.state}</Badge>
        </article>)}</div> : <EmptyState icon={<Icon name="pulse" />} title="还没有运行记录" description="创建 Provider、消息与受众后，即可从任务页启动第一次 Dry Run。" action={<Button onClick={() => onNavigate("providers")}>从 Provider 开始</Button>} />}
      </section>
    </div>
  );
}

function ProvidersPage({ client, workspaceId, providers, loading }: { client: WePushClient; workspaceId: string; providers: ProviderSummary[]; loading: boolean }) {
  const [selectedId, setSelectedId] = useState<string>();
  const selected = providers.find((provider) => provider.providerId === selectedId) ?? providers[0];
  const [schema, setSchema] = useState<JsonSchema>();
  const [formValue, setFormValue] = useState<Record<string, unknown>>({});
  const [schemaError, setSchemaError] = useState<string>();
  const [accountName, setAccountName] = useState("Local HTTP");
  const [saving, setSaving] = useState(false);
  const [savedAccount, setSavedAccount] = useState<Account>();
  const [pluginName, setPluginName] = useState<string>();
  const [pluginResult, setPluginResult] = useState<DesktopCommandResult>();
  const [pluginBusy, setPluginBusy] = useState(false);
  const desktopPlugins = window.wepushDesktop?.plugins;

  useEffect(() => {
    if (!selected) return;
    setSelectedId(selected.providerId);
    const controller = new AbortController();
    void client.providerSchema(selected.links.accountSchema, controller.signal)
      .then((document) => {
        const nextSchema = document as JsonSchema;
        setSchema(nextSchema);
        setFormValue(defaultsForSchema(nextSchema));
        setAccountName(selected.displayName);
        setSavedAccount(undefined);
        setSchemaError(undefined);
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") return;
        setSchemaError(error instanceof Error ? error.message : "Schema 加载失败");
      });
    return () => controller.abort();
  }, [client, selected]);

  async function saveAccount() {
    if (!selected) return;
    setSaving(true);
    setSchemaError(undefined);
    try {
      const account = await client.createAccount({
        name: accountName,
        providerId: selected.providerId,
        providerVersion: selected.implementationVersion,
        configuration: formValue,
      }, workspaceId);
      setSavedAccount(account);
    } catch (error) {
      setSchemaError(error instanceof Error ? error.message : "账号保存失败");
    } finally {
      setSaving(false);
    }
  }

  async function stagePlugin() {
    if (!desktopPlugins) return;
    setPluginBusy(true);
    try {
      const result = await desktopPlugins.selectAndStage();
      setPluginResult(result);
      if (result.ok && result.name) setPluginName(result.name);
    } catch (error) {
      setPluginResult({ ok: false, message: error instanceof Error ? error.message : "插件校验失败", output: "" });
    } finally { setPluginBusy(false); }
  }

  async function changePlugin(action: "activate" | "rollback") {
    if (!desktopPlugins || !pluginName) return;
    setPluginBusy(true);
    try {
      setPluginResult(await desktopPlugins[action](pluginName));
    } catch (error) {
      setPluginResult({ ok: false, message: error instanceof Error ? error.message : "插件操作失败", output: "" });
    } finally { setPluginBusy(false); }
  }

  return (
    <div className="page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">EXTENSIBILITY</p><h2>Providers</h2><p>发现消息能力，并通过 Provider Schema 可视化配置连接。</p></div>
        <Button variant="primary" disabled={!desktopPlugins || pluginBusy} onClick={() => void stagePlugin()}>
          {pluginBusy ? "处理中…" : "＋ 本地安装 Provider"}
        </Button>
      </section>
      <div className="split-layout">
        <section className="panel provider-list-panel">
          <div className="list-toolbar"><strong>已发现</strong><Badge>{providers.length}</Badge></div>
          {loading ? <div className="loading-row"><Spinner />正在发现 Provider…</div> : null}
          {!loading && providers.length === 0 ? <EmptyState icon={<Icon name="plug" />} title="没有 Provider" description="把签名后的 Provider 插件放入版本目录后重启 Service。" /> : null}
          {providers.map((provider) => (
            <button
              type="button"
              key={`${provider.providerId}:${provider.implementationVersion}`}
              className={selected?.providerId === provider.providerId ? "provider-list-item provider-list-item--active" : "provider-list-item"}
              onClick={() => setSelectedId(provider.providerId)}
            >
              <span className="provider-logo">{provider.displayName.slice(0, 1)}</span>
              <span><strong>{provider.displayName}</strong><small>{provider.providerId} · v{provider.implementationVersion}</small></span>
              <span className="provider-check">✓</span>
            </button>
          ))}
        </section>
        <section className="panel provider-config-panel">
          {selected ? (
            <>
              <div className="provider-title-row">
                <div className="provider-logo provider-logo--large">{selected.displayName.slice(0, 1)}</div>
                <div><h3>{selected.displayName}</h3><p>{selected.providerId} · v{selected.implementationVersion}</p></div>
                <Badge tone="success">Ready</Badge>
              </div>
              <div className="capability-row">
                {selected.capabilities.map((capability) => <Badge key={capability}>{capability}</Badge>)}
                <span>最大并发 {selected.maximumConcurrency}</span>
              </div>
              <div className="section-divider" />
              <div className="form-heading"><div><h3>连接配置</h3><p>字段来自当前 Provider 的 Account Schema。</p></div><Badge tone="info">Dynamic Schema</Badge></div>
              <label className="simple-field"><span>账号名称</span><input value={accountName} onChange={(event) => setAccountName(event.target.value)} /></label>
              {schemaError ? <div className="inline-error">{schemaError}</div> : null}
              {savedAccount ? <div className="inline-success">账号 {savedAccount.name} 已保存，可以继续创建消息和受众。</div> : null}
              {!schema && !schemaError ? <div className="loading-row"><Spinner />正在加载配置 Schema…</div> : null}
              {schema ? <SchemaForm schema={schema} value={formValue} onChange={setFormValue} /> : null}
              <div className="form-actions"><Button disabled={!savedAccount} onClick={() => savedAccount && void client.testAccount(savedAccount.id, "PT10S", workspaceId).then((result) => window.alert(result.successful ? "连接成功" : result.diagnostic))}>测试已保存连接</Button><Button variant="primary" onClick={() => void saveAccount()} disabled={saving || !schema}>{saving ? "保存中…" : "保存账号"}</Button></div>
            </>
          ) : <EmptyState icon={<Icon name="plug" />} title="选择一个 Provider" description="发现 Provider 后即可查看并渲染其实时配置 Schema。" />}
        </section>
      </div>
      <section className="panel recent-panel">
        <PanelHeader title="签名 Provider 插件" description={desktopPlugins
          ? "选择本机 ZIP 后，Agent 会先校验清单、内容摘要和受信签名，再进入 Stage。"
          : "插件安装只在 Desktop 提供；浏览器不会获得本机文件或服务管理权限。"} />
        {pluginResult ? <div className={pluginResult.ok ? "inline-success" : "inline-error"}>{pluginResult.message}</div> : null}
        {pluginResult?.output ? <pre className="response-body"><code>{pluginResult.output}</code></pre> : null}
        <div className="form-actions">
          <Button disabled={!desktopPlugins || pluginBusy} onClick={() => void stagePlugin()}>校验并 Stage</Button>
          <Button variant="primary" disabled={!desktopPlugins || !pluginName || pluginBusy} onClick={() => void changePlugin("activate")}>Activate</Button>
          <Button disabled={!desktopPlugins || !pluginName || pluginBusy} onClick={() => void changePlugin("rollback")}>Rollback</Button>
        </div>
        {pluginName ? <p className="api-description">当前操作目标：<code>{pluginName}</code></p> : null}
      </section>
    </div>
  );
}

function AccountsPage({ client, workspaceId, onNavigate }: { client: WePushClient; workspaceId: string; onNavigate: (page: PageId) => void }) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [search, setSearch] = useState("");
  const [nextCursor, setNextCursor] = useState<string>();
  const [action, setAction] = useState<string>();

  const load = useCallback(async (append = false) => {
    setLoading(true);
    try {
      const page = await client.accountPage({ name: search || undefined, cursor: append ? nextCursor : undefined }, workspaceId);
      setAccounts((current) => append ? [...current, ...page.items] : page.items);
      setNextCursor(page.page.nextCursor);
      setError(undefined);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "账号加载失败");
    } finally {
      setLoading(false);
    }
  }, [client, nextCursor, search, workspaceId]);

  useEffect(() => { void load(); }, [client, workspaceId]);

  async function mutate(account: Account, kind: "test" | "edit" | "enable" | "disable" | "archive") {
    setAction(account.id); setError(undefined);
    try {
      if (kind === "test") {
        const result = await client.testAccount(account.id, "PT10S", workspaceId);
        window.alert(result.successful ? `连接成功（${result.latencyMillis} ms）` : `${result.code}: ${result.diagnostic}`);
      } else {
        const name = kind === "edit" ? window.prompt("账号名称", account.name) : undefined;
        if (kind === "edit" && !name) return;
        await client.updateAccount(account.id, { name: name ?? undefined,
          status: kind === "enable" ? "ACTIVE" : kind === "disable" ? "DISABLED"
            : kind === "archive" ? "ARCHIVED" : undefined }, workspaceId);
        await load();
      }
    } catch (nextError) { setError(nextError instanceof Error ? nextError.message : "账号操作失败"); }
    finally { setAction(undefined); }
  }

  return (
    <div className="page resource-page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">CHANNEL CONFIGURATION</p><h2>账号</h2><p>Provider 连接配置归属当前工作区，敏感值仅保存 Secret 引用。</p></div>
        <Button variant="primary" onClick={() => onNavigate("providers")}>＋ 新建账号</Button>
      </section>
      <section className="panel resource-table-panel">
        <div className="list-toolbar"><strong>全部账号</strong><Badge>{accounts.length}</Badge><input placeholder="按名称筛选" value={search} onChange={(event) => setSearch(event.target.value)} /><Button variant="ghost" onClick={() => void load()}>筛选</Button></div>
        {loading ? <div className="loading-row"><Spinner />正在读取账号…</div> : null}
        {error ? <div className="inline-error">{error}</div> : null}
        {!loading && !error && accounts.length === 0 ? <EmptyState icon={<Icon name="key" />} title="还没有账号" description="从 Provider 页面打开动态 Schema 表单，保存第一个渠道账号。" action={<Button onClick={() => onNavigate("providers")}>配置 Provider</Button>} /> : null}
        {accounts.length ? <div className="resource-table">
          <div className="resource-row resource-row--header"><span>名称</span><span>Provider</span><span>状态</span><span>操作</span></div>
          {accounts.map((account) => <div className="resource-row" key={account.id}>
            <span><strong>{account.name}</strong><small>{account.id}</small></span>
            <span>{account.providerId}<small>v{account.providerVersion}</small></span>
            <span><Badge tone="success">{account.status}</Badge></span>
            <span className="heading-actions"><Button variant="ghost" disabled={action === account.id} onClick={() => void mutate(account, "test")}>测试</Button><Button variant="ghost" onClick={() => void mutate(account, "edit")}>编辑</Button>{account.status === "ACTIVE" ? <Button variant="ghost" onClick={() => void mutate(account, "disable")}>停用</Button> : account.status === "DISABLED" ? <Button variant="ghost" onClick={() => void mutate(account, "enable")}>启用</Button> : null}{account.status !== "ARCHIVED" ? <Button variant="ghost" onClick={() => void mutate(account, "archive")}>归档</Button> : null}</span>
          </div>)}
        </div> : null}
        {nextCursor ? <div className="load-more-row"><Button variant="ghost" onClick={() => void load(true)}>加载更多</Button></div> : null}
      </section>
    </div>
  );
}

function MessagesPage({ client, workspaceId, providers }: { client: WePushClient; workspaceId: string; providers: ProviderSummary[] }) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [providerId, setProviderId] = useState<string>();
  const [schema, setSchema] = useState<JsonSchema>();
  const [content, setContent] = useState<Record<string, unknown>>({});
  const [name, setName] = useState("Welcome message");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string>();
  const [editing, setEditing] = useState<Message>();
  const [revisions, setRevisions] = useState<import("@wepush-next/api-client").MessageRevision[]>([]);
  const [diff, setDiff] = useState<string[]>();
  const [nextCursor, setNextCursor] = useState<string>();
  const selected = providers.find((provider) => provider.providerId === providerId) ?? providers[0];

  const load = useCallback(async (append = false) => {
    try {
      const page = await client.messagePage({ cursor: append ? nextCursor : undefined }, workspaceId);
      setMessages((current) => append ? [...current, ...page.items] : page.items);
      setNextCursor(page.page.nextCursor);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "消息加载失败");
    }
  }, [client, nextCursor, workspaceId]);

  useEffect(() => { void load(); }, [client, workspaceId]);
  useEffect(() => {
    if (!selected) return;
    setProviderId(selected.providerId);
    const controller = new AbortController();
    void client.providerSchema(selected.links.messageSchema, controller.signal)
      .then((document) => {
        const nextSchema = document as JsonSchema;
        setSchema(nextSchema);
        setContent(defaultsForSchema(nextSchema));
        setError(undefined);
      })
      .catch((nextError: unknown) => {
        if (nextError instanceof DOMException && nextError.name === "AbortError") return;
        setError(nextError instanceof Error ? nextError.message : "消息 Schema 加载失败");
      });
    return () => controller.abort();
  }, [client, selected]);

  async function createMessage() {
    if (!selected) return;
    setSaving(true);
    setError(undefined);
    try {
      if (editing) await client.updateMessage(editing.id, { name, content }, workspaceId);
      else await client.createMessage({ name, providerId: selected.providerId,
        providerVersion: selected.implementationVersion, content }, workspaceId);
      await load();
      setEditing(undefined);
      setName(`Message ${messages.length + 2}`);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "消息保存失败");
    } finally {
      setSaving(false);
    }
  }

  async function inspect(message: Message) {
    setEditing(message); setName(message.name); setProviderId(message.providerId); setContent(message.content);
    try {
      const history = await client.messageRevisions(message.id, 0, 100, workspaceId);
      setRevisions(history.items);
      if (history.items.length > 1) setDiff((await client.messageDiff(message.id,
        history.items[1]!.revision, history.items[0]!.revision, workspaceId)).changedPaths);
      else setDiff([]);
    } catch (nextError) { setError(nextError instanceof Error ? nextError.message : "修订历史加载失败"); }
  }

  async function messageAction(message: Message, kind: "copy" | "status") {
    try {
      if (kind === "copy") {
        const copyName = window.prompt("副本名称", `${message.name} copy`); if (!copyName) return;
        await client.copyMessage(message.id, copyName, workspaceId);
      } else await client.updateMessage(message.id,
        { status: message.status === "ACTIVE" ? "DISABLED" : "ACTIVE" }, workspaceId);
      await load();
    } catch (nextError) { setError(nextError instanceof Error ? nextError.message : "消息操作失败"); }
  }

  return (
    <div className="page resource-page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">IMMUTABLE REVISION</p><h2>消息</h2><p>根据 Provider Message Schema 可视化编排内容，每次保存形成可追溯修订。</p></div>
        <Badge tone="info">Schema driven</Badge>
      </section>
      <div className="composer-layout">
        <section className="panel composer-panel">
          <PanelHeader title={editing ? `编辑 revision ${editing.revision}` : "新建消息"} description="保存内容变更会创建新的不可变 Revision" />
          <label className="simple-field"><span>名称</span><input value={name} onChange={(event) => setName(event.target.value)} /></label>
          <label className="simple-field"><span>Provider</span><select value={selected?.providerId ?? ""} onChange={(event) => setProviderId(event.target.value)}>
            {providers.map((provider) => <option value={provider.providerId} key={provider.providerId}>{provider.displayName} · {provider.implementationVersion}</option>)}
          </select></label>
          {error ? <div className="inline-error">{error}</div> : null}
          {!schema && selected ? <div className="loading-row"><Spinner />正在读取 Message Schema…</div> : null}
          {schema ? <SchemaForm schema={schema} value={content} onChange={setContent} /> : null}
          <div className="form-actions"><Button variant="primary" disabled={!schema || saving || !name.trim()} onClick={() => void createMessage()}>{saving ? "保存中…" : editing ? "保存新 Revision" : "保存消息"}</Button>{editing ? <Button onClick={() => setEditing(undefined)}>取消编辑</Button> : null}</div>
          {editing ? <div className="api-safety-note"><span>Δ</span><p><strong>修订历史</strong><br />{revisions.map((item) => `r${item.revision}`).join(" → ") || "加载中"}<br />最近 Diff: {diff?.join(", ") || "无顶层字段变化"}</p></div> : null}
        </section>
        <section className="panel resource-list-panel">
          <div className="list-toolbar"><strong>消息修订</strong><Badge>{messages.length}</Badge><Button variant="ghost" onClick={() => void load()}>刷新</Button></div>
          {messages.length === 0 ? <EmptyState icon={<Icon name="message" />} title="还没有消息" description="左侧表单来自 Provider Schema，保存后可直接组合到任务。" /> : null}
          <div className="resource-card-list">{messages.map((message) => <article key={message.id}>
            <span className="resource-card-icon"><Icon name="message" /></span>
            <div><strong>{message.name}</strong><small>{message.providerId} · revision {message.revision}</small><code>{shortId(message.contentHash)}</code></div>
            <div className="heading-actions"><Badge tone="success">{message.status}</Badge><Button variant="ghost" onClick={() => void inspect(message)}>编辑/历史</Button><Button variant="ghost" onClick={() => void messageAction(message, "copy")}>复制</Button><Button variant="ghost" onClick={() => void messageAction(message, "status")}>{message.status === "ACTIVE" ? "停用" : "启用"}</Button></div>
          </article>)}</div>
          {nextCursor ? <div className="load-more-row"><Button variant="ghost" onClick={() => void load(true)}>加载更多</Button></div> : null}
        </section>
      </div>
    </div>
  );
}

function AudiencesPage({ client, workspaceId }: { client: WePushClient; workspaceId: string }) {
  const [audiences, setAudiences] = useState<Audience[]>([]);
  const [name, setName] = useState("Sample audience");
  const [file, setFile] = useState<File>();
  const [format, setFormat] = useState<"CSV" | "TXT">("CSV");
  const [headers, setHeaders] = useState<string[]>([]);
  const [itemIdColumn, setItemIdColumn] = useState("itemId");
  const [mapping, setMapping] = useState<Record<string, string>>({});
  const [targetAudienceId, setTargetAudienceId] = useState("");
  const [preview, setPreview] = useState<AudienceImport>();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string>();
  const [nextCursor, setNextCursor] = useState<string>();

  const load = useCallback(async () => {
    try {
      const page = await client.audiencePage({}, workspaceId);
      setAudiences(page.items); setNextCursor(page.page.nextCursor);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "受众加载失败");
    }
  }, [client, workspaceId]);
  useEffect(() => { void load(); }, [load]);

  async function loadMore() {
    if (!nextCursor) return;
    try {
      const page = await client.audiencePage({ cursor: nextCursor }, workspaceId);
      setAudiences((current) => [...current, ...page.items]);
      setNextCursor(page.page.nextCursor);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "受众加载失败");
    }
  }

  async function chooseFile(nextFile?: File) {
    setFile(nextFile); setPreview(undefined);
    if (!nextFile) return;
    const nextFormat = nextFile.name.toLowerCase().endsWith(".txt") ? "TXT" : "CSV";
    setFormat(nextFormat);
    const nextHeaders = nextFormat === "TXT" ? ["value"]
      : (await nextFile.slice(0, 65_536).text()).split(/\r?\n/, 1)[0]!.split(",").map((item) => item.trim().replace(/^"|"$/g, ""));
    setHeaders(nextHeaders);
    const guessed = nextHeaders.find((item) => /^(item_?id|id|mobile|email|phone)$/i.test(item)) ?? nextHeaders[0] ?? "itemId";
    setItemIdColumn(nextFormat === "TXT" ? "value" : guessed);
    setMapping(Object.fromEntries(nextHeaders.filter((item) => item !== guessed)
      .map((item) => [item, item])));
  }

  async function uploadAudience() {
    if (!file) return;
    setSaving(true);
    setError(undefined);
    try {
      setPreview(await client.uploadAudience(file, { name, audienceId: targetAudienceId || undefined,
        format, itemIdColumn, fieldMapping: mapping }, workspaceId));
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "受众导入失败");
    } finally {
      setSaving(false);
    }
  }

  async function commitImport() {
    if (!preview) return; setSaving(true);
    try { await client.commitAudienceImport(preview.id, workspaceId); setPreview(undefined); await load(); }
    catch (nextError) { setError(nextError instanceof Error ? nextError.message : "快照提交失败"); }
    finally { setSaving(false); }
  }

  async function downloadErrors() {
    if (!preview) return;
    const blob = await client.downloadAudienceImportErrors(preview.id, workspaceId);
    downloadBlob(blob, `audience-import-${preview.id}-errors.csv`);
  }

  return (
    <div className="page resource-page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">STREAMING SNAPSHOT INPUT</p><h2>受众</h2><p>CSV/TXT 由 Service 流式导入，预览、去重和错误处理后再生成不可变快照。</p></div>
        <Badge tone="success">CSV · TXT</Badge>
      </section>
      <div className="composer-layout">
        <section className="panel composer-panel">
          <PanelHeader title="导入受众" description="选择文件 → 字段映射 → 预览 → 提交快照" />
          <label className="simple-field"><span>名称</span><input value={name} onChange={(event) => setName(event.target.value)} /></label>
          <label className="simple-field"><span>更新已有受众（可选）</span><select value={targetAudienceId} onChange={(event) => setTargetAudienceId(event.target.value)}><option value="">创建新受众</option>{audiences.map((item) => <option key={item.id} value={item.id}>{item.name} · r{item.revision}</option>)}</select></label>
          <label className="simple-field"><span>CSV / TXT 文件</span><input type="file" accept=".csv,.txt,text/csv,text/plain" onChange={(event) => void chooseFile(event.target.files?.[0])} /></label>
          {headers.length ? <><label className="simple-field"><span>itemId 字段</span><select value={itemIdColumn} onChange={(event) => setItemIdColumn(event.target.value)}>{headers.map((header) => <option key={header}>{header}</option>)}</select></label>
            <div className="field-mapping"><strong>字段映射</strong>{headers.filter((header) => header !== itemIdColumn).map((header) => <label className="simple-field" key={header}><span>{header} →</span><input value={mapping[header] ?? ""} onChange={(event) => setMapping((current) => ({ ...current, [header]: event.target.value }))} /></label>)}</div></> : null}
          {error ? <div className="inline-error">{error}</div> : null}
          <div className="form-actions"><Button variant="primary" disabled={saving || !name.trim() || !file} onClick={() => void uploadAudience()}>{saving ? "流式导入中…" : "上传并预览"}</Button></div>
          {preview ? <div className="import-preview"><div className="run-counter-grid"><div><span>总行数</span><strong>{preview.totalRows}</strong></div><div><span>接受</span><strong>{preview.acceptedRows}</strong></div><div><span>错误</span><strong>{preview.rejectedRows}</strong></div><div><span>重复</span><strong>{preview.duplicateRows}</strong></div></div>
            <div className="result-table"><div className="result-row result-row--header"><span>Item</span><span>字段</span><span>状态</span><span>行</span><span>说明</span></div>{[...preview.acceptedPreview, ...preview.errorPreview].slice(0, 20).map((row) => <div className="result-row" key={row.sequence}><span>{row.itemId}</span><span>{Object.keys(row.fields).join(", ")}</span><span><Badge tone={row.accepted ? "success" : "danger"}>{row.accepted ? "ACCEPT" : "ERROR"}</Badge></span><span>{row.sequence}</span><span>{row.errorMessage || "—"}</span></div>)}</div>
            <div className="form-actions"><Button variant="primary" disabled={!preview.acceptedRows || saving} onClick={() => void commitImport()}>确认并生成 Snapshot</Button>{preview.rejectedRows ? <Button onClick={() => void downloadErrors()}>下载错误行 CSV</Button> : null}</div></div> : null}
        </section>
        <section className="panel resource-list-panel">
          <div className="list-toolbar"><strong>受众快照</strong><Badge>{audiences.length}</Badge><Button variant="ghost" onClick={() => void load()}>刷新</Button></div>
          {audiences.length === 0 ? <EmptyState icon={<Icon name="people" />} title="还没有受众" description="粘贴 JSON Recipient 列表即可形成第一份不可变快照。" /> : null}
          <div className="resource-card-list">{audiences.map((audience) => <article key={audience.id}>
            <span className="resource-card-icon"><Icon name="people" /></span>
            <div><strong>{audience.name}</strong><small>{audience.recordCount} recipients · revision {audience.revision}</small><code>{shortId(audience.contentHash)}</code></div>
            <Badge tone="success">{audience.status}</Badge>
          </article>)}</div>
          {nextCursor ? <div className="load-more-row"><Button variant="ghost" onClick={() => void loadMore()}>加载更多</Button></div> : null}
        </section>
      </div>
    </div>
  );
}

function JobsPage({ client, workspaceId, onNavigate }: { client: WePushClient; workspaceId: string; onNavigate: (page: PageId) => void }) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [audiences, setAudiences] = useState<Audience[]>([]);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [name, setName] = useState("Welcome job");
  const [accountId, setAccountId] = useState("");
  const [messageId, setMessageId] = useState("");
  const [audienceId, setAudienceId] = useState("");
  const [concurrency, setConcurrency] = useState(8);
  const [saving, setSaving] = useState(false);
  const [runningJob, setRunningJob] = useState<string>();
  const [error, setError] = useState<string>();
  const [scheduleJobId, setScheduleJobId] = useState("");
  const [cronExpression, setCronExpression] = useState("0 0 9 * * *");
  const [editingJob, setEditingJob] = useState<Job>();
  const [confirmation, setConfirmation] = useState<LiveConfirmation>();

  const load = useCallback(async () => {
    try {
      const [nextAccounts, nextMessages, nextAudiences, nextJobs, nextSchedules] = await Promise.all([
        client.accounts(workspaceId), client.messages(workspaceId), client.audiences(workspaceId),
        client.jobs(workspaceId), client.schedules(workspaceId),
      ]);
      setAccounts(nextAccounts); setMessages(nextMessages); setAudiences(nextAudiences); setJobs(nextJobs);
      setSchedules(nextSchedules);
      setAccountId((current) => current || nextAccounts[0]?.id || "");
      setMessageId((current) => current || nextMessages[0]?.id || "");
      setAudienceId((current) => current || nextAudiences[0]?.id || "");
      setScheduleJobId((current) => current || nextJobs[0]?.id || "");
      setError(undefined);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "任务资源加载失败");
    }
  }, [client, workspaceId]);
  useEffect(() => { void load(); }, [load]);

  async function createJob() {
    setSaving(true); setError(undefined);
    try {
      const request = { name, accountId, messageId, audienceId,
        policies: { concurrency: { minimum: 1, target: concurrency, maximum: Math.max(16, concurrency) } },
        enabled: true };
      if (editingJob) await client.updateJob(editingJob.id, request, workspaceId);
      else await client.createJob(request, workspaceId);
      await load();
      setEditingJob(undefined);
      setName(`Job ${jobs.length + 2}`);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "任务创建失败");
    } finally {
      setSaving(false);
    }
  }

  async function dryRun(job: Job) {
    setRunningJob(job.id); setError(undefined);
    try {
      await client.createRun(job.id, { dryRun: true, reason: "web-ui" }, crypto.randomUUID(), workspaceId);
      onNavigate("runs");
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "Dry Run 创建失败");
    } finally {
      setRunningJob(undefined);
    }
  }

  async function prepareLive(job: Job) {
    setRunningJob(job.id); setError(undefined);
    try { setConfirmation(await client.confirmRun(job.id, workspaceId)); }
    catch (nextError) { setError(nextError instanceof Error ? nextError.message : "发送确认加载失败"); }
    finally { setRunningJob(undefined); }
  }

  async function sendLive() {
    if (!confirmation) return; setRunningJob(confirmation.jobId);
    try {
      await client.createRun(confirmation.jobId, { dryRun: false, reason: "web-ui-live",
        confirmationToken: confirmation.confirmationToken }, crypto.randomUUID(), workspaceId);
      setConfirmation(undefined); onNavigate("runs");
    } catch (nextError) { setError(nextError instanceof Error ? nextError.message : "正式运行创建失败"); }
    finally { setRunningJob(undefined); }
  }

  function editJob(job: Job) {
    setEditingJob(job); setName(job.name); setAccountId(job.accountId); setMessageId(job.messageId);
    setAudienceId(job.audienceId);
    const policy = job.policies.concurrency as { target?: number } | undefined;
    setConcurrency(policy?.target ?? 8);
  }

  async function jobAction(job: Job, kind: "copy" | "toggle" | "archive") {
    try {
      if (kind === "copy") { const copyName = window.prompt("任务副本名称", `${job.name} copy`); if (!copyName) return; await client.copyJob(job.id, copyName, workspaceId); }
      else await client.updateJob(job.id, kind === "archive" ? { archived: true }
        : { enabled: !job.enabled }, workspaceId);
      await load();
    } catch (nextError) { setError(nextError instanceof Error ? nextError.message : "任务操作失败"); }
  }

  async function createSchedule() {
    if (!scheduleJobId) return;
    setError(undefined);
    try {
      await client.createSchedule({
        name: `Schedule ${schedules.length + 1}`, jobId: scheduleJobId,
        cronExpression, timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC",
        misfirePolicy: "FIRE_ONCE", enabled: true,
      }, workspaceId);
      await load();
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "调度创建失败");
    }
  }

  async function toggleSchedule(schedule: Schedule) {
    try { await client.setScheduleEnabled(schedule.id, !schedule.enabled, workspaceId); await load(); }
    catch (nextError) { setError(nextError instanceof Error ? nextError.message : "调度更新失败"); }
  }

  async function editSchedule(schedule: Schedule) {
    const nextName = window.prompt("调度名称", schedule.name); if (!nextName) return;
    const nextCron = window.prompt("六段 Cron", schedule.cronExpression); if (!nextCron) return;
    const timezone = window.prompt("时区", schedule.timezone); if (!timezone) return;
    try { await client.updateSchedule(schedule.id, { name: nextName, cronExpression: nextCron,
      timezone, jobId: schedule.jobId, misfirePolicy: schedule.misfirePolicy,
      enabled: schedule.enabled }, workspaceId); await load(); }
    catch (nextError) { setError(nextError instanceof Error ? nextError.message : "调度编辑失败"); }
  }

  const ready = Boolean(accountId && messageId && audienceId && name.trim());
  return (
    <div className="page resource-page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">EXECUTION DEFINITION</p><h2>任务与调度</h2><p>组合账号、消息、受众与执行策略，先 Dry Run 再正式发送。</p></div>
        <Badge tone={ready ? "success" : "warning"}>{ready ? "Ready" : "需要上游资源"}</Badge>
      </section>
      {error ? <div className="inline-error">{error}</div> : null}
      <div className="composer-layout">
        <section className="panel composer-panel compact-form">
          <PanelHeader title={editingJob ? "编辑任务" : "新建任务"} description="Run 创建时会冻结所有资源修订" />
          <label className="simple-field"><span>名称</span><input value={name} onChange={(event) => setName(event.target.value)} /></label>
          <label className="simple-field"><span>账号</span><select value={accountId} onChange={(event) => setAccountId(event.target.value)}><option value="">请选择</option>{accounts.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
          <label className="simple-field"><span>消息</span><select value={messageId} onChange={(event) => setMessageId(event.target.value)}><option value="">请选择</option>{messages.map((item) => <option key={item.id} value={item.id}>{item.name} · r{item.revision}</option>)}</select></label>
          <label className="simple-field"><span>受众</span><select value={audienceId} onChange={(event) => setAudienceId(event.target.value)}><option value="">请选择</option>{audiences.map((item) => <option key={item.id} value={item.id}>{item.name} · {item.recordCount} items</option>)}</select></label>
          <label className="simple-field"><span>目标并发</span><input type="number" min={1} max={256} value={concurrency} onChange={(event) => setConcurrency(Number(event.target.value))} /></label>
          <div className="form-actions"><Button variant="primary" disabled={!ready || saving} onClick={() => void createJob()}>{saving ? "保存中…" : editingJob ? "保存修改" : "保存任务"}</Button>{editingJob ? <Button onClick={() => setEditingJob(undefined)}>取消</Button> : null}</div>
        </section>
        <section className="panel resource-list-panel">
          <div className="list-toolbar"><strong>任务定义</strong><Badge>{jobs.length}</Badge><Button variant="ghost" onClick={() => void load()}>刷新</Button></div>
          {jobs.length === 0 ? <EmptyState icon={<Icon name="task" />} title="还没有任务" description="先创建账号、消息和受众，再在左侧组合执行定义。" /> : null}
          <div className="job-card-list">{jobs.map((job) => <article key={job.id}>
            <div className="job-card-heading"><div><strong>{job.name}</strong><small>{shortId(job.id)}</small></div><Badge tone={job.enabled ? "success" : "neutral"}>{job.status}</Badge></div>
            <div className="job-links"><span>Account <code>{shortId(job.accountId)}</code></span><span>Message <code>{shortId(job.messageId)}</code></span><span>Audience <code>{shortId(job.audienceId)}</code></span></div>
            <div className="job-actions"><Button disabled={!job.enabled || runningJob === job.id} onClick={() => void dryRun(job)}>Dry Run</Button><Button variant="primary" disabled={!job.enabled || runningJob === job.id} onClick={() => void prepareLive(job)}>正式发送</Button><Button variant="ghost" onClick={() => editJob(job)}>编辑</Button><Button variant="ghost" onClick={() => void jobAction(job, "copy")}>复制</Button><Button variant="ghost" onClick={() => void jobAction(job, "toggle")}>{job.enabled ? "停用" : "启用"}</Button>{!job.archived ? <Button variant="ghost" onClick={() => void jobAction(job, "archive")}>归档</Button> : null}</div>
          </article>)}</div>
          {confirmation ? <div className="live-confirmation"><h3>正式发送确认</h3><p>Provider: {confirmation.providerId} {confirmation.providerVersion}</p><p>账号: {confirmation.accountName}</p><p>受众: {confirmation.audienceName} · {confirmation.audienceCount} 人</p><p>目标并发: {confirmation.targetConcurrency} · 限速: {confirmation.rateLimitPermits === Number.MAX_SAFE_INTEGER ? "无限制" : `${confirmation.rateLimitPermits}/${confirmation.rateLimitPeriod}`}</p><p>预计发送规模: {confirmation.estimatedItems}</p><pre>{JSON.stringify(confirmation.policies, null, 2)}</pre><div className="form-actions"><Button variant="primary" onClick={() => void sendLive()}>我已核对，开始发送</Button><Button onClick={() => setConfirmation(undefined)}>取消</Button></div></div> : null}
        </section>
      </div>
      <section className="panel recent-panel schedule-panel">
        <PanelHeader title="Cron 调度" description="Service 以数据库幂等键触发，PostgreSQL Server 模式由 advisory lock 选主" />
        <div className="schedule-composer">
          <label className="simple-field"><span>任务</span><select value={scheduleJobId} onChange={(event) => setScheduleJobId(event.target.value)}><option value="">请选择</option>{jobs.map((job) => <option key={job.id} value={job.id}>{job.name}</option>)}</select></label>
          <label className="simple-field"><span>六段 Cron</span><input value={cronExpression} onChange={(event) => setCronExpression(event.target.value)} /></label>
          <Button variant="primary" disabled={!scheduleJobId || !cronExpression.trim()} onClick={() => void createSchedule()}>创建调度</Button>
        </div>
        {schedules.length === 0 ? <EmptyState icon={<Icon name="task" />} title="还没有调度" description="选择一个已启用任务并设置 Cron 表达式。" /> : null}
        <div className="resource-card-list">{schedules.map((schedule) => <article key={schedule.id}>
          <span className="resource-card-icon"><Icon name="task" /></span>
          <div><strong>{schedule.name}</strong><small>{schedule.cronExpression} · {schedule.timezone}</small><code>next {new Date(schedule.nextFireAt).toLocaleString()}</code></div>
          <div className="heading-actions"><Button variant="ghost" onClick={() => void editSchedule(schedule)}>完整编辑</Button><Button variant="ghost" onClick={() => void toggleSchedule(schedule)}>{schedule.enabled ? "停用" : "启用"}</Button><Button variant="ghost" onClick={() => void client.deleteSchedule(schedule.id, workspaceId).then(load)}>删除</Button></div>
        </article>)}</div>
      </section>
    </div>
  );
}

interface VisibleRunEvent {
  id: string;
  type: string;
  data: string;
}

function RunsPage({ client, workspaceId, onNavigate }: { client: WePushClient; workspaceId: string; onNavigate: (page: PageId) => void }) {
  const [runs, setRuns] = useState<Run[]>([]);
  const [selectedId, setSelectedId] = useState<string>();
  const [events, setEvents] = useState<VisibleRunEvent[]>([]);
  const [results, setResults] = useState<RunItemResult[]>([]);
  const [resultsCursor, setResultsCursor] = useState<string>();
  const [runsCursor, setRunsCursor] = useState<string>();
  const [runSearch, setRunSearch] = useState("");
  const [runStatus, setRunStatus] = useState("");
  const [resultsLoading, setResultsLoading] = useState(false);
  const [artifacts, setArtifacts] = useState<Artifact[]>([]);
  const [artifactsLoading, setArtifactsLoading] = useState(false);
  const [exportBusy, setExportBusy] = useState(false);
  const [downloadBusy, setDownloadBusy] = useState<string>();
  const [commandBusy, setCommandBusy] = useState<string>();
  const [commandError, setCommandError] = useState<string>();
  const [concurrency, setConcurrency] = useState(8);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [retryConfirmation, setRetryConfirmation] = useState<import("@wepush-next/api-client").RetryConfirmation>();
  const selected = runs.find((run) => run.id === selectedId) ?? runs[0];

  const refreshRuns = useCallback(async (append = false) => {
    try {
      const page = await client.runPage({ cursor: append ? runsCursor : undefined,
        name: runSearch || undefined, status: runStatus || undefined }, workspaceId);
      setRuns((current) => append ? [...current, ...page.items] : page.items);
      setRunsCursor(page.page.nextCursor);
      setSelectedId((current) => current ?? page.items[0]?.id);
      setError(undefined);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "运行记录加载失败");
    } finally {
      setLoading(false);
    }
  }, [client, runSearch, runStatus, runsCursor, workspaceId]);

  useEffect(() => {
    void refreshRuns();
    const timer = window.setInterval(() => void refreshRuns(), 1_500);
    return () => window.clearInterval(timer);
  }, [refreshRuns]);

  const refreshResults = useCallback(async () => {
    if (!selectedId) { setResults([]); setResultsCursor(undefined); return; }
    setResultsLoading(true);
    try {
      const page = await client.runItems(selectedId, undefined, 100, workspaceId);
      setResults(page.items);
      setResultsCursor(page.page.nextCursor);
    } catch (nextError) {
      setCommandError(nextError instanceof Error ? nextError.message : "Item Result 加载失败");
    } finally {
      setResultsLoading(false);
    }
  }, [client, selectedId, workspaceId]);

  useEffect(() => {
    void refreshResults();
    const timer = window.setInterval(() => void refreshResults(), 1_500);
    return () => window.clearInterval(timer);
  }, [refreshResults]);

  const refreshArtifacts = useCallback(async () => {
    if (!selectedId) { setArtifacts([]); return; }
    setArtifactsLoading(true);
    try {
      setArtifacts(await client.runArtifacts(selectedId, workspaceId));
    } catch (nextError) {
      setCommandError(nextError instanceof Error ? nextError.message : "Artifact 加载失败");
    } finally {
      setArtifactsLoading(false);
    }
  }, [client, selectedId, workspaceId]);

  useEffect(() => {
    void refreshArtifacts();
  }, [refreshArtifacts]);

  useEffect(() => {
    if (!selected?.id) return;
    setEvents([]);
    const controller = new AbortController();
    let lastSequence = "";
    void (async () => {
      while (!controller.signal.aborted) {
        try {
          lastSequence = await client.streamRunEvents(selected.id, lastSequence, (event) => {
            setEvents((current) => [...current, event].slice(-200));
            void refreshRuns();
          }, workspaceId, controller.signal);
        } catch (problem) {
          if (controller.signal.aborted) return;
          await new Promise((resolve) => window.setTimeout(resolve, 1_000));
        }
      }
    })();
    return () => controller.abort();
  }, [client, refreshRuns, selected?.id, workspaceId]);

  async function loadMoreResults() {
    if (!selected?.id || !resultsCursor) return;
    setResultsLoading(true);
    try {
      const page = await client.runItems(selected.id, resultsCursor, 100, workspaceId);
      setResults((current) => [...current, ...page.items]);
      setResultsCursor(page.page.nextCursor);
    } catch (nextError) {
      setCommandError(nextError instanceof Error ? nextError.message : "下一页加载失败");
    } finally {
      setResultsLoading(false);
    }
  }

  async function issueCommand(type: "pause" | "resume" | "cancel" | "concurrency") {
    if (!selected) return;
    setCommandBusy(type); setCommandError(undefined);
    try {
      const key = crypto.randomUUID();
      if (type === "pause") await client.pauseRun(selected.id, key, workspaceId);
      if (type === "resume") await client.resumeRun(selected.id, key, workspaceId);
      if (type === "cancel") await client.cancelRun(selected.id, "web-ui", key, workspaceId);
      if (type === "concurrency") await client.changeRunConcurrency(selected.id, concurrency, key, workspaceId);
      await refreshRuns();
    } catch (nextError) {
      setCommandError(nextError instanceof Error ? nextError.message : "运行命令失败");
    } finally {
      setCommandBusy(undefined);
    }
  }

  async function createResultExport() {
    if (!selected) return;
    setExportBusy(true); setCommandError(undefined);
    try {
      await client.createResultExport(selected.id, workspaceId);
      await refreshArtifacts();
    } catch (nextError) {
      setCommandError(nextError instanceof Error ? nextError.message : "结果导出失败");
    } finally {
      setExportBusy(false);
    }
  }

  async function downloadArtifact(artifact: Artifact) {
    setDownloadBusy(artifact.id); setCommandError(undefined);
    try {
      const blob = await client.downloadArtifact(artifact.id, workspaceId);
      downloadBlob(blob, artifact.originalName);
    } catch (nextError) {
      setCommandError(nextError instanceof Error ? nextError.message : "Artifact 下载失败");
    } finally {
      setDownloadBusy(undefined);
    }
  }

  async function prepareRetry() {
    if (!selected) return;
    try { setRetryConfirmation(await client.confirmRetry(selected.id,
      ["FAILED", "UNKNOWN", "UNSENT"], workspaceId)); }
    catch (nextError) { setCommandError(nextError instanceof Error ? nextError.message : "重发预览失败"); }
  }

  async function retryFailedItems() {
    if (!retryConfirmation) return; setCommandBusy("retry");
    try {
      const created = await client.retryRun(retryConfirmation.sourceRunId, retryConfirmation.states,
        retryConfirmation.confirmationToken, crypto.randomUUID(), workspaceId);
      setRetryConfirmation(undefined); await refreshRuns(); setSelectedId(created.id);
    } catch (nextError) { setCommandError(nextError instanceof Error ? nextError.message : "重发创建失败"); }
    finally { setCommandBusy(undefined); }
  }

  return (
    <div className="page runs-page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">EXECUTION OBSERVABILITY</p><h2>运行中心</h2><p>查看 SQLite 中的运行快照、实时状态、结果计数与可重放事件。</p></div>
        <Button variant="primary" onClick={() => onNavigate("jobs")}>＋ 发起运行</Button>
      </section>
      {error ? <div className="connection-banner"><div className="banner-icon">!</div><div><strong>运行数据暂不可用</strong><p>{error}</p></div><Button onClick={() => void refreshRuns()}>重试</Button></div> : null}
      <div className="runs-layout">
        <section className="panel runs-list-panel">
          <div className="list-toolbar"><strong>最近运行</strong><Badge>{runs.length}</Badge><input placeholder="任务名" value={runSearch} onChange={(event) => setRunSearch(event.target.value)} /><select value={runStatus} onChange={(event) => setRunStatus(event.target.value)}><option value="">全部状态</option>{["PENDING", "RUNNING", "PAUSED", "SUCCEEDED", "PARTIAL", "FAILED", "CANCELLED"].map((state) => <option key={state}>{state}</option>)}</select><Button variant="ghost" onClick={() => void refreshRuns()}>筛选</Button></div>
          {loading ? <div className="loading-row"><Spinner />正在加载运行…</div> : null}
          {!loading && runs.length === 0 ? <EmptyState icon={<Icon name="pulse" />} title="还没有运行" description="创建 Job 后先发起一次 Dry Run 验证完整链路。" action={<Button onClick={() => onNavigate("jobs")}>创建任务</Button>} /> : null}
          {runs.map((run) => <button type="button" key={run.id} className={selected?.id === run.id ? "run-list-item run-list-item--active" : "run-list-item"} onClick={() => setSelectedId(run.id)}>
            <span className={`run-state-dot run-state-dot--${run.state.toLowerCase()}`} />
            <span><strong>{run.dryRun ? "Dry Run" : "Run"} · {shortId(run.id)}</strong><small>{formatTime(run.createdAt)} · {run.counters.total} items</small></span>
            <Badge tone={runTone(run.state)}>{run.state}</Badge>
          </button>)}
          {runsCursor ? <div className="load-more-row"><Button variant="ghost" onClick={() => void refreshRuns(true)}>加载更多 Runs</Button></div> : null}
        </section>
        <section className="panel run-detail-panel">
          {selected ? <>
            <div className="run-detail-heading"><div><p className="eyebrow">{selected.dryRun ? "DRY RUN" : selected.sourceRunId ? "RETRY RUN" : "LIVE RUN"}</p><h3>{selected.id}</h3><p>{selected.jobName} · {selected.sourceRunId ? `source ${selected.sourceRunId}` : selected.jobId}</p></div><Badge tone={runTone(selected.state)}>{selected.state}</Badge></div>
            <div className="run-command-bar">
              {selected.state === "RUNNING" ? <Button disabled={Boolean(commandBusy)} onClick={() => void issueCommand("pause")}>暂停</Button> : null}
              {selected.state === "PAUSED" ? <Button disabled={Boolean(commandBusy)} onClick={() => void issueCommand("resume")}>恢复</Button> : null}
              {["RUNNING", "PAUSED"].includes(selected.state) ? <><label><span>并发</span><input type="number" min={1} max={256} value={concurrency} onChange={(event) => setConcurrency(Number(event.target.value))} /></label><Button disabled={Boolean(commandBusy)} onClick={() => void issueCommand("concurrency")}>应用</Button></> : null}
              {["PENDING", "RUNNING", "PAUSED", "RECOVERING"].includes(selected.state) ? <Button variant="ghost" disabled={Boolean(commandBusy)} onClick={() => void issueCommand("cancel")}>取消运行</Button> : null}
              {["CANCELLED", "SUCCEEDED", "PARTIAL", "FAILED"].includes(selected.state) && (selected.counters.failed + selected.counters.unknown + selected.counters.unsent > 0) ? <Button variant="primary" disabled={Boolean(commandBusy)} onClick={() => void prepareRetry()}>重发失败项</Button> : null}
              {commandBusy ? <span><Spinner />正在提交 {commandBusy}…</span> : null}
            </div>
            {commandError ? <div className="inline-error">{commandError}</div> : null}
            {retryConfirmation ? <div className="live-confirmation"><h3>重发确认</h3><p>来源 Run: {retryConfirmation.sourceRunId}</p><p>状态: {retryConfirmation.states.join(", ")}</p><p>将创建关联的新 Run，共 {retryConfirmation.itemCount} 个 Item。</p><div className="form-actions"><Button variant="primary" onClick={() => void retryFailedItems()}>确认重发</Button><Button onClick={() => setRetryConfirmation(undefined)}>取消</Button></div></div> : null}
            <div className="run-counter-grid">
              {Object.entries(selected.counters).map(([name, value]) => <div key={name}><span>{name}</span><strong>{value}</strong></div>)}
            </div>
            <div className="event-heading result-heading"><div><h3>Item Results</h3><p>持久化明细 · HMAC 游标分页 · 同 Item 幂等写入</p></div><div className="heading-actions"><Badge tone="neutral">{results.length} loaded</Badge>{["CANCELLED", "SUCCEEDED", "PARTIAL", "FAILED"].includes(selected.state) ? <Button variant="ghost" disabled={exportBusy} onClick={() => void createResultExport()}>{exportBusy ? "生成中…" : "导出 CSV"}</Button> : null}</div></div>
            <div className="result-table">
              <div className="result-row result-row--header"><span>Item</span><span>State</span><span>Attempts</span><span>Provider</span><span>Completed</span></div>
              {results.map((result) => <div className="result-row" key={result.itemId}>
                <span><strong>{result.itemId}</strong><small>{result.externalRequestId || "no external id"}</small></span>
                <span><Badge tone={runTone(result.state)}>{result.state}</Badge></span>
                <span>{result.attempts}</span><span>{result.providerCode || "—"}</span><span>{formatTime(result.completedAt)}</span>
              </div>)}
              {!resultsLoading && results.length === 0 ? <div className="result-empty">运行产生结果后会在这里逐项显示。</div> : null}
            </div>
            {resultsCursor ? <div className="load-more-row"><Button variant="ghost" disabled={resultsLoading} onClick={() => void loadMoreResults()}>{resultsLoading ? "加载中…" : "加载更多"}</Button></div> : null}
            <div className="artifact-section">
              <div className="artifact-heading"><div><h3>Artifacts</h3><p>本地文件存储 · SHA-256 校验 · 默认保留 24 小时</p></div><Badge tone="info">{artifacts.length}</Badge></div>
              {artifactsLoading ? <div className="loading-row"><Spinner />正在加载导出文件…</div> : null}
              {!artifactsLoading && artifacts.length === 0 ? <div className="artifact-empty">运行结束后可生成脱敏的 Item Results CSV。</div> : null}
              <div className="artifact-list">{artifacts.map((artifact) => <article key={artifact.id}>
                <span className="artifact-file-icon">CSV</span>
                <span><strong>{artifact.originalName}</strong><small>{formatBytes(artifact.size)} · SHA {artifact.sha256.slice(0, 12)}… · {formatTime(artifact.expiresAt)} 过期</small></span>
                <Badge tone={artifact.state === "READY" ? "success" : artifact.state === "FAILED" ? "danger" : "neutral"}>{artifact.state}</Badge>
                {artifact.state === "READY" ? <Button disabled={downloadBusy === artifact.id} onClick={() => void downloadArtifact(artifact)}>{downloadBusy === artifact.id ? "下载中…" : "下载"}</Button> : null}
              </article>)}</div>
            </div>
            <div className="event-heading"><div><h3>事件流</h3><p>SSE 实时订阅 · 断线后按 Last-Event-ID 回放</p></div><Badge tone="info">{events.length} events</Badge></div>
            <div className="event-timeline">
              {events.length === 0 ? <div className="response-placeholder"><Icon name="pulse" /><p>等待运行事件…</p></div> : events.map((event) => <article key={`${event.id}:${event.type}`}><span>{event.id}</span><div><strong>{event.type}</strong><pre>{prettyBody(event.data)}</pre></div></article>)}
            </div>
          </> : <EmptyState icon={<Icon name="pulse" />} title="选择一次运行" description="这里会显示状态、计数和可重放事件。" />}
        </section>
      </div>
    </div>
  );
}

function AgentsPage({ client }: { client: WePushClient }) {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [selectedId, setSelectedId] = useState<string>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const selected = agents.find((agent) => agent.id === selectedId) ?? agents[0];

  const refresh = useCallback(async () => {
    try {
      const nextAgents = await client.agents();
      setAgents(nextAgents);
      setSelectedId((current) => current ?? nextAgents[0]?.id);
      setError(undefined);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "Agent 状态加载失败");
    } finally {
      setLoading(false);
    }
  }, [client]);

  useEffect(() => {
    void refresh();
    const timer = window.setInterval(() => void refresh(), 3_000);
    return () => window.clearInterval(timer);
  }, [refresh]);

  const online = agents.filter((agent) => agent.status === "ONLINE").length;
  const maximumRuns = agents.reduce((sum, agent) => sum + agent.maximumRuns, 0);
  const activeRuns = agents.reduce((sum, agent) => sum + agent.activeRuns, 0);

  return (
    <div className="page agents-page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">DISTRIBUTED EXECUTION</p><h2>Agents</h2><p>观察 gRPC 长连接、心跳、执行容量以及每个节点提供的 Provider 能力。</p></div>
        <Button onClick={() => void refresh()}>刷新状态</Button>
      </section>
      {error ? <div className="connection-banner"><div className="banner-icon">!</div><div><strong>Agent 状态暂不可用</strong><p>{error}</p></div><Button onClick={() => void refresh()}>重试</Button></div> : null}
      <section className="metric-grid">
        <MetricCard label="Registered" value={loading ? "—" : String(agents.length)} detail="保留离线节点记录" />
        <MetricCard label="Online" value={loading ? "—" : String(online)} detail="最近心跳正常" tone="success" />
        <MetricCard label="Capacity" value={loading ? "—" : String(maximumRuns)} detail="最大并发 Run" tone="info" />
        <MetricCard label="Active" value={loading ? "—" : String(activeRuns)} detail={`${Math.max(0, maximumRuns - activeRuns)} slots available`} />
      </section>
      <div className="agents-layout">
        <section className="panel agent-list-panel">
          <div className="list-toolbar"><strong>执行节点</strong><Badge>{agents.length}</Badge></div>
          {loading ? <div className="loading-row"><Spinner />正在读取 Agent 注册表…</div> : null}
          {!loading && agents.length === 0 ? <EmptyState icon={<Icon name="agent" />} title="还没有 Agent" description="启动 wepush-next-agent，并让它连接 Service 的 19090 gRPC 端口。" /> : null}
          {agents.map((agent) => <button type="button" key={agent.id} className={selected?.id === agent.id ? "agent-list-item agent-list-item--active" : "agent-list-item"} onClick={() => setSelectedId(agent.id)}>
            <span className={`status-dot ${agent.status === "ONLINE" ? "status-dot--online" : ""}`} />
            <span><strong>{agent.id}</strong><small>{agent.operatingSystem} · {agent.architecture} · last seen {formatTime(agent.lastSeenAt)}</small></span>
            <Badge tone={agentTone(agent.status)}>{agent.status}</Badge>
          </button>)}
        </section>
        <section className="panel agent-detail-panel">
          {selected ? <>
            <div className="agent-detail-heading"><div><p className="eyebrow">AGENT SESSION</p><h3>{selected.id}</h3><p>{selected.sessionId}</p></div><Badge tone={agentTone(selected.status)}>{selected.status}</Badge></div>
            <div className="agent-capacity">
              <div><span>Active runs</span><strong>{selected.activeRuns}</strong></div>
              <div><span>Available</span><strong>{selected.availableRuns}</strong></div>
              <div><span>Maximum</span><strong>{selected.maximumRuns}</strong></div>
              <div><span>Protocol</span><strong>v{selected.protocolVersion}</strong></div>
            </div>
            <div className="agent-progress"><span style={{ width: `${selected.maximumRuns ? Math.min(100, selected.activeRuns / selected.maximumRuns * 100) : 0}%` }} /></div>
            <div className="agent-metadata">
              <span><small>Agent version</small><strong>{selected.agentVersion}</strong></span>
              <span><small>Runtime</small><strong>Java {selected.javaVersion}</strong></span>
              <span><small>Connected</small><strong>{formatTime(selected.connectedAt)}</strong></span>
              <span><small>Last heartbeat</small><strong>{formatTime(selected.lastSeenAt)}</strong></span>
              <span><small>Agent sequence</small><strong>{selected.lastAgentSequence}</strong></span>
              <span><small>Service sequence</small><strong>{selected.lastServiceSequence}</strong></span>
            </div>
            <div className="event-heading"><div><h3>Provider capabilities</h3><p>该 Agent 可实际装载和执行的 Provider 版本</p></div><Badge tone="info">{selected.providers.length}</Badge></div>
            <div className="agent-provider-list">{selected.providers.map((provider) => <article key={`${provider.providerId}:${provider.implementationVersion}`}>
              <span className="provider-logo">{provider.providerId.slice(0, 1).toUpperCase()}</span>
              <span><strong>{provider.providerId}</strong><small>v{provider.implementationVersion} · SPI {provider.spiMajor}</small></span>
              <span><small>并发</small><strong>{provider.maximumConcurrency}</strong></span>
            </article>)}</div>
          </> : <EmptyState icon={<Icon name="agent" />} title="选择一个 Agent" description="会话与容量详情会显示在这里。" />}
        </section>
      </div>
    </div>
  );
}

interface ApiEndpoint {
  method: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  path: string;
  title: string;
  description: string;
  exampleBody?: string;
}

const apiEndpoints: ApiEndpoint[] = [
  { method: "GET", path: "/actuator/health", title: "Health", description: "检查 Service 是否可用" },
  { method: "GET", path: "/api/v1/system/info", title: "System info", description: "读取产品版本、模式与服务器时间" },
  { method: "GET", path: "/api/v1/providers", title: "List providers", description: "返回当前进程发现的 Provider 清单" },
  { method: "GET", path: "/api/v1/agents", title: "List agents", description: "读取 Agent 会话、容量、心跳和 Provider 能力" },
  { method: "GET", path: "/api/v1/workspaces/ws_default/accounts", title: "List accounts", description: "读取当前工作区的 Provider 账号" },
  { method: "GET", path: "/api/v1/workspaces/ws_default/messages", title: "List messages", description: "读取消息模板及当前不可变修订" },
  { method: "GET", path: "/api/v1/workspaces/ws_default/audiences", title: "List audiences", description: "读取受众及当前快照元数据" },
  { method: "GET", path: "/api/v1/workspaces/ws_default/jobs", title: "List jobs", description: "读取任务组合与执行策略" },
  { method: "GET", path: "/api/v1/workspaces/ws_default/runs", title: "List runs", description: "读取执行状态和结果计数" },
  { method: "GET", path: "/api/v1/workspaces/ws_default/schedules", title: "List schedules", description: "读取 Cron 调度定义" },
  { method: "POST", path: "/api/v1/workspaces/ws_default/schedules", title: "Create schedule", description: "为任务创建数据库幂等调度", exampleBody: '{\n  "name": "Daily",\n  "jobId": "job_id",\n  "cronExpression": "0 0 9 * * *",\n  "timezone": "Asia/Shanghai",\n  "misfirePolicy": "FIRE_ONCE",\n  "enabled": true\n}' },
  { method: "GET", path: "/api/v1/workspaces/ws_default/audit-events", title: "Audit events", description: "读取工作区审计日志" },
  { method: "POST", path: "/api/v1/workspaces/ws_default/agent-enrollment-tokens", title: "Create enrollment token", description: "为当前 Workspace 签发一次性 Agent 注册令牌", exampleBody: '{\n  "name": "new-agent",\n  "ttl": "PT15M"\n}' },
];

function ApiDocsPage({ client }: { client: WePushClient }) {
  const [selected, setSelected] = useState<ApiEndpoint>(apiEndpoints[1] ?? apiEndpoints[0]!);
  const [response, setResponse] = useState<DebugResponse>();
  const [running, setRunning] = useState(false);
  const [openApi, setOpenApi] = useState<string>();
  const [requestPath, setRequestPath] = useState(selected.path);
  const [requestBody, setRequestBody] = useState(selected.exampleBody ?? "");

  useEffect(() => {
    const controller = new AbortController();
    void client.openApi(controller.signal).then(setOpenApi).catch(() => setOpenApi(undefined));
    return () => controller.abort();
  }, [client]);

  async function execute() {
    if (selected.method !== "GET" && !window.confirm(
      `确认以 ${selected.method} 调用 ${requestPath}？写操作将按当前身份执行并进入服务端审计。`,
    )) return;
    setRunning(true);
    try {
      if (requestBody.trim()) JSON.parse(requestBody);
      setResponse(await client.debugRequest(requestPath, selected.method, requestBody || undefined));
    } catch (error) {
      setResponse({ status: 0, statusText: "Client error", durationMs: 0, headers: {},
        body: error instanceof Error ? error.message : "请求失败" });
    } finally {
      setRunning(false);
    }
  }

  return (
    <div className="page api-docs-page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">LIVE CONTRACT</p><h2>API 文档</h2><p>基于当前 Service 契约查看接口，并在本地身份下动态调试。</p></div>
        <a className="wp-button wp-button--secondary" href={`${client.baseUrl}/openapi.yaml`} target="_blank" rel="noreferrer">OpenAPI YAML ↗</a>
      </section>
      <div className="api-layout">
        <aside className="panel endpoint-list">
          <div className="endpoint-search"><Icon name="search" /><input aria-label="筛选接口" placeholder="筛选接口…" /></div>
          <div className="endpoint-group"><h3>Service API <Badge tone="success">{openApi ? "live" : "offline"}</Badge></h3>
            {apiEndpoints.map((endpoint) => (
              <button type="button" key={`${endpoint.method}:${endpoint.path}`} className={endpoint.path === selected.path && endpoint.method === selected.method ? "endpoint-item endpoint-item--active" : "endpoint-item"} onClick={() => { setSelected(endpoint); setRequestPath(endpoint.path); setRequestBody(endpoint.exampleBody ?? ""); setResponse(undefined); }}>
                <span className="http-method">{endpoint.method}</span><span><strong>{endpoint.title}</strong><small>{endpoint.path}</small></span>
              </button>
            ))}
          </div>
        </aside>
        <section className="panel api-console">
          <div className="api-console-heading"><Badge tone="info">{selected.method}</Badge><input className="api-path-input" value={requestPath} onChange={(event) => setRequestPath(event.target.value)} aria-label="请求路径" /><Button variant="primary" onClick={() => void execute()} disabled={running}>{running ? "发送中…" : "发送请求"}</Button></div>
          <p className="api-description">{selected.description}</p>
          <div className="request-preview"><span>Request URL</span><code>{client.baseUrl || window.location.origin}{requestPath}</code></div>
          {selected.method !== "GET" && selected.method !== "DELETE" ? <label className="simple-field"><span>JSON Request Body</span><textarea rows={10} value={requestBody} onChange={(event) => setRequestBody(event.target.value)} spellCheck={false} /></label> : null}
          <div className="response-heading"><h3>Response</h3>{response ? <span><Badge tone={response.status < 400 ? "success" : "danger"}>{response.status} {response.statusText}</Badge> {response.durationMs} ms</span> : null}</div>
          {response ? <pre className="response-body"><code>{prettyBody(response.body)}</code></pre> : <div className="response-placeholder"><Icon name="code" /><p>发送请求后，结构化响应会显示在这里。</p></div>}
          <div className="api-safety-note"><span>i</span><p><strong>动态调试安全规则</strong><br />Secret 示例只显示占位符；所有写操作发送前二次确认，权限和审计仍由 Service 强制执行；API Client 拒绝跨源 URL，避免 Bearer Token 外发。</p></div>
        </section>
      </div>
    </div>
  );
}

function SettingsPage({ client, workspaceId }: { client: WePushClient; workspaceId: string }) {
  const [token, setToken] = useState(() => { try { return sessionStorage.getItem("wepush.apiToken") ?? ""; } catch { return ""; } });
  const [tokens, setTokens] = useState<ApiTokenSummary[]>([]);
  const [audits, setAudits] = useState<AuditEvent[]>([]);
  const [issued, setIssued] = useState<string>();
  const [enrollment, setEnrollment] = useState<string>();
  const [error, setError] = useState<string>();
  const [auditSearch, setAuditSearch] = useState("");
  const [auditStatus, setAuditStatus] = useState("");
  const [auditCursor, setAuditCursor] = useState<string>();
  const [serviceStatus, setServiceStatus] = useState<DesktopServiceStatus>();
  const [operation, setOperation] = useState<DesktopCommandResult>();
  const [operationBusy, setOperationBusy] = useState(false);
  const desktop = window.wepushDesktop;

  const load = useCallback(async () => {
    try {
      const [nextTokens, nextAudits] = await Promise.all([client.apiTokens(workspaceId), client.auditEvents(50, workspaceId)]);
      setTokens(nextTokens); setAudits(nextAudits); setError(undefined);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "安全数据加载失败");
    }
  }, [client, workspaceId]);
  useEffect(() => { if (token) void load(); }, [load, token]);
  useEffect(() => {
    if (!desktop) return;
    let active = true;
    void Promise.all([desktop.token.load(), desktop.service.status()]).then(([saved, status]) => {
      if (!active) return;
      setToken(saved); client.setToken(saved); setServiceStatus(status);
    }).catch((nextError: unknown) => {
      if (active) setError(nextError instanceof Error ? nextError.message : "Desktop 本机状态读取失败");
    });
    return () => { active = false; };
  }, [client, desktop]);

  async function saveToken() {
    client.setToken(token);
    try {
      if (desktop) await desktop.token.save(token);
      else sessionStorage.setItem("wepush.apiToken", token);
      await load();
    } catch (nextError) { setError(nextError instanceof Error ? nextError.message : "Token 保存失败"); }
  }

  async function clearToken() {
    client.setToken(""); setToken("");
    try {
      if (desktop) await desktop.token.clear();
      else sessionStorage.removeItem("wepush.apiToken");
    } catch (nextError) { setError(nextError instanceof Error ? nextError.message : "Token 清除失败"); }
  }

  async function serviceOperation(action: "start" | "stop" | "logs" | "diagnose") {
    if (!desktop) return;
    setOperationBusy(true);
    try {
      const result = await desktop.service[action]();
      setOperation(result);
      setServiceStatus(await desktop.service.status());
    } catch (nextError) {
      setOperation({ ok: false, message: nextError instanceof Error ? nextError.message : "本机服务操作失败", output: "" });
    } finally { setOperationBusy(false); }
  }

  async function loadAudits(append = false) {
    try {
      const page = await client.auditPage({ name: auditSearch || undefined,
        status: auditStatus || undefined, cursor: append ? auditCursor : undefined }, workspaceId);
      setAudits((current) => append ? [...current, ...page.items] : page.items);
      setAuditCursor(page.page.nextCursor);
    } catch (nextError) { setError(nextError instanceof Error ? nextError.message : "审计加载失败"); }
  }

  async function createToken() {
    try {
      const result = await client.createApiToken({ name: "Web UI operator", workspaceId, role: "OPERATOR", ttl: "P30D" });
      setIssued(result.token); await load();
    } catch (nextError) { setError(nextError instanceof Error ? nextError.message : "令牌签发失败"); }
  }

  async function createEnrollment() {
    try { setEnrollment((await client.createAgentEnrollmentToken("Web UI enrollment", "PT15M", workspaceId)).token); }
    catch (nextError) { setError(nextError instanceof Error ? nextError.message : "注册令牌签发失败"); }
  }

  return <div className="page settings-page">
    <section className="page-heading page-heading--compact"><div><p className="eyebrow">SECURITY & OPERATIONS</p><h2>设置</h2><p>管理本机 API 身份、Agent 注册和只追加审计日志。</p></div><Badge tone="info">Workspace RBAC</Badge></section>
    {error ? <div className="inline-error">{error}</div> : null}
    <div className="dashboard-grid">
      <section className="panel composer-panel compact-form"><PanelHeader title="当前 API Token" description={desktop ? "由操作系统原生安全存储加密保存" : "仅保存在当前浏览器标签页会话中，关闭后清除"} />
        <label className="simple-field"><span>Bearer Token</span><input type="password" value={token} onChange={(event) => setToken(event.target.value)} placeholder="wpu.… 或 bootstrap token" /></label>
        <div className="form-actions"><Button variant="primary" onClick={() => void saveToken()}>保存并验证</Button><Button onClick={() => void clearToken()}>清除</Button><Button onClick={() => void createToken()}>签发 30 天 Operator</Button></div>
        {issued ? <div className="one-time-secret"><strong>只显示一次</strong><code>{issued}</code></div> : null}
      </section>
      <section className="panel composer-panel compact-form"><PanelHeader title="Agent Enrollment" description="一次性、短时有效，完成注册后自动失效" />
        <Button variant="primary" onClick={() => void createEnrollment()}>生成 15 分钟注册令牌</Button>
        {enrollment ? <div className="one-time-secret"><strong>只显示一次</strong><code>{enrollment}</code></div> : null}
      </section>
    </div>
    {desktop ? <section className="panel recent-panel">
      <PanelHeader title="本机 WePush Next Service" description="检测、启动、停止、读取最近日志，并生成不包含 Token/Secret 的诊断结果。" action={<Badge tone={serviceStatus?.running ? "success" : serviceStatus?.installed ? "warning" : "neutral"}>{serviceStatus?.running ? "RUNNING" : serviceStatus?.installed ? "STOPPED" : "NOT INSTALLED"}</Badge>} />
      {serviceStatus?.detail ? <pre className="response-body"><code>{serviceStatus.detail}</code></pre> : null}
      {operation ? <div className={operation.ok ? "inline-success" : "inline-error"}>{operation.message}</div> : null}
      {operation?.output && operation.output !== operation.message ? <pre className="response-body"><code>{operation.output}</code></pre> : null}
      <div className="form-actions">
        <Button variant="primary" disabled={operationBusy || serviceStatus?.running} onClick={() => void serviceOperation("start")}>启动</Button>
        <Button disabled={operationBusy || !serviceStatus?.running} onClick={() => void serviceOperation("stop")}>停止</Button>
        <Button disabled={operationBusy} onClick={() => void serviceOperation("logs")}>最近日志</Button>
        <Button disabled={operationBusy} onClick={() => void serviceOperation("diagnose")}>诊断</Button>
      </div>
    </section> : null}
    <section className="panel recent-panel"><PanelHeader title="API Tokens" description="Token 明文不会再次返回" action={<Button variant="ghost" onClick={() => void load()}>刷新</Button>} />
      <div className="resource-card-list">{tokens.map((item) => <article key={item.tokenId}><span className="resource-card-icon"><Icon name="key" /></span><div><strong>{item.name}</strong><small>{shortId(item.principalId)} · expires {new Date(item.expiresAt).toLocaleString()}</small><code>{shortId(item.tokenId)}</code></div><Badge tone={item.revokedAt ? "neutral" : "success"}>{item.revokedAt ? "REVOKED" : "ACTIVE"}</Badge></article>)}</div>
    </section>
    <section className="panel recent-panel"><PanelHeader title="Audit Events" description="游标分页的工作区访问和变更记录" />
      <div className="list-toolbar"><input placeholder="Actor / action / resource" value={auditSearch} onChange={(event) => setAuditSearch(event.target.value)} /><select value={auditStatus} onChange={(event) => setAuditStatus(event.target.value)}><option value="">全部结果</option><option>SUCCESS</option><option>FAILURE</option><option>DENIED</option></select><Button variant="ghost" onClick={() => void loadAudits()}>筛选</Button></div>
      <div className="audit-list">{audits.map((item) => <article key={item.id}><Badge tone={item.result === "SUCCESS" ? "success" : item.result === "DENIED" ? "danger" : "neutral"}>{item.result}</Badge><div><strong>{item.action}</strong><small>{item.actorId} · {new Date(item.occurredAt).toLocaleString()}</small></div></article>)}</div>
      {auditCursor ? <div className="load-more-row"><Button variant="ghost" onClick={() => void loadAudits(true)}>加载更多审计</Button></div> : null}
    </section>
  </div>;
}

function ComingSoon({ page, onNavigate }: { page: PageId; onNavigate: (page: PageId) => void }) {
  return (
    <div className="page coming-page">
      <EmptyState icon={<Icon name={navigation.flatMap((group) => group.items).find((item) => item.id === page)?.icon ?? "task"} />} title={`${pageTitles[page]}正在构建`} description="模块入口和依赖边界已经预留，将在单机闭环 API 就绪后接入真实数据。" action={<Button onClick={() => onNavigate("overview")}>返回总览</Button>} />
    </div>
  );
}

function MetricCard({ label, value, detail, tone = "neutral" }: { label: string; value: string; detail: string; tone?: "neutral" | "success" | "info" }) {
  return <article className="metric-card"><div className="metric-card__top"><span>{label}</span><i className={`metric-status metric-status--${tone}`} /></div><strong>{value}</strong><p>{detail}</p></article>;
}

function agentTone(status: Agent["status"]): "neutral" | "success" | "warning" | "danger" {
  if (status === "ONLINE") return "success";
  if (status === "DRAINING" || status === "DEGRADED") return "warning";
  return "neutral";
}

function PanelHeader({ title, description, action }: { title: string; description: string; action?: ReactNode }) {
  return <div className="panel-header"><div><h3>{title}</h3><p>{description}</p></div>{action}</div>;
}

function Step({ number, title, detail, done = false, onClick }: { number: string; title: string; detail: string; done?: boolean; onClick: () => void }) {
  return <li><button type="button" onClick={onClick}><span className={done ? "step-number step-number--done" : "step-number"}>{done ? "✓" : number}</span><span><strong>{title}</strong><small>{detail}</small></span><span className="step-arrow">→</span></button></li>;
}

function prettyBody(value: string): string {
  try {
    return JSON.stringify(JSON.parse(value) as unknown, null, 2);
  } catch {
    return value;
  }
}

function shortId(value: string): string {
  return value.length > 18 ? `${value.slice(0, 12)}…${value.slice(-4)}` : value;
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).format(new Date(value));
}

function formatBytes(value: number): string {
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

function downloadBlob(blob: Blob, name: string): void {
  const objectUrl = window.URL.createObjectURL(blob);
  const download = document.createElement("a");
  download.href = objectUrl; download.download = name; document.body.append(download);
  download.click(); download.remove(); window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 0);
}

function trendPoints(trend: RunOverview["trend"]): string {
  if (!trend.length) return "0,95 640,95";
  const maximum = Math.max(1, ...trend.map((point) => point.total));
  return trend.map((point, index) => `${trend.length === 1 ? 320 : index * 640 / (trend.length - 1)},${105 - point.total / maximum * 90}`).join(" ");
}

function runTone(state: string): "neutral" | "success" | "warning" | "danger" | "info" {
  if (state === "SUCCEEDED") return "success";
  if (state === "FAILED" || state === "CANCELLED") return "danger";
  if (state === "PARTIAL" || state === "CANCELLING") return "warning";
  if (state === "RUNNING" || state === "LEASED") return "info";
  return "neutral";
}

function Icon({ name }: { name: IconName | "search" }) {
  const paths: Record<IconName | "search", ReactNode> = {
    home: <><path d="M3 10.5 10 4l7 6.5" /><path d="M5.5 9.5V17h9V9.5" /></>,
    plug: <><path d="M7 3v5M13 3v5M5 8h10v2a5 5 0 0 1-10 0V8ZM10 15v3" /></>,
    key: <><circle cx="7" cy="10" r="3.5" /><path d="m10 10 7-7M14 6l2 2M12 8l2 2" /></>,
    message: <><path d="M3 4h14v11H8l-4 3v-3H3V4Z" /><path d="M6 8h8M6 11h5" /></>,
    people: <><circle cx="7" cy="8" r="3" /><circle cx="14.5" cy="9" r="2.5" /><path d="M2.5 17c.5-3.3 2-5 4.5-5s4 1.7 4.5 5M12 13c3.2-.7 5 .7 5.5 4" /></>,
    task: <><rect x="4" y="3" width="12" height="14" rx="2" /><path d="M7 7h6M7 10h6M7 13h4" /></>,
    pulse: <><path d="M3 14h3l2-8 4 11 2-7 1 4h2" /><circle cx="10" cy="10" r="8" /></>,
    agent: <><rect x="4" y="5" width="12" height="11" rx="3" /><path d="M10 2v3M7 10h.01M13 10h.01M8 13h4" /></>,
    code: <><path d="m7 6-4 4 4 4M13 6l4 4-4 4M11 4 9 16" /></>,
    settings: <><circle cx="10" cy="10" r="3" /><path d="M10 2v2M10 16v2M2 10h2M16 10h2M4.3 4.3l1.4 1.4M14.3 14.3l1.4 1.4M15.7 4.3l-1.4 1.4M5.7 14.3l-1.4 1.4" /></>,
    search: <><circle cx="9" cy="9" r="5.5" /><path d="m13 13 4 4" /></>,
  };
  return <svg className="icon" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}
