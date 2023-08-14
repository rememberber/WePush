package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * 微信公众号账号配置
 */
@Data
public class WxMpAccountConfig {
    private String appId;
    private String appSecret;
    private String token;
    private String aesKey;
    private boolean mpUseProxy;
    private String mpProxyHost;
    private String mpProxyPort;
    private String mpProxyUserName;
    private String mpProxyPassword;
    private boolean mpUseOutSideAt;
    private boolean mpManualAt;
    private boolean mpApiAt;
    private String mpAt;
    private String mpAtExpiresIn;
    private String mpAtApiUrl;
}
