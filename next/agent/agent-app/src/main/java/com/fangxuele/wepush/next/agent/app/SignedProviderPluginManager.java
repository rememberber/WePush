package com.fangxuele.wepush.next.agent.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.agent.plugin.ProviderFactoryExtension;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import org.pf4j.DefaultPluginManager;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Verifies active PF4J packages before any plugin class is loaded. */
final class SignedProviderPluginManager implements AutoCloseable {
    private static final long MAXIMUM_ARCHIVE_BYTES = 512L * 1024L * 1024L;
    private static final long MAXIMUM_ENTRY_BYTES = 256L * 1024L * 1024L;
    private static final Set<String> FORBIDDEN_PREFIXES = Set.of(
            "com/fangxuele/wepush/next/core/", "com/fangxuele/wepush/next/provider/spi/",
            "org/slf4j/", "org/pf4j/");

    private final Path activeDirectory;
    private final boolean developerMode;
    private final Map<String, PublicKey> trustedKeys;
    private final ObjectMapper json;
    private DefaultPluginManager manager;

    SignedProviderPluginManager(Path activeDirectory, boolean developerMode,
                                String trustedKeyConfiguration, ObjectMapper json) {
        this.activeDirectory = activeDirectory.toAbsolutePath().normalize();
        this.developerMode = developerMode;
        this.trustedKeys = keys(trustedKeyConfiguration);
        this.json = json;
    }

    List<ProviderFactory> load() {
        try {
            if (!Files.exists(activeDirectory)) return List.of();
            List<Path> packages;
            try (var paths = Files.list(activeDirectory)) {
                packages = paths.filter(path -> path.getFileName().toString().endsWith(".zip"))
                        .sorted().toList();
            }
            for (Path pluginPackage : packages) verify(pluginPackage);
            manager = new DefaultPluginManager(activeDirectory);
            manager.loadPlugins();
            manager.startPlugins();
            List<ProviderFactory> factories = manager.getExtensions(ProviderFactoryExtension.class)
                    .stream().map(ProviderFactoryExtension::factory).toList();
            if (factories.stream().map(factory -> factory.descriptor().providerId()
                            + "@" + factory.descriptor().implementationVersion()).distinct().count()
                    != factories.size()) {
                throw new IllegalStateException("Provider plugins expose duplicate Provider versions");
            }
            return factories;
        } catch (RuntimeException problem) {
            close();
            throw problem;
        } catch (Exception problem) {
            close();
            throw new IllegalStateException("Provider plugin catalog cannot be loaded", problem);
        }
    }

