package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.AgentIdentityService;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.net.ssl.SSLSession;

final class AgentTokenServerInterceptor implements ServerInterceptor {
    static final Metadata.Key<String> TOKEN = Metadata.Key.of(
            "x-wepush-agent-token", Metadata.ASCII_STRING_MARSHALLER);
    static final Context.Key<String> AUTHENTICATED_AGENT_ID = Context.key("wepush-agent-id");

    private final byte[] expected;
    private final AgentIdentityService identities;
    private final boolean allowAnonymous;

    AgentTokenServerInterceptor(String expected, AgentIdentityService identities,
                                boolean allowAnonymous) {
        this.expected = expected == null ? new byte[0] : expected.getBytes(StandardCharsets.UTF_8);
        this.identities = identities;
        this.allowAnonymous = allowAnonymous;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String presented = headers.get(TOKEN);
        byte[] actual = presented == null ? new byte[0] : presented.getBytes(StandardCharsets.UTF_8);
        if (expected.length > 0 && MessageDigest.isEqual(expected, actual)) {
            return Contexts.interceptCall(Context.current().withValue(AUTHENTICATED_AGENT_ID, ""),
                    call, headers, next);
        }
        if (presented != null && !presented.isBlank()) {
            try {
                AgentIdentityService.AuthenticatedAgent authenticated = identities.authenticate(presented);
                verifyPeerCertificate(call, authenticated.certificateFingerprint());
                String agentId = authenticated.agentId();
                return Contexts.interceptCall(
                        Context.current().withValue(AUTHENTICATED_AGENT_ID, agentId),
                        call, headers, next);
            } catch (AgentIdentityService.InvalidAgentCredentialException ignored) {
                // Return the same generic response for unknown, expired, and malformed credentials.
            }
        }
        if (allowAnonymous && expected.length == 0 && (presented == null || presented.isBlank())) {
            return Contexts.interceptCall(Context.current().withValue(AUTHENTICATED_AGENT_ID, ""),
                    call, headers, next);
        } else {
            call.close(Status.UNAUTHENTICATED.withDescription("Agent token is missing or invalid"),
                    new Metadata());
            return new ServerCall.Listener<>() { };
        }
    }

    private static <ReqT, RespT> void verifyPeerCertificate(
            ServerCall<ReqT, RespT> call, String expectedFingerprint) {
        SSLSession session = call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
        if (session == null || expectedFingerprint == null || expectedFingerprint.isBlank()) return;
        try {
            byte[] encoded = session.getPeerCertificates()[0].getEncoded();
            String actual = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(encoded));
            if (!MessageDigest.isEqual(expectedFingerprint.getBytes(StandardCharsets.US_ASCII),
                    actual.getBytes(StandardCharsets.US_ASCII))) {
                throw new AgentIdentityService.InvalidAgentCredentialException(
                        "Agent certificate does not match its Credential");
            }
        } catch (AgentIdentityService.InvalidAgentCredentialException problem) {
            throw problem;
        } catch (Exception problem) {
            throw new AgentIdentityService.InvalidAgentCredentialException(
                    "Agent client certificate cannot be verified");
        }
    }
}
