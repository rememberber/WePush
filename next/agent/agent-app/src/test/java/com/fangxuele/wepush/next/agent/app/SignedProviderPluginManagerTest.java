package com.fangxuele.wepush.next.agent.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SignedProviderPluginManagerTest {
    @TempDir
    Path temporary;

    @Test
    void acceptsACompleteEd25519SignedManifest() throws Exception {
        KeyPair keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] content = "verified provider payload".getBytes(StandardCharsets.UTF_8);
        byte[] manifest = manifest("release", Map.of("payload/readme.txt", sha256(content)));
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keys.getPrivate());
        signer.update(manifest);
        Path archive = archive(Map.of(
                "plugin.json", manifest,
                "signature.ed25519", Base64.getEncoder().encode(signer.sign()),
                "payload/readme.txt", content));
        String trusted = "release:" + Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());

        try (SignedProviderPluginManager manager = new SignedProviderPluginManager(
                temporary, false, trusted, new ObjectMapper())) {
            assertDoesNotThrow(() -> manager.verify(archive));
        }
    }

    @Test
    void rejectsZipSlipBeforePluginLoading() throws Exception {
        Path archive = archive(Map.of(
                "plugin.json", manifest("", Map.of("../outside.txt", sha256(new byte[]{1}))),
                "../outside.txt", new byte[]{1}));
        try (SignedProviderPluginManager manager = new SignedProviderPluginManager(
                temporary, true, "", new ObjectMapper())) {
            IllegalStateException problem = assertThrows(IllegalStateException.class,
                    () -> manager.verify(archive));
            assertTrue(problem.getMessage().contains("unsafe path"));
        }
    }

    @Test
    void rejectsUntrustedProductionSigner() throws Exception {
        byte[] content = new byte[]{1, 2, 3};
        Path archive = archive(Map.of(
                "plugin.json", manifest("unknown", Map.of("payload.bin", sha256(content))),
                "signature.ed25519", "AA==".getBytes(StandardCharsets.US_ASCII),
                "payload.bin", content));
        try (SignedProviderPluginManager manager = new SignedProviderPluginManager(
                temporary, false, "", new ObjectMapper())) {
            IllegalStateException problem = assertThrows(IllegalStateException.class,
                    () -> manager.verify(archive));
            assertTrue(problem.getMessage().contains("not trusted"));
        }
    }

    @Test
    void missingActiveDirectoryIsAnEmptyCatalog() {
        Path absent = temporary.resolve("absent");
        try (SignedProviderPluginManager manager = new SignedProviderPluginManager(
                absent, false, "", new ObjectMapper())) {
            assertTrue(manager.load().isEmpty());
        }
    }

    private Path archive(Map<String, byte[]> entries) throws Exception {
        Path archive = temporary.resolve("plugin-" + System.nanoTime() + ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, byte[]> entry : new LinkedHashMap<>(entries).entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        return archive;
    }

    private static byte[] manifest(String keyId, Map<String, String> files) throws Exception {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("pluginId", "test-provider");
        manifest.put("version", "1.0.0");
        manifest.put("spiMajor", 1);
        manifest.put("signatureKeyId", keyId);
        manifest.put("files", files);
        return new ObjectMapper().writeValueAsBytes(manifest);
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
