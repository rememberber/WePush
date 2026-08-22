import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";

import { type DebugResponse, type ProviderSummary, type SystemInfo, WePushClient } from "@wepush-next/api-client";
import { defaultsForSchema, SchemaForm, type JsonSchema } from "@wepush-next/schema-renderer";
import { Badge, Button, EmptyState, Spinner } from "@wepush-next/ui";

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
const implementedPages: readonly PageId[] = ["overview", "providers", "docs"];

export function WePushApp({ apiBaseUrl }: { apiBaseUrl?: string }) {
  const client = useMemo(() => new WePushClient(apiBaseUrl), [apiBaseUrl]);
  const [activePage, setActivePage] = useState<PageId>("overview");
  const [system, setSystem] = useState<SystemInfo>();
  const [providers, setProviders] = useState<ProviderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [connectionError, setConnectionError] = useState<string>();

  const refresh = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    try {
      const [nextSystem, nextProviders] = await Promise.all([
        client.systemInfo(signal),
        client.providers(signal),
      ]);
      setSystem(nextSystem);
      setProviders(nextProviders);
      setConnectionError(undefined);
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") return;
      setConnectionError(error instanceof Error ? error.message : "无法连接 Service");
    } finally {
      setLoading(false);
    }
  }, [client]);

  useEffect(() => {
    const controller = new AbortController();
    void refresh(controller.signal);
    return () => controller.abort();
  }, [refresh]);

  return (
    <div className="app-shell">
      <Sidebar activePage={activePage} onNavigate={setActivePage} connected={!connectionError && Boolean(system)} />
      <div className="app-workspace">
        <Topbar title={pageTitles[activePage]} connected={!connectionError && Boolean(system)} />
        <main className="app-content">
          {activePage === "overview" ? (
            <Overview
              system={system}
              providers={providers}
              loading={loading}
              error={connectionError}
              onRefresh={() => void refresh()}
              onNavigate={setActivePage}
            />
          ) : null}
          {activePage === "providers" ? <ProvidersPage client={client} providers={providers} loading={loading} /> : null}
          {activePage === "docs" ? <ApiDocsPage client={client} /> : null}
          {!implementedPages.includes(activePage) ? (
            <ComingSoon page={activePage} onNavigate={setActivePage} />
          ) : null}
        </main>
      </div>
    </div>
  );
}

function Sidebar({ activePage, onNavigate, connected }: { activePage: PageId; onNavigate: (page: PageId) => void; connected: boolean }) {
  return (
    <aside className="sidebar">
      <div className="brand-row">
        <div className="brand-mark" aria-hidden="true"><span /><span /><span /></div>
        <span>WePush</span>
        <Badge tone="neutral">Next</Badge>
      </div>
      <button className="workspace-switcher" type="button">
        <span className="workspace-avatar">L</span>
        <span><strong>Local workspace</strong><small>Standalone</small></span>
        <span className="chevrons">⌃⌄</span>
      </button>
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
                {item.id === "runs" ? <span className="nav-count">0</span> : null}
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
  loading: boolean;
  error?: string;
  onRefresh: () => void;
  onNavigate: (page: PageId) => void;
}

function Overview({ system, providers, loading, error, onRefresh, onNavigate }: OverviewProps) {
  return (
    <div className="page page--overview">
      <section className="page-heading">
        <div><p className="eyebrow">LOCAL WORKSPACE</p><h2>开始使用 WePush Next</h2><p>配置消息通道，创建发送任务，并在一个工作台中观察执行情况。</p></div>
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
        <MetricCard label="活动 Runs" value="0" detail="当前没有执行中的任务" />
        <MetricCard label="Agents" value="Local" detail="Embedded execution" tone="info" />
      </section>

      <div className="dashboard-grid">
        <section className="panel activity-panel">
          <PanelHeader title="运行情况" description="最近 24 小时" action={<button type="button" className="text-button" onClick={() => onNavigate("runs")}>查看全部 <span>→</span></button>} />
          <div className="activity-summary"><strong>0</strong><span>次发送</span><span className="delta neutral">暂无运行数据</span></div>
          <div className="chart-empty" aria-label="暂无运行趋势数据">
            <div className="chart-grid-lines"><i /><i /><i /><i /></div>
            <svg viewBox="0 0 640 120" preserveAspectRatio="none" role="img" aria-label="空趋势线">
              <path d="M0,95 C100,94 130,96 220,95 S380,95 460,95 S560,95 640,95" fill="none" stroke="currentColor" strokeWidth="2" />
            </svg>
            <div className="chart-labels"><span>00:00</span><span>06:00</span><span>12:00</span><span>18:00</span><span>现在</span></div>
          </div>
          <div className="chart-legend"><span><i className="legend-dot success" />成功 0</span><span><i className="legend-dot danger" />失败 0</span><span><i className="legend-dot unknown" />未知 0</span></div>
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
        <EmptyState icon={<Icon name="pulse" />} title="还没有运行记录" description="创建 Provider、消息与受众后，即可从任务页启动第一次 Dry Run。" action={<Button onClick={() => onNavigate("providers")}>从 Provider 开始</Button>} />
      </section>
    </div>
  );
}

function ProvidersPage({ client, providers, loading }: { client: WePushClient; providers: ProviderSummary[]; loading: boolean }) {
  const [selectedId, setSelectedId] = useState<string>();
  const selected = providers.find((provider) => provider.providerId === selectedId) ?? providers[0];
  const [schema, setSchema] = useState<JsonSchema>();
  const [formValue, setFormValue] = useState<Record<string, unknown>>({});
  const [schemaError, setSchemaError] = useState<string>();

  useEffect(() => {
    if (!selected) return;
    setSelectedId(selected.providerId);
    const controller = new AbortController();
    void client.providerSchema(selected.links.accountSchema, controller.signal)
      .then((document) => {
        const nextSchema = document as JsonSchema;
        setSchema(nextSchema);
        setFormValue(defaultsForSchema(nextSchema));
        setSchemaError(undefined);
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") return;
        setSchemaError(error instanceof Error ? error.message : "Schema 加载失败");
      });
    return () => controller.abort();
  }, [client, selected]);

  return (
    <div className="page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">EXTENSIBILITY</p><h2>Providers</h2><p>发现消息能力，并通过 Provider Schema 可视化配置连接。</p></div>
        <Button variant="primary">＋ 安装 Provider</Button>
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
              {schemaError ? <div className="inline-error">{schemaError}</div> : null}
              {!schema && !schemaError ? <div className="loading-row"><Spinner />正在加载配置 Schema…</div> : null}
              {schema ? <SchemaForm schema={schema} value={formValue} onChange={setFormValue} /> : null}
              <div className="form-actions"><Button>测试连接</Button><Button variant="primary">保存账号</Button></div>
            </>
          ) : <EmptyState icon={<Icon name="plug" />} title="选择一个 Provider" description="发现 Provider 后即可查看并渲染其实时配置 Schema。" />}
        </section>
      </div>
    </div>
  );
}

