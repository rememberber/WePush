package com.fangxuele.tool.push.logic.carriersms;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 将协议结果码和网络异常转换为不包含凭据的可读信息。 */
public final class CarrierSmsErrorTranslator {
    private static final Pattern LOGIN_STATUS = Pattern.compile("status\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Map<Integer, String> CMPP_SUBMIT = Map.ofEntries(
            Map.entry(1, "消息结构错误"),
            Map.entry(2, "命令字错误"),
            Map.entry(3, "消息序号重复"),
            Map.entry(4, "消息长度错误"),
            Map.entry(5, "资费代码错误"),
            Map.entry(6, "超过最大信息长"),
            Map.entry(7, "业务代码错误"),
            Map.entry(8, "流量控制错误")
    );
    private static final Map<Integer, String> SMGP_SUBMIT = Map.ofEntries(
            Map.entry(1, "系统错误"),
            Map.entry(2, "消息结构错误"),
            Map.entry(3, "命令字错误"),
            Map.entry(4, "消息序号重复"),
            Map.entry(5, "消息长度错误"),
            Map.entry(6, "资费代码错误"),
            Map.entry(7, "超过最大信息长"),
            Map.entry(8, "业务代码错误"),
            Map.entry(9, "流量控制错误")
    );
    private static final Map<Integer, String> SGIP_SUBMIT = Map.ofEntries(
            Map.entry(1, "非法命令标识"),
            Map.entry(2, "消息序号重复"),
            Map.entry(3, "消息长度错误"),
            Map.entry(4, "资费代码错误"),
            Map.entry(5, "消息过长"),
            Map.entry(6, "企业代码或业务代码错误"),
            Map.entry(7, "流量控制错误"),
            Map.entry(8, "其他错误")
    );
    private static final Map<Integer, String> SMPP_SUBMIT = Map.ofEntries(
            Map.entry(1, "消息长度错误"),
            Map.entry(3, "命令标识错误"),
            Map.entry(4, "绑定状态错误"),
            Map.entry(5, "已绑定"),
            Map.entry(8, "系统错误"),
            Map.entry(10, "源地址错误"),
            Map.entry(11, "目标地址错误"),
            Map.entry(13, "绑定失败"),
            Map.entry(14, "密码错误"),
            Map.entry(15, "SystemId 错误"),
            Map.entry(20, "消息队列已满"),
            Map.entry(69, "提交失败"),
            Map.entry(88, "流量控制错误")
    );

    private CarrierSmsErrorTranslator() {
    }

    public static String submitStatus(CarrierSmsProtocol protocol, long status) {
        int code = (int) status;
        String description = switch (protocol) {
            case CMPP -> CMPP_SUBMIT.get(code);
            case SMGP -> SMGP_SUBMIT.get(code);
            case SGIP -> SGIP_SUBMIT.get(code);
            case SMPP -> SMPP_SUBMIT.get(code);
        };
        String formattedCode = protocol == CarrierSmsProtocol.SMPP
                ? status + " (0x" + Integer.toHexString(code).toUpperCase() + ")"
                : String.valueOf(status);
        return description == null ? "错误码=" + formattedCode : description + "，错误码=" + formattedCode;
    }

    public static String connectionFailure(CarrierSmsProtocol protocol, Throwable error) {
        Throwable root = rootCause(error);
        if (root instanceof TimeoutException) {
            return protocol + " 网关登录或应答超时";
        }
        if (root instanceof UnknownHostException) {
            return protocol + " 网关域名无法解析";
        }
        if (root instanceof ConnectException || root instanceof NoRouteToHostException) {
            return protocol + " 网关无法连接，请检查地址、端口、网络和 IP 白名单";
        }
        String message = root.getMessage();
        if (message != null) {
            Matcher matcher = LOGIN_STATUS.matcher(message);
            if (matcher.find()) {
                return protocol + " 网关拒绝登录，状态码=" + matcher.group(1) + "，请检查账号、密码、版本和 IP 白名单";
            }
            if (message.contains("login Failed") || message.contains("登录失败")) {
                return protocol + " 网关登录失败，请检查账号、密码、版本和 IP 白名单";
            }
        }
        return protocol + " 通信失败（" + root.getClass().getSimpleName() + "）";
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
