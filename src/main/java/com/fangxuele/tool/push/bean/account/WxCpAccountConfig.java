package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * 微信企业号账号配置
 */
@Data
public class WxCpAccountConfig {
    private String corpId;
    private String appName;
    private String agentId;
    private String secret;
    private Boolean privateDep;
    private String baseApiUrl;
}
