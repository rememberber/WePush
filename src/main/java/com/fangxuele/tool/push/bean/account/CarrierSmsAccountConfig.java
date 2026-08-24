package com.fangxuele.tool.push.bean.account;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsProtocol;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** CMPP/SMGP/SGIP/SMPP 的统一账号配置。 */
@Data
@ToString(exclude = "password")
public class CarrierSmsAccountConfig {
    private CarrierSmsProtocol protocol = CarrierSmsProtocol.CMPP;
    private String host;
    private int port;
    private String username;
    private String password;
    private String version;
    private int maxChannels = 1;
    private int windowSize = 16;
    private int requestTimeoutMillis = 10000;
    private int heartbeatIntervalSeconds = 30;

    private String sourceAddress;
    private String serviceId;
    private String msgSrc;
    private long nodeId;
    private String corpId;
    private String systemType;
    private boolean addZeroByte;

    private String chargeNumber = "000000000000000000000";
    private String feeType;
    private String feeCode;
    private String feeValue;
    private String fixedFee;

    private int sourceTon;
    private int sourceNpi;
    private int destinationTon;
    private int destinationNpi = 1;

    public void applyDefaults() {
        protocol = CarrierSmsProtocol.from(protocol);
        if (port <= 0) {
            port = protocol.getDefaultPort();
        }
        if (StringUtils.isBlank(version)) {
            version = protocol.getDefaultVersion();
        }
        if (chargeNumber == null) {
            chargeNumber = "000000000000000000000";
        }
    }

    public List<String> validate() {
        applyDefaults();
        List<String> errors = new ArrayList<>();
        if (StringUtils.isBlank(host)) {
            errors.add("请填写网关地址");
        } else if (host.contains("://")) {
            errors.add("网关地址不要包含协议前缀");
        }
        if (port < 1 || port > 65535) {
            errors.add("网关端口必须在 1-65535 之间");
        }
        if (StringUtils.isBlank(username)) {
            errors.add("请填写登录账号");
        }
        if (StringUtils.isBlank(password)) {
            errors.add("请填写登录密码");
        }
        if (StringUtils.isBlank(sourceAddress)) {
            errors.add("请填写接入号/源地址");
        }
        if (maxChannels < 1 || maxChannels > 64) {
            errors.add("连接数必须在 1-64 之间");
        }
        if (windowSize < 1 || windowSize > Short.MAX_VALUE) {
            errors.add("发送窗口必须在 1-32767 之间");
        }
        if (requestTimeoutMillis < 100 || requestTimeoutMillis > 300000) {
            errors.add("应答超时必须在 100-300000 ms 之间");
        }
        if (heartbeatIntervalSeconds < 5 || heartbeatIntervalSeconds > 300) {
            errors.add("心跳间隔必须在 5-300 秒之间");
        }
        validateVersion(errors);
        validateProtocolFields(errors);
        return errors;
    }

