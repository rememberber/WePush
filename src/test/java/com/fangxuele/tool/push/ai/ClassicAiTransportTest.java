package com.fangxuele.tool.push.ai;

import com.google.gson.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

import static org.junit.Assert.*;

public class ClassicAiTransportTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void stdioDiscoveryWorksOfflineAndMalformedRequestsDoNotBreakFraming() throws Exception {
        ClassicAiCli cli = new ClassicAiCli(temporary.getRoot().toPath().resolve("absent.json"));
        String input = """
                {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}
                {"jsonrpc":"2.0","method":"notifications/initialized"}
                {"jsonrpc":"2.0","id":2,"method":"tools/list"}
                broken
                []
                {"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"wepush_classic_system_info","arguments":{}}}
                {"jsonrpc":"2.0","id":4,"method":"unknown"}
                {"jsonrpc":"2.0","id":5,"method":"ping"}
                """;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        cli.serve(new StringReader(input), new PrintStream(output, true, StandardCharsets.UTF_8));
        List<JsonObject> results = output.toString(StandardCharsets.UTF_8).lines().map(line -> JsonParser.parseString(line).getAsJsonObject()).toList();
        assertEquals(7, results.size());
        assertEquals("2025-11-25", results.get(0).getAsJsonObject("result").get("protocolVersion").getAsString());
        assertEquals(8, results.get(1).getAsJsonObject("result").getAsJsonArray("tools").size());
        assertEquals(-32700, results.get(2).getAsJsonObject("error").get("code").getAsInt());
        assertEquals(-32600, results.get(3).getAsJsonObject("error").get("code").getAsInt());
        assertTrue(results.get(4).getAsJsonObject("result").get("isError").getAsBoolean());
        assertEquals(-32601, results.get(5).getAsJsonObject("error").get("code").getAsInt());
        assertEquals(new JsonObject(), results.get(6).get("result"));
    }

    @Test public void loopbackAuthRejectsBrowserRequestsAndClientReconnectsAfterTokenRotation() throws Exception {
        Path directory = temporary.newFolder().toPath();
        ClassicAiService service = new ClassicAiService(new ClassicAiServiceTest.FakeGateway(), directory);
        ClassicAiCli cli = new ClassicAiCli(directory.resolve("connection.json"));
        String firstToken;
        try (ClassicAiServer server = new ClassicAiServer(directory, service)) {
            JsonObject connection = JsonParser.parseString(Files.readString(directory.resolve("connection.json"))).getAsJsonObject();
            firstToken = connection.get("token").getAsString();
            assertThrows(IOException.class, () -> new ClassicAiServer(directory, service));
            URI uri = URI.create("http://127.0.0.1:" + connection.get("port").getAsInt() + "/tools/call");
            try (HttpClient client = HttpClient.newHttpClient()) {
                for (boolean origin : List.of(false, true)) {
                    HttpRequest.Builder request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.ofString("{}"));
                    if (origin) request.header("Origin", "https://example.invalid").header("Authorization", "Bearer " + firstToken);
                    assertEquals(403, client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode());
                }
            }
            assertEquals("WePush Classic", cli.call("wepush_classic_system_info", new JsonObject()).getAsJsonObject().get("product").getAsString());
            if (Files.getFileStore(directory).supportsFileAttributeView("posix")) {
                assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(directory.resolve("connection.json")));
            }
        }
        assertFalse(Files.exists(directory.resolve("connection.json")));
        try (ClassicAiServer server = new ClassicAiServer(directory, service)) {
            String connection = Files.readString(directory.resolve("connection.json"));
            assertFalse(connection.contains(firstToken));
            assertNotNull(cli.call("wepush_classic_list_tasks", new JsonObject()));
        }
    }
}
