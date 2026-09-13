package com.fangxuele.tool.push.ai;

import com.google.gson.*;
import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

/** A private loopback bridge for CLI subprocesses, not a remotely accessible API. */
final class ClassicAiServer implements AutoCloseable {
    static final int MAX_BODY = 1_048_576;
    private final Path connection;
    private final ClassicAiService service;
    private final String token;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(4, Thread.ofPlatform().daemon().factory());

    ClassicAiServer(Path directory, ClassicAiService service) throws IOException {
        this.service = service;
        AiJson.privateDirectory(directory);
        connection = directory.resolve("connection.json");
        lockChannel = FileChannel.open(directory.resolve("bridge.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try { acquired = lockChannel.tryLock(); }
        catch (OverlappingFileLockException e) { acquired = null; }
        if (acquired == null) {
            lockChannel.close();
            executor.shutdownNow();
            throw new IOException("另一个 Classic 实例已开启 AI 接入，请先关闭该实例的接入");
        }
        lock = acquired;
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        token = HexFormat.of().formatHex(bytes);
        HttpServer started = null;
        try {
            started = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 16);
            server = started;
            server.setExecutor(executor);
            server.createContext("/tools/call", this::handle);
            server.start();
            AiJson.write(connection, AiJson.object("version", 1, "port", server.getAddress().getPort(), "token", token).toString());
        } catch (IOException | RuntimeException e) {
            if (started != null) started.stop(0);
            lock.release();
            lockChannel.close();
            executor.shutdownNow();
            throw e;
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            if (exchange.getRequestHeaders().containsKey("Origin") || authorization == null
                    || !MessageDigest.isEqual(("Bearer " + token).getBytes(StandardCharsets.UTF_8), authorization.getBytes(StandardCharsets.UTF_8))) {
                respond(exchange, 403, AiJson.object("error", "本机接入认证失败"));
                return;
            }
            if (!exchange.getRequestMethod().equals("POST") || !exchange.getRequestURI().toString().equals("/tools/call")) {
                respond(exchange, 404, AiJson.object("error", "不支持的接口"));
                return;
            }
            byte[] input = exchange.getRequestBody().readNBytes(MAX_BODY + 1);
            if (input.length > MAX_BODY) {
                respond(exchange, 413, AiJson.object("error", "请求过大"));
                return;
            }
            try {
                JsonObject request = AiJson.parse(new String(input, StandardCharsets.UTF_8)).getAsJsonObject();
                String name = request.get("name").getAsString();
                JsonObject args = request.has("arguments") ? request.getAsJsonObject("arguments") : new JsonObject();
                respond(exchange, 200, AiJson.object("result", service.call(name, args)));
            } catch (IllegalArgumentException e) {
                respond(exchange, 400, AiJson.object("error", e.getMessage()));
            } catch (Exception e) {
                respond(exchange, 500, AiJson.object("error", "Classic 无法完成请求。请检查任务配置和本机日志；发送结果不明时先查询原 runId，勿自动重发。"));
            }
        }
    }

    private static void respond(HttpExchange exchange, int status, JsonObject result) throws IOException {
        byte[] bytes = result.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override
    public void close() throws IOException {
        server.stop(0);
        executor.shutdown();
        try { Files.deleteIfExists(connection); }
        finally {
            lock.release();
            lockChannel.close();
        }
    }
}
