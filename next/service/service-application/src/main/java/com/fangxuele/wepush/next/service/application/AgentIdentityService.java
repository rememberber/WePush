package com.fangxuele.wepush.next.service.application;

import com.fangxuele.wepush.next.service.domain.AgentIdentityRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

public final class AgentIdentityService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final AgentIdentityRepository identities;
    private final AgentCertificateAuthority certificates;
    private final WorkspaceRepository workspaces;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final Clock clock;
    private final SecureRandom random;
    private final Duration credentialTtl;

    public AgentIdentityService(AgentIdentityRepository identities,
                                AgentCertificateAuthority certificates,
                                WorkspaceRepository workspaces,
                                ResourceIdGenerator ids, TransactionRunner transactions,
                                Clock clock, SecureRandom random, Duration credentialTtl) {
        this.identities = identities;
        this.certificates = certificates;
        this.workspaces = workspaces;
        this.ids = ids;
        this.transactions = transactions;
        this.clock = clock;
        this.random = random;
        if (credentialTtl == null || credentialTtl.isZero() || credentialTtl.isNegative()) {
            throw new IllegalArgumentException("Agent Credential TTL must be positive");
        }
        this.credentialTtl = credentialTtl;
    }

    public EnrollmentToken createEnrollment(String name, String workspaceId, Duration ttl) {
        String safeName = required(name, "Enrollment name", 120);
        WorkspaceId target = new WorkspaceId(workspaceId == null || workspaceId.isBlank()
                ? "ws_default" : workspaceId);
        if (workspaces.findById(target).isEmpty()) {
            throw new ApplicationProblem(ApplicationProblem.Kind.NOT_FOUND, "WORKSPACE_NOT_FOUND",
                    "Workspace was not found: " + target.value());
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.compareTo(Duration.ofDays(7)) > 0) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "ENROLLMENT_TTL_INVALID",
                    "Enrollment TTL must be positive and no more than seven days");
        }
        Instant now = clock.instant();
        String id = ids.next("enrollment");
        String token = token("wpe", id);
        AgentIdentityRepository.EnrollmentToken record = new AgentIdentityRepository.EnrollmentToken(
                id, safeName, hash(token), target.value(), now.plus(ttl), null, now);
        transactions.required(() -> identities.createEnrollment(record));
        return new EnrollmentToken(id, token, record.expiresAt());
    }

    public EnrollmentResult enroll(String enrollmentToken, String requestedAgentId,
                                   String publicKeyBase64) {
        TokenParts enrollment = parse(enrollmentToken, "wpe");
        String enrollmentHash = hash(enrollmentToken);
        Instant now = clock.instant();
        String agentId = requestedAgentId == null || requestedAgentId.isBlank()
                ? ids.next("agent") : safeAgentId(requestedAgentId);
        CertificateMaterial certificate = certificate(agentId, publicKeyBase64, now);
        CredentialMaterial credential = credential(agentId, certificate.fingerprint(), now);
        boolean consumed = transactions.required(() -> {
            if (!identities.consumeEnrollment(enrollment.id(), enrollmentHash, now)) return false;
            identities.createCredential(credential.record());
            String workspaceId = identities.enrollmentWorkspace(enrollment.id())
                    .orElseThrow(() -> new IllegalStateException("Enrollment Workspace is missing"));
            identities.bindWorkspace(agentId, workspaceId, now);
            return true;
        });
        if (!consumed) {
            throw new InvalidAgentCredentialException("Enrollment Token is invalid, expired, or already used");
        }
        return new EnrollmentResult(agentId, credential.token(), credential.record().expiresAt(),
                certificate.certificatePem(), certificate.caCertificatePem(), certificate.expiresAt());
    }

    public EnrollmentResult rotate(String currentCredential, String publicKeyBase64) {
        AuthenticatedAgent authenticated = authenticate(currentCredential);
        Instant now = clock.instant();
        CertificateMaterial certificate = certificate(authenticated.agentId(), publicKeyBase64, now);
        CredentialMaterial replacement = credential(authenticated.agentId(), certificate.fingerprint(), now);
        transactions.required(() -> {
            identities.createCredential(replacement.record());
            if (!identities.revokeCredential(authenticated.credentialId(), authenticated.agentId(), now)) {
                throw new InvalidAgentCredentialException("Agent Credential was already rotated or revoked");
            }
        });
        return new EnrollmentResult(authenticated.agentId(), replacement.token(),
                replacement.record().expiresAt(), certificate.certificatePem(),
                certificate.caCertificatePem(), certificate.expiresAt());
    }

    public AuthenticatedAgent authenticate(String rawCredential) {
        TokenParts parts = parse(rawCredential, "wpa");
        Instant now = clock.instant();
        AgentIdentityRepository.AgentCredential credential = identities.findCredential(parts.id())
                .orElseThrow(() -> new InvalidAgentCredentialException("Agent Credential is invalid"));
        byte[] actual = hash(rawCredential).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = credential.tokenHash().getBytes(StandardCharsets.US_ASCII);
        if (!credential.activeAt(now) || !MessageDigest.isEqual(expected, actual)) {
            throw new InvalidAgentCredentialException("Agent Credential is invalid or expired");
        }
        transactions.required(() -> identities.touchCredential(credential.id(), now));
        return new AuthenticatedAgent(credential.agentId(), credential.id(), credential.expiresAt(),
                credential.certificateFingerprint());
    }

    public boolean allowedInWorkspace(String agentId, WorkspaceId workspaceId) {
        return identities.allowedInWorkspace(agentId, workspaceId.value());
    }

    private CredentialMaterial credential(String agentId, String certificateFingerprint, Instant now) {
        String id = ids.next("agent-credential");
        String token = token("wpa", id);
        return new CredentialMaterial(token, new AgentIdentityRepository.AgentCredential(id, agentId,
                hash(token), certificateFingerprint, now.plus(credentialTtl), null, now, null));
    }

    private CertificateMaterial certificate(String agentId, String publicKeyBase64, Instant now) {
        if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            return new CertificateMaterial("", "", "", null);
        }
        try {
            byte[] encoded = Base64.getDecoder().decode(publicKeyBase64);
            if (encoded.length > 4096) throw new IllegalArgumentException("public key is too large");
            PublicKey publicKey;
            try {
                publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(encoded));
            } catch (Exception notEc) {
                publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded));
            }
            AgentCertificateAuthority.IssuedCertificate issued = certificates.issueClientCertificate(
                    agentId, publicKey, now.minusSeconds(60), now.plus(credentialTtl));
            return new CertificateMaterial(issued.certificatePem(), issued.caCertificatePem(),
                    issued.fingerprintSha256(), issued.expiresAt());
        } catch (Exception problem) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "AGENT_PUBLIC_KEY_INVALID",
                    "Agent mTLS public key is invalid");
        }
    }

    private String token(String prefix, String id) {
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        return prefix + "." + URL_ENCODER.encodeToString(id.getBytes(StandardCharsets.UTF_8))
                + "." + URL_ENCODER.encodeToString(secret);
    }

    private static TokenParts parse(String token, String expectedPrefix) {
        if (token == null || token.length() > 2048) throw invalid();
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || !expectedPrefix.equals(parts[0])) throw invalid();
        try {
            byte[] idBytes = URL_DECODER.decode(parts[1]);
            byte[] secret = URL_DECODER.decode(parts[2]);
            if (!URL_ENCODER.encodeToString(idBytes).equals(parts[1])
                    || !URL_ENCODER.encodeToString(secret).equals(parts[2]) || secret.length != 32) throw invalid();
            String id = new String(idBytes, StandardCharsets.UTF_8);
            if (id.isBlank() || id.length() > 255) throw invalid();
            return new TokenParts(id);
        } catch (RuntimeException problem) {
            if (problem instanceof InvalidAgentCredentialException invalid) throw invalid;
            throw invalid();
        }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private static String safeAgentId(String value) {
        if (!value.matches("[A-Za-z0-9._-]{1,120}")) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "AGENT_ID_INVALID",
                    "Agent ID contains unsupported characters");
        }
        return value;
    }

    private static String required(String value, String label, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum) {
            throw new ApplicationProblem(ApplicationProblem.Kind.BAD_REQUEST, "AGENT_IDENTITY_INVALID",
                    label + " is required");
        }
        return value.trim();
    }

    private static InvalidAgentCredentialException invalid() {
        return new InvalidAgentCredentialException("Agent Credential format is invalid");
    }

    public record EnrollmentToken(String id, String token, Instant expiresAt) {
    }

    public record EnrollmentResult(String agentId, String credential, Instant credentialExpiresAt,
                                   String certificatePem, String caCertificatePem,
                                   Instant certificateExpiresAt) {
    }

    public record AuthenticatedAgent(String agentId, String credentialId, Instant expiresAt,
                                     String certificateFingerprint) {
    }

    private record TokenParts(String id) {
    }

    private record CredentialMaterial(String token, AgentIdentityRepository.AgentCredential record) {
    }

    private record CertificateMaterial(String certificatePem, String caCertificatePem,
                                       String fingerprint, Instant expiresAt) {
    }

    public static final class InvalidAgentCredentialException extends RuntimeException {
        public InvalidAgentCredentialException(String message) {
            super(message);
        }
    }
}