interface ApiEndpoint {
  method: "GET";
  path: string;
  title: string;
  description: string;
}

const apiEndpoints: ApiEndpoint[] = [
  { method: "GET", path: "/actuator/health", title: "Health", description: "检查 Service 是否可用" },
  { method: "GET", path: "/api/v1/system/info", title: "System info", description: "读取产品版本、模式与服务器时间" },
  { method: "GET", path: "/api/v1/providers", title: "List providers", description: "返回当前进程发现的 Provider 清单" },
];

function ApiDocsPage({ client }: { client: WePushClient }) {
  const [selected, setSelected] = useState<ApiEndpoint>(apiEndpoints[1] ?? apiEndpoints[0]!);
  const [response, setResponse] = useState<DebugResponse>();
  const [running, setRunning] = useState(false);
  const [openApi, setOpenApi] = useState<string>();

  useEffect(() => {
    const controller = new AbortController();
    void client.openApi(controller.signal).then(setOpenApi).catch(() => setOpenApi(undefined));
    return () => controller.abort();
  }, [client]);

  async function execute() {
    setRunning(true);
    try {
      setResponse(await client.debugGet(selected.path));
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
              <button type="button" key={endpoint.path} className={endpoint.path === selected.path ? "endpoint-item endpoint-item--active" : "endpoint-item"} onClick={() => { setSelected(endpoint); setResponse(undefined); }}>
                <span className="http-method">{endpoint.method}</span><span><strong>{endpoint.title}</strong><small>{endpoint.path}</small></span>
              </button>
            ))}
          </div>
        </aside>
        <section className="panel api-console">
          <div className="api-console-heading"><Badge tone="info">{selected.method}</Badge><code>{selected.path}</code><Button variant="primary" onClick={() => void execute()} disabled={running}>{running ? "发送中…" : "发送请求"}</Button></div>
          <p className="api-description">{selected.description}</p>
          <div className="request-preview"><span>Request URL</span><code>{client.baseUrl || window.location.origin}{selected.path}</code></div>
          <div className="response-heading"><h3>Response</h3>{response ? <span><Badge tone={response.status < 400 ? "success" : "danger"}>{response.status} {response.statusText}</Badge> {response.durationMs} ms</span> : null}</div>
          {response ? <pre className="response-body"><code>{prettyBody(response.body)}</code></pre> : <div className="response-placeholder"><Icon name="code" /><p>发送请求后，结构化响应会显示在这里。</p></div>}
          <div className="api-safety-note"><span>i</span><p><strong>动态调试安全规则</strong><br />Secret 示例只显示占位符；后续写操作将根据角色和风险级别显示二次确认。</p></div>
        </section>
      </div>
    </div>
  );
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
