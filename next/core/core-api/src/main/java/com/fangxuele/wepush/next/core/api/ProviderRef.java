package com.fangxuele.wepush.next.core.api;

public record ProviderRef(String providerId, String implementationVersion) {
    public ProviderRef {
        providerId = ApiChecks.notBlank(providerId, "providerId");
        implementationVersion = ApiChecks.notBlank(implementationVersion, "implementationVersion");
    }
}
