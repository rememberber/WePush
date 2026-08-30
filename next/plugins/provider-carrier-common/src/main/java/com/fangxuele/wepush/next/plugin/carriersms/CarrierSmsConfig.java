package com.fangxuele.wepush.next.plugin.carriersms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fangxuele.wepush.next.core.api.ConfigDocument;
import com.fangxuele.wepush.next.core.api.SecretRef;

record CarrierSmsConfig(
        CarrierProtocol protocol,
        String host,
        int port,
        String username,
        SecretRef password,
        String version,
        int maxChannels,
        int windowSize,
        int requestTimeoutMillis,
        int heartbeatIntervalSeconds,
        String sourceAddress,
        String serviceId,
        String msgSrc,
        long nodeId,
        String corpId,
        String systemType,
        boolean addZeroByte,
        String chargeNumber,
        String feeType,
        String feeCode,
        String feeValue,
        String fixedFee,
        int sourceTon,
        int sourceNpi,
        int destinationTon,
        int destinationNpi
) {
    static CarrierSmsConfig parse(CarrierProtocol protocol, ConfigDocument document) {
        JsonNode root = CarrierProviderSupport.object(document, protocol + " account");
        String host = CarrierProviderSupport.required(root, "host");
        if (host.contains("://") || host.length() > 253) {
            throw CarrierProviderSupport.invalid("host", "INVALID_HOST", "host must not contain a URI scheme");
        }
        String version = CarrierProviderSupport.optional(root, "version", protocol.defaultVersion());
        validateVersion(protocol, version);
        String username = CarrierProviderSupport.required(root, "username");
        String sourceAddress = CarrierProviderSupport.required(root, "sourceAddress");
        String serviceId = CarrierProviderSupport.optional(root, "serviceId", "");
        String msgSrc = CarrierProviderSupport.optional(root, "msgSrc", "");
        long nodeId = CarrierProviderSupport.longValue(root, "nodeId", 0, 0, 0xffffffffL);
        String corpId = CarrierProviderSupport.optional(root, "corpId", "");
        String systemType = CarrierProviderSupport.optional(root, "systemType", "");
        if (protocol == CarrierProtocol.CMPP && msgSrc.isBlank()) {
            throw CarrierProviderSupport.invalid("msgSrc", "FIELD_REQUIRED", "CMPP msgSrc is required");
        }
        if (protocol == CarrierProtocol.SGIP && (nodeId == 0 || corpId.isBlank())) {
            throw CarrierProviderSupport.invalid("nodeId", "SGIP_IDENTITY_REQUIRED", "SGIP nodeId and corpId are required");
        }
        max(username, protocol == CarrierProtocol.CMPP ? 6 : protocol == CarrierProtocol.SMGP ? 8
                : protocol == CarrierProtocol.SGIP ? 16 : 15, "username");
        max(sourceAddress, protocol == CarrierProtocol.SMPP ? 20 : 21, "sourceAddress");
        if (protocol == CarrierProtocol.SMPP) {
            max(systemType, 12, "systemType"); max(serviceId, 5, "serviceId");
        } else { max(serviceId, 10, "serviceId"); }
        ascii(username, "username"); ascii(sourceAddress, "sourceAddress"); ascii(serviceId, "serviceId");
        ascii(msgSrc, "msgSrc"); ascii(corpId, "corpId"); ascii(systemType, "systemType");
        return new CarrierSmsConfig(protocol, host,
                CarrierProviderSupport.integer(root, "port", protocol.defaultPort(), 1, 65535), username,
                CarrierProviderSupport.secret(root, "password"), version,
                CarrierProviderSupport.integer(root, "maxChannels", 1, 1, 64),
                CarrierProviderSupport.integer(root, "windowSize", 16, 1, 32767),
                CarrierProviderSupport.integer(root, "requestTimeoutMillis", 10_000, 100, 300_000),
                CarrierProviderSupport.integer(root, "heartbeatIntervalSeconds", 30, 5, 300),
                sourceAddress, serviceId, msgSrc, nodeId, corpId, systemType,
                CarrierProviderSupport.bool(root, "addZeroByte", false),
                CarrierProviderSupport.optional(root, "chargeNumber", "000000000000000000000"),
                CarrierProviderSupport.optional(root, "feeType", ""),
                CarrierProviderSupport.optional(root, "feeCode", ""),
                CarrierProviderSupport.optional(root, "feeValue", ""),
                CarrierProviderSupport.optional(root, "fixedFee", ""),
                CarrierProviderSupport.integer(root, "sourceTon", 0, 0, 255),
                CarrierProviderSupport.integer(root, "sourceNpi", 0, 0, 255),
                CarrierProviderSupport.integer(root, "destinationTon", 0, 0, 255),
                CarrierProviderSupport.integer(root, "destinationNpi", 1, 0, 255));
    }

    static String message(ConfigDocument document) {
        JsonNode root = CarrierProviderSupport.object(document, "carrier SMS message");
        String content = CarrierProviderSupport.required(root, "content");
        if (content.length() > 10_000) throw CarrierProviderSupport.invalid("content", "CONTENT_TOO_LONG", "content must not exceed 10000 characters");
        return content;
    }

    private static void validateVersion(CarrierProtocol protocol, String version) {
        try {
            int code = protocol.versionCode(version);
            boolean valid = switch (protocol) {
                case CMPP -> code == 0x20 || code == 0x30;
                case SMGP -> code == 0x30;
                case SGIP -> code == 0x12;
                case SMPP -> code == 0x34;
            };
            if (!valid) throw new IllegalArgumentException();
        } catch (RuntimeException problem) {
            throw CarrierProviderSupport.invalid("version", "UNSUPPORTED_PROTOCOL_VERSION", protocol + " version is unsupported");
        }
    }

    private static void ascii(String value, String field) {
        if (value.chars().anyMatch(character -> character > 0x7f)) {
            throw CarrierProviderSupport.invalid(field, "ASCII_REQUIRED", field + " must contain only ASCII characters");
        }
    }

    private static void max(String value, int maximum, String field) {
        if (value.length() > maximum) {
            throw CarrierProviderSupport.invalid(field, "FIELD_TOO_LONG", field + " must not exceed " + maximum + " characters");
        }
    }
}
