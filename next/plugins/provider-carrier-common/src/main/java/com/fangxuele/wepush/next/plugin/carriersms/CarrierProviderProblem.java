package com.fangxuele.wepush.next.plugin.carriersms;

final class CarrierProviderProblem extends RuntimeException {
    private final String path;
    private final String code;

    CarrierProviderProblem(String path, String code, String message) {
        super(message);
        this.path = path;
        this.code = code;
    }

    String path() { return path; }

    String code() { return code; }
}
