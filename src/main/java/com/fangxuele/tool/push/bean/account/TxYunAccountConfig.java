package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * 腾讯云账号配置
 */
@Data
public class TxYunAccountConfig {
    private String sign;
    private String appKey;
    private String appId;
}
