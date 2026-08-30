package com.fangxuele.wepush.next.service.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Manual operations that intentionally avoid secrets, recipient data, and provider payloads. */
final class SystemOperationsService {
    private static final List<String> SAFE_CONFIGURATION_KEYS = List.of(
            "wepush.mode", "wepush.execution.mode", "wepush.database.kind",
            "wepush.artifact.kind", "wepush.artifact.environment", "server.address", "server.port");

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Environment environment;
    private final URI releasesUri;
    private final HttpClient http;

    SystemOperationsService(JdbcTemplate jdbc, ObjectMapper json, Environment environment,
                            URI releasesUri) {
        this.jdbc = jdbc;
        this.json = json;
        this.environment = environment;
        this.releasesUri = releasesUri;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    byte[] diagnosticBundle(String productVersion, String mode) {
        Instant generatedAt = Instant.now();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("format", "wepush-redacted-diagnostics-v1");
        summary.put("generatedAt", generatedAt.toString());
        summary.put("product", "WePush Next");
        summary.put("version", productVersion);
        summary.put("mode", mode);
        summary.put("redaction", List.of("tokens", "secrets", "recipient fields", "provider payloads",
                "database credentials", "filesystem paths"));
        summary.put("databaseReachable", databaseReachable());
        summary.put("flywayVersion", scalar("SELECT MAX(version) FROM flyway_schema_history WHERE success = 1"));
        summary.put("counts", counts());
        summary.put("runStates", grouped("SELECT status, COUNT(*) FROM run_instance GROUP BY status"));
        summary.put("artifactStates", grouped("SELECT state, COUNT(*) FROM artifact_record GROUP BY state"));

        Map<String, String> configuration = new TreeMap<>();
        SAFE_CONFIGURATION_KEYS.forEach(key -> configuration.put(key,
                environment.getProperty(key, "<default>")));
        Map<String, Object> runtime = Map.of(
                "javaVersion", System.getProperty("java.version", "unknown"),
                "javaVendor", System.getProperty("java.vendor", "unknown"),
                "osName", System.getProperty("os.name", "unknown"),
                "osArch", System.getProperty("os.arch", "unknown"),
                "availableProcessors", Runtime.getRuntime().availableProcessors(),
                "maxMemoryBytes", Runtime.getRuntime().maxMemory());
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
                entry(zip, "summary.json", json.writerWithDefaultPrettyPrinter().writeValueAsBytes(summary));
                entry(zip, "safe-configuration.json",
                        json.writerWithDefaultPrettyPrinter().writeValueAsBytes(configuration));
                entry(zip, "runtime.json", json.writerWithDefaultPrettyPrinter().writeValueAsBytes(runtime));
                entry(zip, "README.txt", ("This bundle is deliberately redacted. It contains aggregate state "
                        + "only; no Token, Secret, recipient record, provider request/response, database URL, "
                        + "credential, or local path is collected.\n").getBytes(StandardCharsets.UTF_8));
            }
            return bytes.toByteArray();
        } catch (IOException problem) {
            throw new IllegalStateException("Diagnostic bundle could not be generated", problem);
        }
    }

    VersionCheck versionCheck(String currentVersion) {
        Instant checkedAt = Instant.now();
        try {
            HttpRequest request = HttpRequest.newBuilder(releasesUri).timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "WePush-Next-manual-version-check")
                    .GET().build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                return new VersionCheck(false, currentVersion, "", false, "", checkedAt,
                        "Release service returned HTTP " + response.statusCode());
            }
            JsonNode release = selectNextStableRelease(json.readTree(response.body()));
            if (release.isMissingNode()) {
                return new VersionCheck(false, currentVersion, "", false, "", checkedAt,
                        "No stable WePush Next release was returned");
            }
            String tag = release.path("tag_name").asText("");
            String latest = normalize(tag);
            String url = release.path("html_url").asText("");
            return new VersionCheck(true, currentVersion, latest,
                    isNewer(latest, normalize(currentVersion)), url, checkedAt, "");
        } catch (InterruptedException problem) {
            Thread.currentThread().interrupt();
            return new VersionCheck(false, currentVersion, "", false, "", checkedAt,
                    "Version check was interrupted");
        } catch (IOException | RuntimeException problem) {
            return new VersionCheck(false, currentVersion, "", false, "", checkedAt,
                    "Release information is temporarily unavailable");
        }
    }

    private Map<String, Long> counts() {
        Map<String, Long> values = new LinkedHashMap<>();
        values.put("workspaces", count("workspace"));
        values.put("accounts", count("account_definition"));
        values.put("messages", count("message_definition"));
        values.put("audiences", count("audience_definition"));
        values.put("jobs", count("job_definition"));
        values.put("runs", count("run_instance"));
        values.put("agents", count("agent_registration"));
        values.put("artifacts", count("artifact_record"));
        return values;
    }

    private long count(String table) {
        Long value = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return value == null ? 0 : value;
    }

    private Map<String, Long> grouped(String sql) {
        Map<String, Long> values = new TreeMap<>();
        jdbc.query(sql, (rs, ignored) -> Map.entry(rs.getString(1), rs.getLong(2)))
                .forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        return values;
    }

    private Object scalar(String sql) {
        return jdbc.query(sql, rs -> rs.next() ? rs.getString(1) : null);
    }

    private boolean databaseReachable() {
        try {
            return Integer.valueOf(1).equals(jdbc.queryForObject("SELECT 1", Integer.class));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static void entry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0);
        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }

    private static String normalize(String version) {
        if (version == null) return "";
        String normalized = version.strip();
        if (normalized.startsWith("next-v")) normalized = normalized.substring(6);
        if (normalized.startsWith("v")) normalized = normalized.substring(1);
        int suffix = normalized.indexOf('-');
        return suffix < 0 ? normalized : normalized.substring(0, suffix);
    }

    static JsonNode selectNextStableRelease(JsonNode response) {
        if (response != null && response.isObject()) {
            return isNextStableRelease(response) ? response : response.path("__missing_release__");
        }
        JsonNode selected = response == null ? null : response.path("__missing_release__");
        if (response != null && response.isArray()) {
            for (JsonNode release : response) {
                if (!isNextStableRelease(release)) continue;
                if (selected == null || selected.isMissingNode()
                        || isNewer(normalize(release.path("tag_name").asText()),
                        normalize(selected.path("tag_name").asText()))) {
                    selected = release;
                }
            }
        }
        return selected == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance() : selected;
    }

    private static boolean isNextStableRelease(JsonNode release) {
        return release.isObject()
                && release.path("tag_name").asText("").startsWith("next-v")
                && !release.path("draft").asBoolean(false)
                && !release.path("prerelease").asBoolean(false);
    }

    private static boolean isNewer(String candidate, String current) {
        int[] left = parts(candidate);
        int[] right = parts(current);
        for (int index = 0; index < 3; index++) {
            if (left[index] != right[index]) return left[index] > right[index];
        }
        return false;
    }

    private static int[] parts(String version) {
        String[] values = version.split("\\.");
        int[] parsed = new int[3];
        for (int index = 0; index < Math.min(values.length, parsed.length); index++) {
            try { parsed[index] = Integer.parseInt(values[index]); }
            catch (NumberFormatException ignored) { return new int[3]; }
        }
        return parsed;
    }

    record VersionCheck(boolean successful, String currentVersion, String latestVersion,
                        boolean updateAvailable, String releaseUrl, Instant checkedAt, String diagnostic) { }
}
