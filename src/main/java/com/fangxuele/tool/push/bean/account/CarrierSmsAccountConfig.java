package com.fangxuele.tool.push.bean.account;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsProtocol;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** CMPP/SMGP/SGIP/SMPP 的统一账号配置。 */
@Data
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

    private String sourceAddress;
    private String serviceId;
    private String msgSrc;
    private long nodeId;
    private String corpId;
    private String systemType;
    private boolean addZeroByte;
    private boolean registeredDelivery = true;

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
        if (maxChannels <= 0) {
            maxChannels = 1;
        }
        if (windowSize <= 0) {
            windowSize = 16;
        }
        if (requestTimeoutMillis <= 0) {
            requestTimeoutMillis = 10000;
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
        if (maxChannels < 1 || maxChannels > Short.MAX_VALUE) {
            errors.add("连接数必须在 1-32767 之间");
        }
        if (windowSize < 1) {
            errors.add("发送窗口必须大于 0");
        }
        if (requestTimeoutMillis < 100) {
            errors.add("应答超时不能小于 100 ms");
        }
        validateVersion(errors);
        if (protocol == CarrierSmsProtocol.CMPP && StringUtils.isBlank(msgSrc)) {
            errors.add("CMPP 请填写企业代码 MsgSrc");
        }
        if (protocol == CarrierSmsProtocol.SGIP && nodeId <= 0) {
            errors.add("SGIP 请填写大于 0 的 NodeId");
        }
        if (protocol == CarrierSmsProtocol.SGIP && StringUtils.isNotBlank(feeType)) {
            try {
                Short.parseShort(feeType.trim());
            } catch (NumberFormatException e) {
                errors.add("SGIP FeeType 必须是整数");
            }
        }
        validateByte(sourceTon, "SMPP 源 TON", errors);
        validateByte(sourceNpi, "SMPP 源 NPI", errors);
        validateByte(destinationTon, "SMPP 目标 TON", errors);
        validateByte(destinationNpi, "SMPP 目标 NPI", errors);
        return errors;
    }

    private void validateVersion(List<String> errors) {
        try {
            int code = protocol.versionCode(version);
            boolean valid = switch (protocol) {
                case CMPP -> code == 0x20 || code == 0x30;
                case SMGP -> code == 0x30;
                case SGIP -> true;
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
