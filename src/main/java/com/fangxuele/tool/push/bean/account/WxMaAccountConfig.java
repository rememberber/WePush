package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * 微信小程序账号配置
 */
@Data
public class WxMaAccountConfig {
    private String appId;
    private String appSecret;
    private String token;
    private String aesKey;
    private boolean maUseProxy;
    private String maProxyHost;
    private String maProxyPort;
    private String maProxyUserName;
    private String maProxyPassword;
}
