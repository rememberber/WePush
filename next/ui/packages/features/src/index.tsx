import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";

import {
  type Account,
  type Audience,
  type DebugResponse,
  type Job,
  type Message,
  type ProviderSummary,
  type Run,
  type RunItemResult,
  type SystemInfo,
  WePushClient,
} from "@wepush-next/api-client";
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
const implementedPages: readonly PageId[] = [
  "overview", "providers", "accounts", "messages", "audiences", "jobs", "runs", "docs",
];

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
          {activePage === "accounts" ? <AccountsPage client={client} onNavigate={setActivePage} /> : null}
          {activePage === "messages" ? <MessagesPage client={client} providers={providers} /> : null}
          {activePage === "audiences" ? <AudiencesPage client={client} /> : null}
          {activePage === "jobs" ? <JobsPage client={client} onNavigate={setActivePage} /> : null}
          {activePage === "runs" ? <RunsPage client={client} onNavigate={setActivePage} /> : null}
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
  const [accountName, setAccountName] = useState("Local HTTP");
  const [saving, setSaving] = useState(false);
  const [savedAccount, setSavedAccount] = useState<Account>();

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
      });
      setSavedAccount(account);
    } catch (error) {
      setSchemaError(error instanceof Error ? error.message : "账号保存失败");
    } finally {
      setSaving(false);
    }
  }

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
              <label className="simple-field"><span>账号名称</span><input value={accountName} onChange={(event) => setAccountName(event.target.value)} /></label>
              {schemaError ? <div className="inline-error">{schemaError}</div> : null}
              {savedAccount ? <div className="inline-success">账号 {savedAccount.name} 已保存，可以继续创建消息和受众。</div> : null}
              {!schema && !schemaError ? <div className="loading-row"><Spinner />正在加载配置 Schema…</div> : null}
              {schema ? <SchemaForm schema={schema} value={formValue} onChange={setFormValue} /> : null}
              <div className="form-actions"><Button>测试连接</Button><Button variant="primary" onClick={() => void saveAccount()} disabled={saving || !schema}>{saving ? "保存中…" : "保存账号"}</Button></div>
            </>
          ) : <EmptyState icon={<Icon name="plug" />} title="选择一个 Provider" description="发现 Provider 后即可查看并渲染其实时配置 Schema。" />}
        </section>
      </div>
    </div>
  );
}

function AccountsPage({ client, onNavigate }: { client: WePushClient; onNavigate: (page: PageId) => void }) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setAccounts(await client.accounts());
      setError(undefined);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "账号加载失败");
    } finally {
      setLoading(false);
    }
  }, [client]);

  useEffect(() => { void load(); }, [load]);

  return (
    <div className="page resource-page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">CHANNEL CONFIGURATION</p><h2>账号</h2><p>Provider 连接配置归属当前工作区，敏感值仅保存 Secret 引用。</p></div>
        <Button variant="primary" onClick={() => onNavigate("providers")}>＋ 新建账号</Button>
      </section>
      <section className="panel resource-table-panel">
        <div className="list-toolbar"><strong>全部账号</strong><Badge>{accounts.length}</Badge><Button variant="ghost" onClick={() => void load()}>刷新</Button></div>
        {loading ? <div className="loading-row"><Spinner />正在读取账号…</div> : null}
        {error ? <div className="inline-error">{error}</div> : null}
        {!loading && !error && accounts.length === 0 ? <EmptyState icon={<Icon name="key" />} title="还没有账号" description="从 Provider 页面打开动态 Schema 表单，保存第一个渠道账号。" action={<Button onClick={() => onNavigate("providers")}>配置 Provider</Button>} /> : null}
        {accounts.length ? <div className="resource-table">
          <div className="resource-row resource-row--header"><span>名称</span><span>Provider</span><span>状态</span><span>更新时间</span></div>
          {accounts.map((account) => <div className="resource-row" key={account.id}>
            <span><strong>{account.name}</strong><small>{account.id}</small></span>
            <span>{account.providerId}<small>v{account.providerVersion}</small></span>
            <span><Badge tone="success">{account.status}</Badge></span>
            <span>{formatTime(account.updatedAt)}</span>
          </div>)}
        </div> : null}
      </section>
    </div>
  );
}

