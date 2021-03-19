package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * 钉钉账号配置
 */
@Data
public class DingAccountConfig {
    private String appSecret;
    private String appName;
    private String agentId;
    private String appKey;
}
