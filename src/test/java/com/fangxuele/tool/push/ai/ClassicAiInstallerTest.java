package com.fangxuele.tool.push.ai;

import com.google.gson.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.tomlj.Toml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ClassicAiInstallerTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void preservesNextAndOtherTomlTablesAndCanReinstall() throws Exception {
        Path home = temporary.newFolder("中文 space ' $ `").toPath();
        Path codex = home.resolve("custom-codex");
        Files.createDirectories(codex);
        String original = """
                model = "test-model"
                instructions = """ + "\"\"\"\nkeep multiline\n\"\"\"\n" + """
                [mcp_servers.wepush]
                command = 'next-runtime'
                args = ['mcp']
                [profiles.work]
                approval_policy = 'on-request'
                """;
        Path config = codex.resolve("config.toml");
        Files.writeString(config, original);
        ClassicAiInstaller installer = installer(home, codex);
        installer.install(true);
        String installed = Files.readString(config);
        assertTrue(installed.startsWith(original));
        assertEquals("next-runtime", Toml.parse(installed).getString("mcp_servers.wepush.command"));
        assertTrue(Toml.parse(installed).contains("mcp_servers.wepush-classic"));
        installer.install(true);
        assertEquals(installed, Files.readString(config));
        try (var files = Files.list(codex)) {
            assertEquals(1, files.filter(path -> path.getFileName().toString().endsWith(".bak")).count());
        }
        assertFalse(Files.readString(installer.skillPath().resolve("mcp.json")).contains("token"));
        byte[] ps = Files.readAllBytes(installer.skillPath().resolve("scripts/wepush-classic.ps1"));
        assertArrayEquals(new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf}, Arrays.copyOf(ps, 3));
    }

    @Test public void rejectsManualOrBrokenConfigWithoutInstallingSkill() throws Exception {
        for (String invalid : List.of("[mcp_servers.wepush-classic]\ncommand = 'custom'\n", "broken = [",
                "# BEGIN WEPUSH CLASSIC MANAGED MCP\n", "mcp_servers = {wepush-classic = {command = 'custom'}}\n")) {
            Path home = temporary.newFolder().toPath();
            Path codex = home.resolve(".codex");
            Files.createDirectories(codex);
            Files.writeString(codex.resolve("config.toml"), invalid);
            ClassicAiInstaller installer = installer(home, codex);
            assertThrows(IOException.class, () -> installer.install(true));
            assertEquals(invalid, Files.readString(codex.resolve("config.toml")));
            assertFalse(Files.exists(installer.skillPath()));
        }
    }

    @Test public void skillOnlyPreservesInvalidCodexConfigAndForeignSkillIsNotOverwritten() throws Exception {
        Path home = temporary.newFolder().toPath();
        Path codex = home.resolve(".codex");
        Files.createDirectories(codex);
        Files.writeString(codex.resolve("config.toml"), "invalid = [");
        ClassicAiInstaller installer = installer(home, codex);
        installer.install(false);
        assertEquals("invalid = [", Files.readString(codex.resolve("config.toml")));
        Files.delete(installer.skillPath().resolve(".wepush-classic-managed"));
        Files.writeString(installer.skillPath().resolve("SKILL.md"), "user skill");
        assertThrows(IOException.class, () -> installer.install(false));
        assertEquals("user skill", Files.readString(installer.skillPath().resolve("SKILL.md")));
    }

    @Test public void managedBlockCannotSwallowOtherConfiguration() throws Exception {
        ClassicAiInstaller installer = installer(temporary.newFolder().toPath(), temporary.newFolder().toPath());
        String config = installer.mergeConfig("model='test'\n");
        String foreign = config.replace("# END WEPUSH CLASSIC MANAGED MCP", "[profiles.personal]\nmodel='keep'\n# END WEPUSH CLASSIC MANAGED MCP");
        assertThrows(IOException.class, () -> installer.mergeConfig(foreign));
    }

    @Test public void generatedLauncherExecutesRealCliWithQuotedPaths() throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        Path home = temporary.newFolder("launcher 中文 ' $ `").toPath();
        ClassicAiInstaller installer = installer(home, home.resolve(".codex"));
        installer.install(false);
        List<String> command = windows
                ? List.of("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", installer.skillPath().resolve("scripts/wepush-classic.ps1").toString(), "tools")
                : List.of("sh", installer.skillPath().resolve("scripts/wepush-classic.sh").toString(), "tools");
        Process process = new ProcessBuilder(command)
                .redirectError(home.resolve("stderr.log").toFile()).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(20, TimeUnit.SECONDS));
        assertEquals(Files.readString(home.resolve("stderr.log")), 0, process.exitValue());
        var tools = JsonParser.parseString(output).getAsJsonArray();
        assertEquals(8, tools.size());
        assertTrue(tools.get(0).getAsJsonObject().get("description").getAsString().contains("查询"));
    }

    private ClassicAiInstaller installer(Path home, Path codex) {
        String java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        return new ClassicAiInstaller(home, codex, java, classpath, home.resolve("connection.json"));
    }
}
