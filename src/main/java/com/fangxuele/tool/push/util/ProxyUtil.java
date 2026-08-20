package com.fangxuele.tool.push.util;

import java.net.Proxy;

/**
 * 代理工具类
 */
public class ProxyUtil {

    public static final String PROXY_TYPE_HTTP = "HTTP";
    public static final String PROXY_TYPE_SOCKS = "SOCKS";

    public static final String[] PROXY_TYPES = {PROXY_TYPE_HTTP, PROXY_TYPE_SOCKS};

    /**
     * 根据代理类型字符串获取Proxy.Type
     *
     * @param proxyType 代理类型字符串
     * @return Proxy.Type
     */
    public static Proxy.Type getProxyType(String proxyType) {
        if (PROXY_TYPE_SOCKS.equalsIgnoreCase(proxyType)) {
            return Proxy.Type.SOCKS;
        }
        return Proxy.Type.HTTP;
    }
}
