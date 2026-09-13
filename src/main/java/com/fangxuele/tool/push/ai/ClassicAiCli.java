package com.fangxuele.tool.push.ai;

import com.google.gson.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Headless entry point. It must not initialize App, Swing, MyBatis or provider SDKs. */
public final class ClassicAiCli {
    private final Path connection;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .proxy(new ProxySelector() {
                @Override public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); }
                @Override public void connectFailed(URI uri, SocketAddress address, IOException e) { }
            }).followRedirects(HttpClient.Redirect.NEVER).build();

    ClassicAiCli(Path connection) { this.connection = connection; }

    public static void main(String[] args) throws Exception {
        PrintStream output = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        System.setOut(System.err); // Third-party logging must never corrupt the stdio protocol.
        if (args.length < 3 || !args[0].equals("--connection")) {
            System.err.println("Usage: ClassicAiCli --connection <connection.json> mcp|tools|call <tool> (JSON arguments on stdin)");
            System.exit(2);
        }
        ClassicAiCli cli = new ClassicAiCli(Path.of(args[1]));
        switch (args[2]) {
            case "mcp" -> cli.serve(new InputStreamReader(System.in, StandardCharsets.UTF_8), output);
            case "tools" -> output.println(ClassicAiTools.list());
            case "call" -> {
                try {
                    if (args.length != 4) throw new IllegalArgumentException("call 需要一个工具名称");
                    byte[] bytes = System.in.readNBytes(ClassicAiServer.MAX_BODY + 1);
                    if (bytes.length > ClassicAiServer.MAX_BODY) throw new IllegalArgumentException("参数过大");
                    String input = new String(bytes, StandardCharsets.UTF_8);
                    JsonObject params = input.isBlank() ? new JsonObject() : JsonParser.parseString(input).getAsJsonObject();
                    output.println(cli.call(args[3], params));
                } catch (Exception e) {
                    output.println(AiJson.object("error", e.getMessage()));
                    System.exit(1);
                }
            }
            default -> throw new IllegalArgumentException("未知命令");
        }
    }

    JsonElement call(String name, JsonObject args) throws IOException, InterruptedException {
        ClassicAiTools.validate(name, args);
        if (!Files.isRegularFile(connection)) throw new IOException("请先启动 WePush Classic，并在「应用 → AI 助手接入」开启接入");
        JsonObject config = JsonParser.parseString(Files.readString(connection)).getAsJsonObject();
        int port = config.get("port").getAsBigDecimal().intValueExact();
        String token = config.get("token").getAsString();
        if (port < 1 || port > 65535 || !token.matches("[a-f0-9]{64}")) throw new IOException("本机连接信息无效，请在 Classic 中重新开启接入");
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/tools/call"))
                .timeout(Duration.ofMinutes(2)).header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(AiJson.object("name", name, "arguments", args).toString())).build();
        HttpResponse<String> response;
        try { response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); }
        catch (IOException e) { throw new IOException("Classic 本机桥接不可用。请检查应用是否运行；发送超时后应使用原请求编号查询或重试。", e); }
        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
        if (response.statusCode() != 200) throw new IOException(body.has("error") ? body.get("error").getAsString() : "Classic 请求失败");
        return body.get("result");
    }

    void serve(Reader input, PrintStream output) throws IOException {
        BufferedReader reader = new BufferedReader(input);
        String line;
        while ((line = readLine(reader)) != null) {
            if (line.isBlank()) continue;
            JsonObject request;
            try {
                JsonElement parsed = AiJson.parse(line);
                if (!parsed.isJsonObject()) {
                    output.println(error(JsonNull.INSTANCE, -32600, "Invalid Request"));
                    continue;
                }
                request = parsed.getAsJsonObject();
            } catch (JsonParseException e) {
                output.println(error(JsonNull.INSTANCE, -32700, "Parse error"));
                continue;
            }
            JsonElement id = request.get("id");
            if (!request.has("jsonrpc") || !request.get("jsonrpc").equals(new JsonPrimitive("2.0"))
                    || !request.has("method") || !request.get("method").isJsonPrimitive()
                    || !request.getAsJsonPrimitive("method").isString()
                    || (id != null && (!id.isJsonPrimitive() || id.getAsJsonPrimitive().isBoolean()))) {
                output.println(error(JsonNull.INSTANCE, -32600, "Invalid Request"));
                continue;
            }
            if (id == null) continue; // initialized/cancelled notifications have no response.
            try {
                JsonObject params = request.has("params") ? request.getAsJsonObject("params") : new JsonObject();
                JsonElement result = switch (request.get("method").getAsString()) {
                    case "initialize" -> {
                        String requested = params.get("protocolVersion").getAsString();
                        String version = Set.of("2024-11-05", "2025-03-26", "2025-06-18", "2025-11-25").contains(requested) ? requested : "2025-11-25";
                        yield AiJson.object("protocolVersion", version, "capabilities", AiJson.object("tools", new JsonObject()),
                                "serverInfo", AiJson.object("name", "wepush-classic", "version", "1.0.0"),
                                "instructions", "操作本机已开启 AI 接入的 Classic。正式发送先 prepare_run，按现有用户授权执行；UNKNOWN 结果勿自动重发。");
                    }
                    case "ping" -> new JsonObject();
                    case "tools/list" -> AiJson.object("tools", ClassicAiTools.list());
                    case "tools/call" -> {
                        String name = params.get("name").getAsString();
                        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : new JsonObject();
                        ClassicAiTools.validate(name, arguments);
                        try { yield toolResult(call(name, arguments), false); }
                        catch (Exception e) { yield toolResult(new JsonPrimitive(e.getMessage()), true); }
                    }
                    default -> null;
                };
                output.println(result == null ? error(id, -32601, "Method not found") : AiJson.object("jsonrpc", "2.0", "id", id, "result", result));
            } catch (RuntimeException e) {
                output.println(error(id, -32602, "Invalid params"));
            }
            output.flush();
        }
    }

    private static String readLine(BufferedReader reader) throws IOException {
        StringBuilder line = new StringBuilder();
        int character;
        while ((character = reader.read()) != -1 && character != '\n') {
            if (line.length() >= ClassicAiServer.MAX_BODY) throw new IOException("MCP request too large");
            line.append((char) character);
        }
        return character == -1 && line.isEmpty() ? null : line.toString();
    }

    private static JsonObject toolResult(JsonElement value, boolean isError) {
        return AiJson.object("content", List.of(AiJson.object("type", "text", "text", value.isJsonPrimitive() ? value.getAsString() : value.toString())), "isError", isError);
    }

    private static JsonObject error(JsonElement id, int code, String message) {
        return AiJson.object("jsonrpc", "2.0", "id", id, "error", AiJson.object("code", code, "message", message));
    }
}
