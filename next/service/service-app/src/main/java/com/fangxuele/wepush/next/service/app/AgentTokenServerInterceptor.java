package com.fangxuele.wepush.next.service.app;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class AgentTokenServerInterceptor implements ServerInterceptor {
    static final Metadata.Key<String> TOKEN = Metadata.Key.of(
            "x-wepush-agent-token", Metadata.ASCII_STRING_MARSHALLER);

    private final byte[] expected;

    AgentTokenServerInterceptor(String expected) {
        this.expected = expected == null ? new byte[0] : expected.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        if (expected.length == 0) {
            return next.startCall(call, headers);
        }
        String presented = headers.get(TOKEN);
        byte[] actual = presented == null ? new byte[0] : presented.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Agent token is missing or invalid"),
                    new Metadata());
            return new ServerCall.Listener<>() { };
        }
        return next.startCall(call, headers);
    }
}
