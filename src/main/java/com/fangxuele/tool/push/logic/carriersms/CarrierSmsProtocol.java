package com.fangxuele.tool.push.logic.carriersms;

import java.util.Locale;

/** Classic 版支持的运营商短信协议。 */
public enum CarrierSmsProtocol {
    CMPP(7890, "3.0"),
    SMGP(8900, "3.0"),
    SGIP(8801, "1.2"),
    SMPP(2775, "3.4");

    private final int defaultPort;
    private final String defaultVersion;

    CarrierSmsProtocol(int defaultPort, String defaultVersion) {
        this.defaultPort = defaultPort;
        this.defaultVersion = defaultVersion;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public static CarrierSmsProtocol from(Object value) {
        if (value instanceof CarrierSmsProtocol protocol) {
            return protocol;
        }
        if (value == null) {
            return CMPP;
        }
        return valueOf(value.toString().trim().toUpperCase(Locale.ROOT));
    }

    public int versionCode(String version) {
        String normalized = version == null || version.isBlank() ? defaultVersion : version.trim();
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