function MessagesPage({ client, providers }: { client: WePushClient; providers: ProviderSummary[] }) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [providerId, setProviderId] = useState<string>();
  const [schema, setSchema] = useState<JsonSchema>();
  const [content, setContent] = useState<Record<string, unknown>>({});
  const [name, setName] = useState("Welcome message");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string>();
  const selected = providers.find((provider) => provider.providerId === providerId) ?? providers[0];

  const load = useCallback(async () => {
    try {
      setMessages(await client.messages());
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "消息加载失败");
    }
  }, [client]);

  useEffect(() => { void load(); }, [load]);
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
      await client.createMessage({
        name,
        providerId: selected.providerId,
        providerVersion: selected.implementationVersion,
        content,
      });
      await load();
      setName(`Message ${messages.length + 2}`);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "消息保存失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page resource-page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">IMMUTABLE REVISION</p><h2>消息</h2><p>根据 Provider Message Schema 可视化编排内容，每次保存形成可追溯修订。</p></div>
        <Badge tone="info">Schema driven</Badge>
      </section>
      <div className="composer-layout">
        <section className="panel composer-panel">
          <PanelHeader title="新建消息" description="选择 Provider 后动态渲染内容字段" />
          <label className="simple-field"><span>名称</span><input value={name} onChange={(event) => setName(event.target.value)} /></label>
          <label className="simple-field"><span>Provider</span><select value={selected?.providerId ?? ""} onChange={(event) => setProviderId(event.target.value)}>
            {providers.map((provider) => <option value={provider.providerId} key={provider.providerId}>{provider.displayName} · {provider.implementationVersion}</option>)}
          </select></label>
          {error ? <div className="inline-error">{error}</div> : null}
          {!schema && selected ? <div className="loading-row"><Spinner />正在读取 Message Schema…</div> : null}
          {schema ? <SchemaForm schema={schema} value={content} onChange={setContent} /> : null}
          <div className="form-actions"><Button variant="primary" disabled={!schema || saving || !name.trim()} onClick={() => void createMessage()}>{saving ? "保存中…" : "保存消息"}</Button></div>
        </section>
        <section className="panel resource-list-panel">
          <div className="list-toolbar"><strong>消息修订</strong><Badge>{messages.length}</Badge><Button variant="ghost" onClick={() => void load()}>刷新</Button></div>
          {messages.length === 0 ? <EmptyState icon={<Icon name="message" />} title="还没有消息" description="左侧表单来自 Provider Schema，保存后可直接组合到任务。" /> : null}
          <div className="resource-card-list">{messages.map((message) => <article key={message.id}>
            <span className="resource-card-icon"><Icon name="message" /></span>
            <div><strong>{message.name}</strong><small>{message.providerId} · revision {message.revision}</small><code>{shortId(message.contentHash)}</code></div>
            <Badge tone="success">{message.status}</Badge>
          </article>)}</div>
        </section>
      </div>
    </div>
  );
}

function AudiencesPage({ client }: { client: WePushClient }) {
  const [audiences, setAudiences] = useState<Audience[]>([]);
  const [name, setName] = useState("Sample audience");
  const [source, setSource] = useState(`[
  { "itemId": "alice", "fields": { "mobile": "13000000000", "name": "Alice" } },
  { "itemId": "bob", "fields": { "mobile": "13100000000", "name": "Bob" } }
]`);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string>();

  const load = useCallback(async () => {
    try {
      setAudiences(await client.audiences());
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "受众加载失败");
    }
  }, [client]);
  useEffect(() => { void load(); }, [load]);

  async function createAudience() {
    setSaving(true);
    setError(undefined);
    try {
      const parsed = JSON.parse(source) as unknown;
      if (!Array.isArray(parsed) || parsed.some((item) => !item || typeof item !== "object" || !("fields" in item))) {
        throw new Error("受众必须是包含 fields 的 JSON 数组");
      }
      await client.createAudience({
        name,
        recipients: parsed as { itemId?: string; fields: Record<string, unknown> }[],
      });
      await load();
      setName(`Audience ${audiences.length + 2}`);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "受众保存失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page resource-page">
      <section className="page-heading page-heading--compact">
        <div><p className="eyebrow">SNAPSHOT INPUT</p><h2>受众</h2><p>导入结构化 Recipient，Service 会生成不可变快照和内容哈希。</p></div>
        <Badge tone="neutral">JSON · CSV next</Badge>
      </section>
      <div className="composer-layout">
        <section className="panel composer-panel">
          <PanelHeader title="导入受众" description="itemId 可省略，字段会按 Provider Recipient Schema 校验" />
          <label className="simple-field"><span>名称</span><input value={name} onChange={(event) => setName(event.target.value)} /></label>
          <label className="simple-field"><span>Recipients JSON</span><textarea rows={14} value={source} onChange={(event) => setSource(event.target.value)} spellCheck={false} /></label>
          {error ? <div className="inline-error">{error}</div> : null}
          <div className="form-actions"><Button variant="primary" disabled={saving || !name.trim()} onClick={() => void createAudience()}>{saving ? "生成快照中…" : "创建受众快照"}</Button></div>
        </section>
        <section className="panel resource-list-panel">
          <div className="list-toolbar"><strong>受众快照</strong><Badge>{audiences.length}</Badge><Button variant="ghost" onClick={() => void load()}>刷新</Button></div>
          {audiences.length === 0 ? <EmptyState icon={<Icon name="people" />} title="还没有受众" description="粘贴 JSON Recipient 列表即可形成第一份不可变快照。" /> : null}
          <div className="resource-card-list">{audiences.map((audience) => <article key={audience.id}>
            <span className="resource-card-icon"><Icon name="people" /></span>
            <div><strong>{audience.name}</strong><small>{audience.recordCount} recipients · revision {audience.revision}</small><code>{shortId(audience.contentHash)}</code></div>
            <Badge tone="success">{audience.status}</Badge>
          </article>)}</div>
        </section>
      </div>
    </div>
  );
}

