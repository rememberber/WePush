package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * 腾讯云3.0账号配置
 */
@Data
public class TxYun3AccountConfig {
    private String secretId;
    private String secretKey;
    private String endPoint;
    private String region;
    private String sign;
    private String sdkAppId;
}
