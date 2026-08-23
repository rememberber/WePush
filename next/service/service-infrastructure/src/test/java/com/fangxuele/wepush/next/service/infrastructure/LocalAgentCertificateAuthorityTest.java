package com.fangxuele.wepush.next.service.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalAgentCertificateAuthorityTest {
    @TempDir
    Path temporary;

    @Test
    void issuesClientAuthCertificateAndReloadsSameAuthority() throws Exception {
        Instant now = Instant.parse("2026-08-23T00:00:00Z");
        Path key = temporary.resolve("ca-key.pem");
        Path certificate = temporary.resolve("ca-cert.pem");
        LocalAgentCertificateAuthority first = new LocalAgentCertificateAuthority(
                key, certificate, Clock.fixed(now, ZoneOffset.UTC), new SecureRandom(),
                Duration.ofDays(3650));
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        var client = generator.generateKeyPair();

        var issued = first.issueClientCertificate("edge-agent-1", client.getPublic(),
                now.minusSeconds(30), now.plus(Duration.ofDays(90)));
        X509Certificate parsed = parse(issued.certificatePem());
        parsed.verify(first.caCertificate().getPublicKey());

        assertEquals(-1, parsed.getBasicConstraints());
        assertEquals(List.of("1.3.6.1.5.5.7.3.2"), parsed.getExtendedKeyUsage());
        assertTrue(parsed.getSubjectAlternativeNames().stream().anyMatch(entry ->
                entry.get(1).equals("spiffe://wepush/agents/edge-agent-1")));
        assertEquals(64, issued.fingerprintSha256().length());
        assertFalse(issued.caCertificatePem().isBlank());

        LocalAgentCertificateAuthority reloaded = new LocalAgentCertificateAuthority(
                key, certificate, Clock.fixed(now, ZoneOffset.UTC), new SecureRandom(),
                Duration.ofDays(3650));
        assertEquals(first.caCertificate(), reloaded.caCertificate());

        if (Files.getFileAttributeView(key, PosixFileAttributeView.class) != null) {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(key);
            assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), permissions);
        }
    }

    private static X509Certificate parse(String pem) throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new java.io.ByteArrayInputStream(pem.getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    }
}