function JobsPage({ client, onNavigate }: { client: WePushClient; onNavigate: (page: PageId) => void }) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [audiences, setAudiences] = useState<Audience[]>([]);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [name, setName] = useState("Welcome job");
  const [accountId, setAccountId] = useState("");
  const [messageId, setMessageId] = useState("");
  const [audienceId, setAudienceId] = useState("");
  const [concurrency, setConcurrency] = useState(8);
  const [saving, setSaving] = useState(false);
  const [runningJob, setRunningJob] = useState<string>();
  const [error, setError] = useState<string>();

  const load = useCallback(async () => {
    try {
      const [nextAccounts, nextMessages, nextAudiences, nextJobs] = await Promise.all([
        client.accounts(), client.messages(), client.audiences(), client.jobs(),
      ]);
      setAccounts(nextAccounts); setMessages(nextMessages); setAudiences(nextAudiences); setJobs(nextJobs);
      setAccountId((current) => current || nextAccounts[0]?.id || "");
      setMessageId((current) => current || nextMessages[0]?.id || "");
      setAudienceId((current) => current || nextAudiences[0]?.id || "");
      setError(undefined);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "任务资源加载失败");
    }
  }, [client]);
  useEffect(() => { void load(); }, [load]);

  async function createJob() {
    setSaving(true); setError(undefined);
    try {
      await client.createJob({
        name, accountId, messageId, audienceId,
        policies: { concurrency: { minimum: 1, target: concurrency, maximum: Math.max(16, concurrency) } },
        enabled: true,
      });
      await load();
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
      await client.createRun(job.id, { dryRun: true, reason: "web-ui" }, crypto.randomUUID());
      onNavigate("runs");
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "Dry Run 创建失败");
    } finally {
      setRunningJob(undefined);
    }
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
          <PanelHeader title="新建任务" description="Run 创建时会冻结所有资源修订" />
          <label className="simple-field"><span>名称</span><input value={name} onChange={(event) => setName(event.target.value)} /></label>
          <label className="simple-field"><span>账号</span><select value={accountId} onChange={(event) => setAccountId(event.target.value)}><option value="">请选择</option>{accounts.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
          <label className="simple-field"><span>消息</span><select value={messageId} onChange={(event) => setMessageId(event.target.value)}><option value="">请选择</option>{messages.map((item) => <option key={item.id} value={item.id}>{item.name} · r{item.revision}</option>)}</select></label>
          <label className="simple-field"><span>受众</span><select value={audienceId} onChange={(event) => setAudienceId(event.target.value)}><option value="">请选择</option>{audiences.map((item) => <option key={item.id} value={item.id}>{item.name} · {item.recordCount} items</option>)}</select></label>
          <label className="simple-field"><span>目标并发</span><input type="number" min={1} max={256} value={concurrency} onChange={(event) => setConcurrency(Number(event.target.value))} /></label>
          <div className="form-actions"><Button variant="primary" disabled={!ready || saving} onClick={() => void createJob()}>{saving ? "保存中…" : "保存任务"}</Button></div>
        </section>
        <section className="panel resource-list-panel">
          <div className="list-toolbar"><strong>任务定义</strong><Badge>{jobs.length}</Badge><Button variant="ghost" onClick={() => void load()}>刷新</Button></div>
          {jobs.length === 0 ? <EmptyState icon={<Icon name="task" />} title="还没有任务" description="先创建账号、消息和受众，再在左侧组合执行定义。" /> : null}
          <div className="job-card-list">{jobs.map((job) => <article key={job.id}>
            <div className="job-card-heading"><div><strong>{job.name}</strong><small>{shortId(job.id)}</small></div><Badge tone={job.enabled ? "success" : "neutral"}>{job.enabled ? "ENABLED" : "DISABLED"}</Badge></div>
            <div className="job-links"><span>Account <code>{shortId(job.accountId)}</code></span><span>Message <code>{shortId(job.messageId)}</code></span><span>Audience <code>{shortId(job.audienceId)}</code></span></div>
            <div className="job-actions"><Button variant="primary" disabled={!job.enabled || runningJob === job.id} onClick={() => void dryRun(job)}>{runningJob === job.id ? "启动中…" : "Dry Run"}</Button></div>
          </article>)}</div>
        </section>
      </div>
    </div>
  );
}

