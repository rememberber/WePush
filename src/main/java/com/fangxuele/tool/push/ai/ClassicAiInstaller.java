package com.fangxuele.tool.push.ai;

import org.tomlj.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class ClassicAiInstaller {
    private static final String BEGIN = "# BEGIN WEPUSH CLASSIC MANAGED MCP";
    private static final String END = "# END WEPUSH CLASSIC MANAGED MCP";
    private static final String MARKER = ".wepush-classic-managed";
    private static final String MAIN_CLASS = "com.fangxuele.tool.push.ai.ClassicAiCli";
    private final Path skill;
    private final Path config;
    private final String java;
    private final List<String> arguments;

    public ClassicAiInstaller() {
        this(Path.of(System.getProperty("user.home")), codexHome(),
                Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString(),
                runtimeClasspath(), Path.of(System.getProperty("user.home"), ".WePush5", "ai", "connection.json"));
    }

    ClassicAiInstaller(Path home, Path codexHome, String java, String classpath, Path connection) {
        this.skill = home.resolve(".agents/skills/wepush-classic");
        this.config = codexHome.resolve("config.toml");
        this.java = java;
        this.arguments = List.of("-Djava.awt.headless=true", "-cp", classpath, MAIN_CLASS, "--connection", connection.toString());
    }

    public Path skillPath() { return skill; }

    public String genericConfig() {
        List<String> args = new ArrayList<>(arguments);
        args.add("mcp");
        return new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(
                AiJson.object("mcpServers", AiJson.object("wepush-classic", AiJson.object("command", java, "args", args))));
    }

    /** Preflight all conflicts, then write managed files. Restore previous bytes if any write fails. */
    public synchronized Path install(boolean includeMcp) throws IOException {
        if (Files.exists(skill) && (!Files.isRegularFile(skill.resolve(MARKER))
                || !Files.readString(skill.resolve(MARKER)).equals("wepush-classic:1\n"))) {
            throw new IOException("Skill 目录已存在且并非 Classic 安装器创建，请先自行备份并移走：" + skill);
        }
        String original = includeMcp && Files.exists(config) ? Files.readString(config) : null;
        String updated = includeMcp ? mergeConfig(original == null ? "" : original) : null;
        LinkedHashMap<Path, String> writes = new LinkedHashMap<>();
        try (InputStream input = ClassicAiInstaller.class.getResourceAsStream("/ai/wepush-classic/SKILL.md")) {
            if (input == null) throw new IOException("安装包缺少 Classic Skill 资源");
            writes.put(skill.resolve("SKILL.md"), new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        writes.put(skill.resolve(MARKER), "wepush-classic:1\n");
        List<String> command = new ArrayList<>();
        command.add(java);
        command.addAll(arguments);
        String shell = String.join(" ", command.stream().map(ClassicAiInstaller::shellQuote).toList());
        String powershell = String.join(" ", command.stream().map(ClassicAiInstaller::powershellQuote).toList());
        writes.put(skill.resolve("scripts/wepush-classic.sh"), "#!/bin/sh\nexec " + shell + " \"$@\"\n");
        writes.put(skill.resolve("scripts/wepush-classic.ps1"), "\uFEFF$ErrorActionPreference = 'Stop'\n[Console]::InputEncoding = [Text.UTF8Encoding]::new($false)\n[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)\n$OutputEncoding = [Text.UTF8Encoding]::new($false)\n& " + powershell + " @args\nexit $LASTEXITCODE\n");
        writes.put(skill.resolve("mcp.json"), genericConfig() + "\n");
        if (includeMcp) writes.put(config, updated);
        LinkedHashMap<Path, byte[]> before = new LinkedHashMap<>();
        for (Path path : writes.keySet()) {
            if (Files.isSymbolicLink(path)) throw new IOException("安装目标不能是符号链接：" + path);
            before.put(path, Files.exists(path) ? Files.readAllBytes(path) : null);
        }
        if (includeMcp && !Arrays.equals(before.get(config), original == null ? null : original.getBytes(StandardCharsets.UTF_8))) {
            throw new IOException("准备安装期间 config.toml 已变化，请重试");
        }
        if (includeMcp && original != null && !Objects.equals(original, updated)) {
            AiJson.write(config.resolveSibling("config.toml.wepush-classic-" + UUID.randomUUID() + ".bak"), original);
        }
        List<Path> written = new ArrayList<>();
        try {
            for (Map.Entry<Path, String> entry : writes.entrySet()) {
                Path path = entry.getKey();
                // Avoid replacing changes another editor made while installation was preparing files.
                byte[] now = Files.exists(path) ? Files.readAllBytes(path) : null;
                if (!Arrays.equals(before.get(path), now)) throw new IOException("安装期间文件已变化，请重试：" + path);
                AiJson.write(path, entry.getValue());
                written.add(path);
            }
            AiJson.permissions(skill.resolve("scripts/wepush-classic.sh"), "rwx------");
        } catch (IOException | RuntimeException e) {
            Collections.reverse(written);
            for (Path path : written) {
                try {
                    byte[] bytes = before.get(path);
                    if (bytes == null) Files.deleteIfExists(path);
                    else AiJson.write(path, new String(bytes, StandardCharsets.UTF_8));
                } catch (IOException rollback) { e.addSuppressed(rollback); }
            }
            throw e;
        }
        return skill;
    }

    String mergeConfig(String original) throws IOException {
        TomlParseResult before = parse(original);
        String replacement = BEGIN + "\n[mcp_servers.wepush-classic]\ncommand = " + tomlString(java)
                + "\nargs = [" + String.join(", ", Stream.concat(arguments.stream(), Stream.of("mcp"))
                .map(ClassicAiInstaller::tomlString).toList()) + "]\n" + END + "\n";
        Pattern block = Pattern.compile("(?m)^" + Pattern.quote(BEGIN) + "\\r?\\n[\\s\\S]*?^" + Pattern.quote(END) + "(?:\\r?\\n|$)");
        var matcher = block.matcher(original);
        String updated;
        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (matcher.find() || original.indexOf(BEGIN) != start || original.lastIndexOf(END) >= end) {
                throw new IOException("Classic MCP 托管标记重复，请检查 config.toml");
            }
            updated = original.substring(0, start) + replacement + original.substring(end);
        } else {
            if (original.contains(BEGIN) || original.contains(END) || before.contains(List.of("mcp_servers", "wepush-classic"))) {
                throw new IOException("发现手动配置或不完整的 wepush-classic MCP，安装器不会覆盖，请先自行整理 config.toml");
            }
            updated = original + (original.endsWith("\n") || original.isEmpty() ? "" : "\n") + "\n" + replacement;
        }
        TomlParseResult after = parse(updated);
        if (!unmanaged(before).equals(unmanaged(after))) {
            throw new IOException("更新会影响其他 TOML 配置，已取消安装；请检查 Classic 托管区域");
        }
        return updated;
    }

    private static TomlParseResult parse(String source) throws IOException {
        TomlParseResult parsed = Toml.parse(source);
        if (parsed.hasErrors()) throw new IOException("config.toml 语法无效，安装器未修改原文件");
        return parsed;
    }

    private static Map<List<String>, Object> unmanaged(TomlTable table) {
        Map<List<String>, Object> result = new HashMap<>();
        table.entryPathSet().forEach(entry -> {
            List<String> path = entry.getKey();
            if (path.size() >= 2 && path.get(0).equals("mcp_servers") && path.get(1).equals("wepush-classic")) return;
            result.put(path, normalize(entry.getValue()));
        });
        return result;
    }

    private static Object normalize(Object value) {
        if (value instanceof TomlTable table) return unmanaged(table);
        if (value instanceof TomlArray array) {
            List<Object> result = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) result.add(normalize(array.get(i)));
            return result;
        }
        return value;
    }

    private static String tomlString(String value) {
        // JSON escapes overlap TOML except JSON's optional \/; Gson does not emit that escape.
        return AiJson.GSON.toJson(value);
    }

    private static String shellQuote(String value) { return "'" + value.replace("'", "'\"'\"'") + "'"; }
    private static String powershellQuote(String value) { return "'" + value.replace("'", "''") + "'"; }
    private static boolean isWindows() { return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"); }
    private static Path codexHome() {
        String configured = System.getenv("CODEX_HOME");
        return configured == null || configured.isBlank() ? Path.of(System.getProperty("user.home"), ".codex") : Path.of(configured);
    }

    private static String runtimeClasspath() {
        return String.join(File.pathSeparator, Arrays.stream(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
                .map(value -> Path.of(value).toAbsolutePath().normalize().toString()).toList());
    }
}
