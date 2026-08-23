package com.fangxuele.wepush.next.core.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface ArtifactSink extends AutoCloseable {
    List<ArtifactRef> artifacts();

    /** Writes an execution Artifact and returns its immutable, integrity-bound reference. */
    default ArtifactRef write(String type, String originalName, String contentType,
                              ContentWriter writer) throws IOException {
        throw new UnsupportedOperationException("Artifact writes are not available in this execution mode");
    }

    @Override
    default void close() {
    }

    static ArtifactSink none() {
        return List::of;
    }

    @FunctionalInterface
    interface ContentWriter {
        void write(OutputStream output) throws IOException;
    }
}
