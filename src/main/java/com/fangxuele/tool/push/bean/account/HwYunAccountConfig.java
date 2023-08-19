package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * 华为云账号配置
 */
@Data
public class HwYunAccountConfig {
    private String accessUrl;
    private String senderCode;
    private String signature;
    private String appSecret;
    private String appKey;
}
