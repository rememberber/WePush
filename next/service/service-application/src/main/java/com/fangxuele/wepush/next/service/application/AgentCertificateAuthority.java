package com.fangxuele.wepush.next.service.application;

import java.security.PublicKey;
import java.time.Instant;

public interface AgentCertificateAuthority {
    IssuedCertificate issueClientCertificate(String agentId, PublicKey publicKey,
                                             Instant notBefore, Instant notAfter);

    record IssuedCertificate(String certificatePem, String caCertificatePem,
                             String fingerprintSha256, Instant expiresAt) {
    }
}
