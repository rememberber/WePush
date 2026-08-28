package com.fangxuele.wepush.next.provider.standard;

final class ProviderConfigException extends IllegalArgumentException {
    private final String path;
    private final String code;

    ProviderConfigException(String path, String code, String message) {
        super(message);
        this.path = path;
        this.code = code;
    }

    String path() { return path; }

    String code() { return code; }
}