    private void validateProtocolFields(List<String> errors) {
        validateAscii(username, "登录账号", errors);
        validateAscii(password, "登录密码", errors);
        validateAscii(sourceAddress, "接入号/源地址", errors);
        validateAscii(serviceId, "业务代码", errors);
        validateAscii(msgSrc, "企业代码 MsgSrc", errors);
        validateAscii(corpId, "SGIP CorpId", errors);
        validateAscii(systemType, "SMPP SystemType", errors);
        validateAscii(chargeNumber, "SGIP ChargeNumber", errors);
        validateAscii(feeType, "FeeType", errors);
        validateAscii(feeCode, "FeeCode", errors);
        validateAscii(feeValue, "SGIP FeeValue", errors);
        validateAscii(fixedFee, "SMGP FixedFee", errors);

        switch (protocol) {
            case CMPP -> {
                required(msgSrc, "CMPP 请填写企业代码 MsgSrc", errors);
                validateMaxLength(username, 6, "CMPP 登录账号", errors);
                validateMaxLength(sourceAddress, 21, "CMPP 接入号", errors);
                validateMaxLength(serviceId, 10, "CMPP 业务代码", errors);
                validateMaxLength(msgSrc, 6, "CMPP 企业代码 MsgSrc", errors);
                validateMaxLength(feeType, 2, "CMPP FeeType", errors);
                validateMaxLength(feeCode, 6, "CMPP FeeCode", errors);
            }
            case SMGP -> {
                validateMaxLength(username, 8, "SMGP ClientId", errors);
                validateMaxLength(sourceAddress, 21, "SMGP 源号码", errors);
                validateMaxLength(serviceId, 10, "SMGP 业务代码", errors);
                validateMaxLength(feeType, 2, "SMGP FeeType", errors);
                validateMaxLength(feeCode, 6, "SMGP FeeCode", errors);
                validateMaxLength(fixedFee, 6, "SMGP FixedFee", errors);
            }
            case SGIP -> {
                required(corpId, "SGIP 请填写 CorpId", errors);
                if (nodeId <= 0 || nodeId > 0xffffffffL) {
                    errors.add("SGIP NodeId 必须在 1-4294967295 之间");
                }
                validateMaxLength(username, 16, "SGIP 登录账号", errors);
                validateMaxLength(password, 16, "SGIP 登录密码", errors);
                validateMaxLength(sourceAddress, 21, "SGIP SPNumber", errors);
                validateMaxLength(chargeNumber, 21, "SGIP ChargeNumber", errors);
                validateMaxLength(corpId, 5, "SGIP CorpId", errors);
                validateMaxLength(serviceId, 10, "SGIP ServiceType", errors);
                validateMaxLength(feeValue, 6, "SGIP FeeValue", errors);
                if (StringUtils.isNotBlank(feeType)) {
                    try {
                        int value = Integer.parseInt(feeType.trim());
                        if (value < 0 || value > 255) {
                            errors.add("SGIP FeeType 必须在 0-255 之间");
                        }
                    } catch (NumberFormatException e) {
                        errors.add("SGIP FeeType 必须是整数");
                    }
                }
            }
            case SMPP -> {
                validateMaxLength(username, 15, "SMPP SystemId", errors);
                validateMaxLength(password, 8, "SMPP 登录密码", errors);
                validateMaxLength(systemType, 12, "SMPP SystemType", errors);
                validateMaxLength(serviceId, 5, "SMPP ServiceType", errors);
                validateMaxLength(sourceAddress, 20, "SMPP 源地址", errors);
                validateByte(sourceTon, "SMPP 源 TON", errors);
                validateByte(sourceNpi, "SMPP 源 NPI", errors);
                validateByte(destinationTon, "SMPP 目标 TON", errors);
                validateByte(destinationNpi, "SMPP 目标 NPI", errors);
            }
        }
    }

    private void validateVersion(List<String> errors) {
        try {
            int code = protocol.versionCode(version);
            boolean valid = switch (protocol) {
                case CMPP -> code == 0x20 || code == 0x30;
                case SMGP -> code == 0x30;
                case SGIP -> code == 0x12;
                case SMPP -> code == 0x34;
            };
            if (!valid) {
                errors.add(protocol + " 不支持协议版本 " + version);
            }
        } catch (RuntimeException e) {
            errors.add("协议版本格式不正确");
        }
    }

    private static void validateByte(int value, String fieldName, List<String> errors) {
        if (value < 0 || value > 255) {
            errors.add(fieldName + " 必须在 0-255 之间");
        }
    }

    private static void required(String value, String message, List<String> errors) {
        if (StringUtils.isBlank(value)) {
            errors.add(message);
        }
    }

    private static void validateAscii(String value, String fieldName, List<String> errors) {
        if (value != null && value.chars().anyMatch(ch -> ch > 0x7f)) {
            errors.add(fieldName + " 只能包含 ASCII 字符");
        }
    }

    private static void validateMaxLength(String value, int maxLength, String fieldName, List<String> errors) {
        if (value != null && value.length() > maxLength) {
            errors.add(fieldName + " 不能超过 " + maxLength + " 个字符");
        }
    }

    /** 仅用于识别连接配置变更，不暴露明文凭据。 */
    public String connectionFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(JSON.toJSONString(this).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
