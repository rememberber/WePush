import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";
import { z } from "zod";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { createTools } from "./tools.mjs";
import { loadConnection } from "./config.mjs";
import { install } from "./install.mjs";
import skill from "../skill/wepush/SKILL.md";
import metadata from "../package.json" with { type: "json" };

const entry = fileURLToPath(import.meta.url);
const print = (value) => process.stdout.write(JSON.stringify(value, null, 2) + "\n");

async function main() {
  const { values, positionals } = parseArgs({ allowPositionals: true, options: {
    target: { type: "string" }, url: { type: "string" }, workspace: { type: "string" }, help: { type: "boolean" },
  } });
  const command = values.help ? "help" : positionals[0] || "mcp";
  if (command === "install") {
    if (positionals.length !== 1) throw new Error("install 不接受额外位置参数");
    print(await install({ entry, skill, ...values }));
    return;
  }
  if (command === "help") {
    process.stdout.write(`WePush Next AI · Node.js 24+\n\n` +
      `  node wepush-ai.mjs install --target codex|skill|generic [--url URL] [--workspace ID]\n` +
      `  node wepush-ai.mjs mcp\n  node wepush-ai.mjs tools\n  node wepush-ai.mjs call <tool-name> < arguments.json\n\n` +
      `Environment: WEPUSH_SERVICE_URL, WEPUSH_WORKSPACE_ID, WEPUSH_API_TOKEN\n` +
      `codex installs MCP + Skill; skill installs only the Codex Skill; generic exports mcpServers JSON.\n`);
    return;
  }
  if (Object.keys(values).length) throw new Error("连接选项仅用于 install；运行时使用 WEPUSH_* 环境变量。");
  const config = await loadConnection(entry);
  const tools = createTools(config);
  if (command === "tools") {
    print([...tools.values()].map(({ name, description, schema, annotations }) =>
      ({ name, description, inputSchema: z.toJSONSchema(schema), annotations })));
  } else if (command === "call") {
    const tool = tools.get(positionals[1]);
    if (!tool || positionals.length !== 2) throw new Error("请用 tools 查看可用工具，并使用 call <tool-name>。");
    let input = "";
    for await (const chunk of process.stdin) {
      input += chunk;
      if (Buffer.byteLength(input) > 2 * 1024 * 1024) throw new Error("参数超过 2 MiB；请使用 WePush 批量导入。");
    }
    print(await tool.call(JSON.parse(input || "{}")));
  } else if (command === "mcp") {
    const server = new McpServer({ name: "wepush", version: metadata.version }, {
      instructions: "Use WePush tools in the configured Workspace. Prepare and inspect before sending; live sends need existing explicit user authorization. Never automatically resend UNKNOWN results or retry with a new idempotency key.",
    });
    for (const tool of tools.values()) {
      server.registerTool(tool.name, { description: tool.description, inputSchema: tool.schema,
        annotations: tool.annotations }, async (args) => {
        try { return { content: [{ type: "text", text: JSON.stringify(await tool.call(args)) }] }; }
        catch (error) { return { isError: true, content: [{ type: "text", text: error.message }] }; }
      });
    }
    await server.connect(new StdioServerTransport());
  } else throw new Error("未知命令；使用 --help 查看用法。");
}

main().catch((error) => { process.stderr.write(`WePush AI: ${error.message}\n`); process.exitCode = 1; });
