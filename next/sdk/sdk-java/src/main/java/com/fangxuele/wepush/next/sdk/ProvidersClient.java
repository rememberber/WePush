package com.fangxuele.wepush.next.sdk;

import com.fangxuele.wepush.next.service.api.ProviderSummaryResponse;

import java.util.List;

public final class ProvidersClient {
    private final HttpTransport transport;

    ProvidersClient(HttpTransport transport) {
        this.transport = transport;
    }

    public List<ProviderSummaryResponse> list() {
        ProviderSummaryResponse[] providers = transport.getJson(
                "/api/v1/providers", ProviderSummaryResponse[].class);
        return List.of(providers);
    }

    public String accountSchema(ProviderSummaryResponse provider) {
        return transport.getText(provider.links().accountSchema());
    }

    public String messageSchema(ProviderSummaryResponse provider) {
        return transport.getText(provider.links().messageSchema());
    }

    public String recipientSchema(ProviderSummaryResponse provider) {
        return transport.getText(provider.links().recipientSchema());
    }
}
