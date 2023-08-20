package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * 百度云账号配置
 */
@Data
public class BdYunAccountConfig {
    private String bdEndPoint;
    private String bdInvokeId;
    private String bdSecretAccessKey;
    private String bdAccessKeyId;
}
