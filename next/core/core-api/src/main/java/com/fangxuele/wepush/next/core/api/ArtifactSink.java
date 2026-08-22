package com.fangxuele.wepush.next.core.api;

import java.util.List;

public interface ArtifactSink extends AutoCloseable {
    List<ArtifactRef> artifacts();

    @Override
    default void close() {
    }

    static ArtifactSink none() {
        return List::of;
    }
}