    void verify(Path archive) throws Exception {
        if (!Files.isRegularFile(archive) || Files.size(archive) > MAXIMUM_ARCHIVE_BYTES) {
            throw new IllegalStateException("Provider plugin package is not a bounded regular file: " + archive);
        }
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            byte[] manifestBytes = read(zip, "plugin.json", 1024 * 1024);
            PluginManifest manifest = json.readValue(manifestBytes, PluginManifest.class);
            if (manifest.pluginId() == null || !manifest.pluginId().matches("[A-Za-z0-9._-]{1,120}")
                    || manifest.version() == null || !manifest.version().matches("[A-Za-z0-9._+-]{1,80}")
                    || manifest.spiMajor() != 1 || manifest.files() == null || manifest.files().isEmpty()) {
                throw new IllegalStateException("Provider plugin manifest is invalid");
            }
            long expanded = 0;
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                safeEntry(entry);
                if (entry.getSize() > MAXIMUM_ENTRY_BYTES) {
                    throw new IllegalStateException("Provider plugin entry is too large: " + entry.getName());
                }
                if (entry.getSize() > 0) expanded = Math.addExact(expanded, entry.getSize());
                if (expanded > MAXIMUM_ARCHIVE_BYTES) {
                    throw new IllegalStateException("Provider plugin expanded size exceeds the limit");
                }
            }
            for (Map.Entry<String, String> file : manifest.files().entrySet()) {
                if (!file.getValue().matches("[0-9a-f]{64}")) {
                    throw new IllegalStateException("Provider plugin file digest is invalid");
                }
                byte[] bytes = read(zip, file.getKey(), MAXIMUM_ENTRY_BYTES);
                String actual = HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(bytes));
                if (!MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII),
                        file.getValue().getBytes(StandardCharsets.US_ASCII))) {
                    throw new IllegalStateException("Provider plugin file digest differs: " + file.getKey());
                }
                if (file.getKey().endsWith(".jar")) verifyJar(bytes, file.getKey());
            }
            if (!developerMode) verifySignature(zip, manifestBytes, manifest.signatureKeyId());
        }
    }

    private void verifySignature(ZipFile zip, byte[] manifest, String keyId) throws Exception {
        PublicKey key = trustedKeys.get(keyId);
        if (key == null) throw new IllegalStateException("Provider plugin signer is not trusted: " + keyId);
        byte[] encoded = read(zip, "signature.ed25519", 4096);
        byte[] signatureBytes = Base64.getDecoder().decode(
                new String(encoded, StandardCharsets.US_ASCII).trim());
        Signature signature = Signature.getInstance("Ed25519");
        signature.initVerify(key);
        signature.update(manifest);
        if (!signature.verify(signatureBytes)) {
            throw new IllegalStateException("Provider plugin signature is invalid");
        }
    }

    private static void verifyJar(byte[] jarBytes, String name) throws Exception {
        Path temporary = Files.createTempFile("wepush-plugin-jar-", ".jar");
        try {
            Files.write(temporary, jarBytes);
            try (ZipFile jar = new ZipFile(temporary.toFile())) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    if (FORBIDDEN_PREFIXES.stream().anyMatch(entry::startsWith)) {
                        throw new IllegalStateException("Provider plugin bundles a shared API in " + name);
                    }
                }
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static byte[] read(ZipFile zip, String name, long maximum) throws Exception {
        safeName(name);
        ZipEntry entry = zip.getEntry(name);
        if (entry == null || entry.isDirectory() || entry.getSize() > maximum) {
            throw new IllegalStateException("Provider plugin entry is missing or too large: " + name);
        }
        try (InputStream input = zip.getInputStream(entry)) {
            byte[] bytes = input.readNBytes(Math.toIntExact(maximum + 1));
            if (bytes.length > maximum) throw new IllegalStateException("Provider plugin entry is too large");
            return bytes;
        }
    }

    private static void safeEntry(ZipEntry entry) {
        safeName(entry.getName());
        if (entry.isDirectory() && entry.getSize() > 0) {
            throw new IllegalStateException("Provider plugin directory entry has content");
        }
    }

    private static void safeName(String name) {
        if (name == null || name.isBlank() || name.startsWith("/") || name.startsWith("\\")
                || name.contains("../") || name.contains("..\\") || name.contains(":")
                || name.indexOf('\0') >= 0) {
            throw new IllegalStateException("Provider plugin contains an unsafe path");
        }
    }

    private static Map<String, PublicKey> keys(String configuration) {
        Map<String, PublicKey> result = new LinkedHashMap<>();
        if (configuration == null || configuration.isBlank()) return result;
        try {
            for (String item : configuration.split(",")) {
                int separator = item.indexOf(':');
                if (separator < 1) throw new IllegalArgumentException("key separator");
                String keyId = item.substring(0, separator).trim();
                byte[] encoded = Base64.getDecoder().decode(item.substring(separator + 1).trim());
                result.put(keyId, KeyFactory.getInstance("Ed25519")
                        .generatePublic(new X509EncodedKeySpec(encoded)));
            }
            return Map.copyOf(result);
        } catch (Exception problem) {
            throw new IllegalArgumentException("WEPUSH_PLUGIN_TRUSTED_KEYS is invalid", problem);
        }
    }

    @Override
    public void close() {
        if (manager != null) {
            manager.stopPlugins();
            manager.unloadPlugins();
            manager = null;
        }
    }

    private record PluginManifest(String pluginId, String version, int spiMajor,
                                  String signatureKeyId, Map<String, String> files) {
    }
}
