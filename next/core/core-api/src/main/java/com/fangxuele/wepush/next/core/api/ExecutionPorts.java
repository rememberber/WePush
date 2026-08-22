package com.fangxuele.wepush.next.core.api;

public record ExecutionPorts(
        RecipientSource recipientSource,
        SecretResolver secretResolver,
        ResultSink resultSink,
        ArtifactSink artifactSink,
        RunEventSink eventSink,
        ExecutionClock clock
) {
    public ExecutionPorts {
        if (recipientSource == null || secretResolver == null || resultSink == null
                || artifactSink == null || eventSink == null || clock == null) {
            throw new IllegalArgumentException("execution ports must not contain null values");
        }
    }
}
