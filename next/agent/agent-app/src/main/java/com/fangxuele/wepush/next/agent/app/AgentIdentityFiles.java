package com.fangxuele.wepush.next.agent.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class AgentIdentityFiles {
    private static final Set<PosixFilePermission> OWNER_ONLY = Set.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    private static final Duration ROTATION_WINDOW = Duration.ofDays(14);

    private final ObjectMapper json;
    private final HttpClient http;
    private final Clock clock;
    private final SecureRandom random;

    AgentIdentityFiles(ObjectMapper json, HttpClient http, Clock clock, SecureRandom random) {
        this.json = json;
        this.http = http;
        this.clock = clock;
        this.random = random;
    }

    IdentityMaterial resolve(Path statePath, String serviceBaseUrl, String requestedAgentId,
                             String enrollmentToken, String legacyCredential) {
        Path absoluteState = statePath.toAbsolutePath().normalize();
        if (Files.isRegularFile(absoluteState)) {
            IdentityState state = read(absoluteState);
            verifyMaterial(state, absoluteState);
            if (expiring(state)) state = rotate(absoluteState, serviceBaseUrl, state);
            return material(absoluteState, state);
        }
        if (enrollmentToken == null || enrollmentToken.isBlank()) {
            return new IdentityMaterial(requestedAgentId, legacyCredential,
                    GrpcAgentClient.TlsConfiguration.systemTrust(), false);
        }
        return enroll(absoluteState, serviceBaseUrl, requestedAgentId, enrollmentToken);
    }

    private IdentityMaterial enroll(Path statePath, String serviceBaseUrl, String requestedAgentId,
                                    String enrollmentToken) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), random);
            KeyPair pair = generator.generateKeyPair();
            String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
            EnrollmentResponse response = request(serviceBaseUrl + "/internal/agent/v1/enroll",
                    "Enrollment " + enrollmentToken,
                    new EnrollmentRequest(requestedAgentId, publicKey));
            Path privateKey = sibling(statePath, "agent-client-key.pem");
            Path certificate = sibling(statePath, "agent-client-certificate.pem");
            Path ca = sibling(statePath, "agent-ca-certificate.pem");
            writeSecure(privateKey, privateKeyPem(pair.getPrivate().getEncoded()));
            writeSecure(certificate, response.certificatePem());
            writeSecure(ca, response.caCertificatePem());
            IdentityState state = new IdentityState(response.agentId(), response.credential(),
                    response.credentialExpiresAt(), response.certificateExpiresAt(), publicKey,
                    privateKey.getFileName().toString(), certificate.getFileName().toString(),
                    ca.getFileName().toString());
            writeJson(statePath, state);
            return material(statePath, state);
        } catch (RuntimeException problem) {
            throw problem;
        } catch (Exception problem) {
            throw new IllegalStateException("Agent Enrollment failed", problem);
        }
    }

    private IdentityState rotate(Path statePath, String serviceBaseUrl, IdentityState current) {
        EnrollmentResponse response = request(serviceBaseUrl
                        + "/internal/agent/v1/credentials/rotate",
                "Agent " + current.credential(), new RotationRequest(current.publicKeyBase64()));
        Path certificate = resolveSibling(statePath, current.certificateFile());
        Path ca = resolveSibling(statePath, current.caCertificateFile());
        writeSecure(certificate, response.certificatePem());
        writeSecure(ca, response.caCertificatePem());
        IdentityState replacement = new IdentityState(response.agentId(), response.credential(),
                response.credentialExpiresAt(), response.certificateExpiresAt(),
                current.publicKeyBase64(), current.privateKeyFile(), current.certificateFile(),
                current.caCertificateFile());
        writeJson(statePath, replacement);
        return replacement;
    }

    private EnrollmentResponse request(String endpoint, String authorization, Object body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)))
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Agent identity endpoint returned HTTP "
                        + response.statusCode());
            }
            EnrollmentResponse decoded = json.readValue(response.body(), EnrollmentResponse.class);
            if (decoded.agentId() == null || decoded.agentId().isBlank()
                    || decoded.credential() == null || decoded.credential().isBlank()
                    || decoded.certificatePem() == null || decoded.certificatePem().isBlank()
                    || decoded.caCertificatePem() == null || decoded.caCertificatePem().isBlank()) {
                throw new IllegalStateException("Agent identity response is incomplete");
            }
            return decoded;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Agent identity request was interrupted", interrupted);
        } catch (Exception problem) {
            if (problem instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Agent identity request failed", problem);
        }
    }

    private IdentityState read(Path path) {
        try {
            verifyOwnerOnly(path);
            return json.readValue(Files.readAllBytes(path), IdentityState.class);
        } catch (Exception problem) {
            throw new IllegalStateException("Agent identity file cannot be loaded", problem);
        }
    }

    private boolean expiring(IdentityState state) {
        Instant threshold = clock.instant().plus(ROTATION_WINDOW);
        return state.credentialExpiresAt() == null || state.certificateExpiresAt() == null
                || !state.credentialExpiresAt().isAfter(threshold)
                || !state.certificateExpiresAt().isAfter(threshold);
    }

    private static void verifyMaterial(IdentityState state, Path statePath) {
        if (state.agentId() == null || state.agentId().isBlank()
                || state.credential() == null || state.credential().isBlank()
                || state.publicKeyBase64() == null || state.publicKeyBase64().isBlank()) {
            throw new IllegalStateException("Agent identity file is incomplete");
        }
        requireFile(resolveSibling(statePath, state.privateKeyFile()), "Agent private key");
        requireFile(resolveSibling(statePath, state.certificateFile()), "Agent certificate");
        requireFile(resolveSibling(statePath, state.caCertificateFile()), "Agent CA certificate");
    }

    private static IdentityMaterial material(Path statePath, IdentityState state) {
        return new IdentityMaterial(state.agentId(), state.credential(),
                new GrpcAgentClient.TlsConfiguration(
                        resolveSibling(statePath, state.caCertificateFile()),
                        resolveSibling(statePath, state.certificateFile()),
                        resolveSibling(statePath, state.privateKeyFile())), true);
    }

    private void writeJson(Path path, IdentityState state) {
        try {
            writeSecure(path, new String(json.writeValueAsBytes(state), StandardCharsets.UTF_8));
        } catch (Exception problem) {
            throw new IllegalStateException("Agent identity file cannot be persisted", problem);
        }
    }

    private static void writeSecure(Path path, String content) {
        Path parent = path.getParent();
        if (parent == null) throw new IllegalArgumentException("Agent identity path has no parent");
        try {
            Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, ".wepush-agent-identity-", ".tmp");
            try {
                secureOwnerOnly(temporary);
                Files.writeString(temporary, content, StandardCharsets.US_ASCII);
                try {
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                }
                secureOwnerOnly(path);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (Exception problem) {
            throw new IllegalStateException("Agent identity material cannot be persisted", problem);
        }
    }

    private static void secureOwnerOnly(Path path) throws Exception {
        PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posix != null) {
            Files.setPosixFilePermissions(path, OWNER_ONLY);
            return;
        }
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl != null) {
            AclEntry owner = AclEntry.newBuilder().setType(AclEntryType.ALLOW)
                    .setPrincipal(Files.getOwner(path))
                    .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build();
            acl.setAcl(List.of(owner));
        }
    }

    private static void verifyOwnerOnly(Path path) throws Exception {
        PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (posix != null && Files.getPosixFilePermissions(path).stream().anyMatch(permission ->
                permission.name().startsWith("GROUP_") || permission.name().startsWith("OTHERS_"))) {
            throw new IllegalStateException("Agent identity file permissions are unsafe");
        }
    }

    private static String privateKeyPem(byte[] encoded) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(encoded)
                + "\n-----END PRIVATE KEY-----\n";
    }

    private static Path sibling(Path statePath, String fileName) {
        Path parent = statePath.getParent();
        if (parent == null) throw new IllegalArgumentException("Agent identity path has no parent");
        return parent.resolve(fileName).normalize();
    }

    private static Path resolveSibling(Path statePath, String fileName) {
        if (fileName == null || fileName.isBlank() || Path.of(fileName).isAbsolute()
                || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalStateException("Agent identity material filename is invalid");
        }
        return sibling(statePath, fileName);
    }

    private static void requireFile(Path path, String label) {
        if (!Files.isRegularFile(path)) throw new IllegalStateException(label + " is missing");
    }

    record IdentityMaterial(String agentId, String credential,
                            GrpcAgentClient.TlsConfiguration tls, boolean enrolled) {
    }

    private record IdentityState(String agentId, String credential, Instant credentialExpiresAt,
                                 Instant certificateExpiresAt, String publicKeyBase64,
                                 String privateKeyFile, String certificateFile,
                                 String caCertificateFile) {
    }

    private record EnrollmentRequest(String requestedAgentId, String publicKeyBase64) {
    }

    private record RotationRequest(String publicKeyBase64) {
    }

    private record EnrollmentResponse(String agentId, String credential,
                                      Instant credentialExpiresAt, String certificatePem,
                                      String caCertificatePem, Instant certificateExpiresAt) {
    }
}
