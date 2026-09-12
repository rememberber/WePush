import { useState } from "react";
import { Button } from "@wepush-next/ui";

export interface AiIntegrationBridge {
  install(target: "codex" | "skill" | "generic", url: string, workspace: string): Promise<{
    ok: boolean; message: string; output: string;
  }>;
}

export function AiIntegrationPanel({ serviceUrl, workspaceId, desktop }: {
  serviceUrl: string; workspaceId: string; desktop?: AiIntegrationBridge;
}) {
  const [url, setUrl] = useState(serviceUrl || window.location.origin);
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<{ ok: boolean; message: string; output: string }>();
  const [copied, setCopied] = useState(false);
  const [shell, setShell] = useState<"posix" | "powershell">("posix");
  const quote = (value: string) => shell === "powershell" ? `'${value.replaceAll("'", "''")}'` : `'${value.replaceAll("'", "'\\''")}'`;
  const command = `node ./wepush-ai.mjs install --target codex --url ${quote(url)} --workspace ${quote(workspaceId)}`;

  async function install(target: "codex" | "skill" | "generic") {
    if (!desktop) return;
    setBusy(true); setResult(undefined);
    try { setResult(await desktop.install(target, url, workspaceId)); }
    catch (error) { setResult({ ok: false, message: error instanceof Error ? error.message : "安装失败", output: "" }); }
    finally { setBusy(false); }
  }

  async function copy() {
    try { await navigator.clipboard.writeText(command); setCopied(true); }
    catch { setResult({ ok: false, message: "无法访问剪贴板，请手动复制下方命令。", output: "" }); }
  }

  return <section className="panel composer-panel compact-form ai-integration-panel" aria-labelledby="ai-integration-title">
    <div><h3 id="ai-integration-title">AI 助手接入</h3><p>让 Codex 等助手通过 MCP 或 Skill 使用 WePush：准备消息、测试发送、查询投递结果。</p></div>
    <label className="simple-field"><span>AI 助手连接的 Service URL</span><input type="url" value={url}
      onChange={(event) => { setUrl(event.target.value); setCopied(false); setResult(undefined); }} /></label>
    <p>工作区：<code>{workspaceId}</code>。地址须能从 AI 客户端所在机器访问；本机开发默认使用 http://127.0.0.1:18990。</p>
    {desktop ? <div className="form-actions">
      <Button variant="primary" disabled={busy || !url.trim()} onClick={() => void install("codex")}>{busy ? "处理中…" : "一键接入 Codex"}</Button>
      <Button disabled={busy || !url.trim()} onClick={() => void install("skill")}>仅安装 Skill</Button>
      <Button disabled={busy || !url.trim()} onClick={() => void install("generic")}>导出通用 MCP 配置</Button>
    </div> : <>
      <p>浏览器版：下载安装器，在下载目录执行下方命令即可安装 MCP 和 Skill。需要 Node.js 24+；桌面版可直接一键安装。</p>
      <div className="form-actions"><a className="wp-button wp-button--primary" href="/ai/wepush-ai.mjs" download="wepush-ai.mjs">下载安装器</a>
        <label className="simple-field"><span>终端</span><select value={shell} onChange={(event) => { setShell(event.target.value as "posix" | "powershell"); setCopied(false); }}><option value="posix">macOS / Linux</option><option value="powershell">Windows PowerShell</option></select></label>
        <Button onClick={() => void copy()}>{copied ? "已复制" : "复制安装命令"}</Button></div>
      <pre className="ai-integration-output">{command}</pre>
      <p>其他 MCP 客户端：将命令中的 <code>--target codex</code> 改为 <code>--target generic</code>，然后导入生成的 mcp.json。</p>
    </>}
    <p>Codex 安装包含 MCP 配置和 <code>~/.agents/skills/wepush</code>；已有配置会先备份。启用认证时，请在 AI 客户端环境中设置 <code>WEPUSH_API_TOKEN</code>。安装器不会复制当前登录 Token。</p>
    <p>安装后可说：“用 WePush 列出任务并执行 Dry Run”。正式发送沿用预览确认流程。</p>
    {result ? <div role="status" className={result.ok ? "ai-integration-result" : "inline-error"}><p>{result.message}</p>{result.output ? <pre className="ai-integration-output">{result.output}</pre> : null}</div> : null}
  </section>;
}
