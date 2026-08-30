package com.fangxuele.wepush.next.plugin.carriersms;

import java.util.Locale;

public enum CarrierProtocol {
    CMPP(7890, "3.0"),
    SMGP(8900, "3.0"),
    SGIP(8801, "1.2"),
    SMPP(2775, "3.4");

    private final int defaultPort;
    private final String defaultVersion;

    CarrierProtocol(int defaultPort, String defaultVersion) {
        this.defaultPort = defaultPort;
        this.defaultVersion = defaultVersion;
    }

    public int defaultPort() { return defaultPort; }

    public String defaultVersion() { return defaultVersion; }

    public String providerId() { return "wepush.sms." + name().toLowerCase(Locale.ROOT); }

    public int versionCode(String value) {
        String normalized = value == null || value.isBlank() ? defaultVersion : value.trim();
        if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
            return Integer.parseInt(normalized.substring(2), 16);
        }
        if (normalized.contains(".")) {
            String[] parts = normalized.split("\\.", 2);
            return (Integer.parseInt(parts[0]) << 4) | Integer.parseInt(parts[1]);
        }
        return Integer.parseInt(normalized);
    }
}
