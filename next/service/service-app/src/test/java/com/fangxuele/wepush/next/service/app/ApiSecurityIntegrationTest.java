package com.fangxuele.wepush.next.service.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiSecurityIntegrationTest {
    private static final String ADMIN = "test-bootstrap-token-that-is-at-least-32-characters";
    private static final Path ROOT = Path.of(System.getProperty("java.io.tmpdir"),
            "wepush-next-security-" + UUID.randomUUID());

    @DynamicPropertySource
    static void configuration(DynamicPropertyRegistry registry) {
        registry.add("wepush.database.path", () -> ROOT.resolve("service.db").toString());
        registry.add("wepush.secret.master-key-path", () -> ROOT.resolve("master-key.json").toString());
        registry.add("wepush.agent.identity.ca-key-path", () -> ROOT.resolve("ca-key.pem").toString());
        registry.add("wepush.agent.identity.ca-certificate-path", () -> ROOT.resolve("ca.pem").toString());
        registry.add("wepush.agent.grpc.port", () -> "0");
        registry.add("wepush.security.enabled", () -> "true");
        registry.add("wepush.security.bootstrap-token", () -> ADMIN);
        registry.add("server.shutdown", () -> "immediate");
    }

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void enforcesWorkspaceRolesAndWritesAuditEvents() throws Exception {
        assertEquals(401, get("/api/v1/workspaces/ws_default/accounts", null).statusCode());
        assertEquals(200, get("/api/v1/workspaces/ws_default/accounts", ADMIN).statusCode());

        HttpResponse<String> issued = post("/api/v1/workspaces/ws_default/api-tokens", ADMIN,
                "{\"name\":\"readonly\",\"role\":\"VIEWER\",\"ttl\":\"P1D\"}");
        assertEquals(201, issued.statusCode(), issued.body());
        JsonNode issuedBody = json.readTree(issued.body());
        String viewer = issuedBody.get("token").textValue();
        String viewerTokenId = issuedBody.get("tokenId").textValue();
        assertEquals(200, get("/api/v1/workspaces/ws_default/accounts", viewer).statusCode());
        assertEquals(403, post("/api/v1/workspaces/ws_default/accounts", viewer,
                "{}").statusCode());

        HttpResponse<String> administratorIssued = post(
                "/api/v1/workspaces/ws_default/api-tokens", ADMIN,
                "{\"name\":\"workspace-admin\",\"role\":\"ADMIN\",\"ttl\":\"P1D\"}");
        assertEquals(201, administratorIssued.statusCode(), administratorIssued.body());
        String workspaceAdministrator = json.readTree(administratorIssued.body())
                .get("token").textValue();
        HttpResponse<String> workspaceCreated = post("/api/v1/workspaces", ADMIN,
                "{\"name\":\"Other tenant\"}");
        assertEquals(201, workspaceCreated.statusCode(), workspaceCreated.body());
        JsonNode otherWorkspace = json.readTree(workspaceCreated.body());
        String otherWorkspaceId = otherWorkspace.get("id").textValue();
        assertEquals(403, post("/api/v1/workspaces/" + otherWorkspaceId + "/api-tokens",
                workspaceAdministrator,
                "{\"name\":\"forbidden\",\"role\":\"ADMIN\",\"ttl\":\"P1D\"}")
                .statusCode());
        assertEquals(403, post("/api/v1/workspaces/" + otherWorkspaceId
                        + "/agent-enrollment-tokens", workspaceAdministrator,
                "{\"name\":\"forbidden\",\"ttl\":\"PT10M\"}").statusCode());
        assertEquals(403, get("/api/v1/workspaces", workspaceAdministrator).statusCode());
        HttpResponse<String> scopedTokens = get(
                "/api/v1/workspaces/ws_default/api-tokens", workspaceAdministrator);
        assertEquals(200, scopedTokens.statusCode(), scopedTokens.body());
        assertTrue(!scopedTokens.body().contains("token_bootstrap"));
        assertEquals(403, delete("/api/v1/workspaces/" + otherWorkspaceId
                + "/api-tokens/" + viewerTokenId, workspaceAdministrator).statusCode());
        assertEquals(204, delete("/api/v1/workspaces/ws_default/api-tokens/" + viewerTokenId,
                workspaceAdministrator).statusCode());
        assertEquals(201, post("/api/v1/workspaces/" + otherWorkspaceId + "/api-tokens", ADMIN,
                "{\"name\":\"tenant-admin\",\"role\":\"ADMIN\",\"ttl\":\"P1D\"}")
                .statusCode());

        HttpResponse<String> audit = get(
                "/api/v1/workspaces/ws_default/audit-events?limit=100", ADMIN);
        assertEquals(200, audit.statusCode(), audit.body());
        JsonNode events = json.readTree(audit.body());
        assertTrue(events.isArray());
        assertTrue(events.toString().contains("DENIED"));
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String token, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).DELETE();
        if (token != null) request.header("Authorization", "Bearer " + token);
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