interface VisibleRunEvent {
  id: string;
  type: string;
  data: string;
}

function RunsPage({ client, onNavigate }: { client: WePushClient; onNavigate: (page: PageId) => void }) {
  const [runs, setRuns] = useState<Run[]>([]);
  const [selectedId, setSelectedId] = useState<string>();
  const [events, setEvents] = useState<VisibleRunEvent[]>([]);
  const [results, setResults] = useState<RunItemResult[]>([]);
  const [nextCursor, setNextCursor] = useState<string>();
  const [resultsLoading, setResultsLoading] = useState(false);
  const [commandBusy, setCommandBusy] = useState<string>();
  const [commandError, setCommandError] = useState<string>();
  const [concurrency, setConcurrency] = useState(8);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const selected = runs.find((run) => run.id === selectedId) ?? runs[0];

  const refreshRuns = useCallback(async () => {
    try {
      const nextRuns = await client.runs();
      setRuns(nextRuns);
      setSelectedId((current) => current ?? nextRuns[0]?.id);
      setError(undefined);
    } catch (nextError) {
      setError(nextError instanceof Error ? nextError.message : "运行记录加载失败");
    } finally {
      setLoading(false);
    }
  }, [client]);

  useEffect(() => {
    void refreshRuns();
    const timer = window.setInterval(() => void refreshRuns(), 1_500);
    return () => window.clearInterval(timer);
  }, [refreshRuns]);

  const refreshResults = useCallback(async () => {
    if (!selectedId) { setResults([]); setNextCursor(undefined); return; }
    setResultsLoading(true);
    try {
      const page = await client.runItems(selectedId, undefined, 100);
      setResults(page.items);
      setNextCursor(page.page.nextCursor);
    } catch (nextError) {
      setCommandError(nextError instanceof Error ? nextError.message : "Item Result 加载失败");
    } finally {
      setResultsLoading(false);
    }
  }, [client, selectedId]);

  useEffect(() => {
    void refreshResults();
    const timer = window.setInterval(() => void refreshResults(), 1_500);
    return () => window.clearInterval(timer);
  }, [refreshResults]);

  useEffect(() => {
    if (!selected?.id) return;
    setEvents([]);
    const source = new EventSource(client.runEventsUrl(selected.id));
    const names = ["RUN_CREATED", "RUN_STARTED", "ITEM_COMPLETED", "PROGRESS", "RUN_COMPLETED", "RUN_FAILED", "RUN_FINALIZED", "RUN_FAILED_TO_START", "STATE_CHANGED", "CONCURRENCY_CHANGED", "RUN_COMMAND_ACCEPTED", "RUN_COMMAND_REJECTED"];
    const receive = (raw: Event) => {
      const event = raw as MessageEvent<string>;
      setEvents((current) => [...current, { id: event.lastEventId, type: event.type, data: event.data }].slice(-200));
      void refreshRuns();
    };
    names.forEach((name) => source.addEventListener(name, receive));
    // EventSource reconnects automatically and carries Last-Event-ID for persisted replay.
    source.onerror = () => undefined;
    return () => source.close();
  }, [client, refreshRuns, selected?.id]);

  async function loadMoreResults() {
    if (!selected?.id || !nextCursor) return;
    setResultsLoading(true);
    try {
      const page = await client.runItems(selected.id, nextCursor, 100);
      setResults((current) => [...current, ...page.items]);
      setNextCursor(page.page.nextCursor);
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
      if (type === "pause") await client.pauseRun(selected.id, key);
      if (type === "resume") await client.resumeRun(selected.id, key);
      if (type === "cancel") await client.cancelRun(selected.id, "web-ui", key);
      if (type === "concurrency") await client.changeRunConcurrency(selected.id, concurrency, key);
      await refreshRuns();
    } catch (nextError) {
      setCommandError(nextError instanceof Error ? nextError.message : "运行命令失败");
    } finally {
      setCommandBusy(undefined);
    }
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
          <div className="list-toolbar"><strong>最近运行</strong><Badge>{runs.length}</Badge></div>
          {loading ? <div className="loading-row"><Spinner />正在加载运行…</div> : null}
          {!loading && runs.length === 0 ? <EmptyState icon={<Icon name="pulse" />} title="还没有运行" description="创建 Job 后先发起一次 Dry Run 验证完整链路。" action={<Button onClick={() => onNavigate("jobs")}>创建任务</Button>} /> : null}
          {runs.map((run) => <button type="button" key={run.id} className={selected?.id === run.id ? "run-list-item run-list-item--active" : "run-list-item"} onClick={() => setSelectedId(run.id)}>
            <span className={`run-state-dot run-state-dot--${run.state.toLowerCase()}`} />
            <span><strong>{run.dryRun ? "Dry Run" : "Run"} · {shortId(run.id)}</strong><small>{formatTime(run.createdAt)} · {run.counters.total} items</small></span>
            <Badge tone={runTone(run.state)}>{run.state}</Badge>
          </button>)}
        </section>
        <section className="panel run-detail-panel">
          {selected ? <>
            <div className="run-detail-heading"><div><p className="eyebrow">{selected.dryRun ? "DRY RUN" : "LIVE RUN"}</p><h3>{selected.id}</h3><p>Job {selected.jobId}</p></div><Badge tone={runTone(selected.state)}>{selected.state}</Badge></div>
            <div className="run-command-bar">
              {selected.state === "RUNNING" ? <Button disabled={Boolean(commandBusy)} onClick={() => void issueCommand("pause")}>暂停</Button> : null}
              {selected.state === "PAUSED" ? <Button disabled={Boolean(commandBusy)} onClick={() => void issueCommand("resume")}>恢复</Button> : null}
              {["RUNNING", "PAUSED"].includes(selected.state) ? <><label><span>并发</span><input type="number" min={1} max={256} value={concurrency} onChange={(event) => setConcurrency(Number(event.target.value))} /></label><Button disabled={Boolean(commandBusy)} onClick={() => void issueCommand("concurrency")}>应用</Button></> : null}
              {["PENDING", "RUNNING", "PAUSED", "RECOVERING"].includes(selected.state) ? <Button variant="ghost" disabled={Boolean(commandBusy)} onClick={() => void issueCommand("cancel")}>取消运行</Button> : null}
              {commandBusy ? <span><Spinner />正在提交 {commandBusy}…</span> : null}
            </div>
            {commandError ? <div className="inline-error">{commandError}</div> : null}
            <div className="run-counter-grid">
              {Object.entries(selected.counters).map(([name, value]) => <div key={name}><span>{name}</span><strong>{value}</strong></div>)}
            </div>
            <div className="event-heading result-heading"><div><h3>Item Results</h3><p>持久化明细 · HMAC 游标分页 · 同 Item 幂等写入</p></div><Badge tone="neutral">{results.length} loaded</Badge></div>
            <div className="result-table">
              <div className="result-row result-row--header"><span>Item</span><span>State</span><span>Attempts</span><span>Provider</span><span>Completed</span></div>
              {results.map((result) => <div className="result-row" key={result.itemId}>
                <span><strong>{result.itemId}</strong><small>{result.externalRequestId || "no external id"}</small></span>
                <span><Badge tone={runTone(result.state)}>{result.state}</Badge></span>
                <span>{result.attempts}</span><span>{result.providerCode || "—"}</span><span>{formatTime(result.completedAt)}</span>
              </div>)}
              {!resultsLoading && results.length === 0 ? <div className="result-empty">运行产生结果后会在这里逐项显示。</div> : null}
            </div>
            {nextCursor ? <div className="load-more-row"><Button variant="ghost" disabled={resultsLoading} onClick={() => void loadMoreResults()}>{resultsLoading ? "加载中…" : "加载更多"}</Button></div> : null}
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
  { method: "GET", path: "/api/v1/workspaces/ws_default/accounts", title: "List accounts", description: "读取当前工作区的 Provider 账号" },
  { method: "GET", path: "/api/v1/workspaces/ws_default/messages", title: "List messages", description: "读取消息模板及当前不可变修订" },
  { method: "GET", path: "/api/v1/workspaces/ws_default/audiences", title: "List audiences", description: "读取受众及当前快照元数据" },
  { method: "GET", path: "/api/v1/workspaces/ws_default/jobs", title: "List jobs", description: "读取任务组合与执行策略" },
  { method: "GET", path: "/api/v1/workspaces/ws_default/runs", title: "List runs", description: "读取执行状态和结果计数" },
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
