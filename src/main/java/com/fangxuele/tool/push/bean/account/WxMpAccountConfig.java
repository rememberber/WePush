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
}
