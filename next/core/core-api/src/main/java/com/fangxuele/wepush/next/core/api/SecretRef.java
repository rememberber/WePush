package com.fangxuele.wepush.next.core.api;

public record SecretRef(String namespace, String name, String version) {
    public SecretRef {
        namespace = ApiChecks.notBlank(namespace, "namespace");
        name = ApiChecks.notBlank(name, "name");
        version = ApiChecks.notBlank(version, "version");
    }
}
