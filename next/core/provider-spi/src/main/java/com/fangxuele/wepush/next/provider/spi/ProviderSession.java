package com.fangxuele.wepush.next.provider.spi;

import com.fangxuele.wepush.next.core.api.CancellationToken;

public interface ProviderSession extends AutoCloseable {
    ProviderResult send(ProviderSendRequest request, CancellationToken token) throws Exception;

    @Override
    void close() throws Exception;
}
