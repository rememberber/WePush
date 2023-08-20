package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * 阿里云账号配置
 */
@Data
public class AliYunAccountConfig {
    private String accessKeyId;
    private String accessKeySecret;
    private String sign;
}
