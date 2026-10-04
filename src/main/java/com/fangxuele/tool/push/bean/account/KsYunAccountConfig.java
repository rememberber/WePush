package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/** 金山云短信账号配置。 */
@Data
public class KsYunAccountConfig {
    private String accessKeyId;
    private String accessKeySecret;
    private String sign;
    private String region = "cn-beijing-6";

    public String endpoint() {
        if ("cn-beijing-6".equals(region)) return "https://ksms.ksyun.com/";
        if ("cn-guangzhou-1".equals(region)) return "https://ksms.cn-guangzhou-1.api.ksyun.com/";
        throw new IllegalArgumentException("金山云短信区域须为 cn-beijing-6 或 cn-guangzhou-1");
    }

    public void validate() {
        if (accessKeyId == null || accessKeyId.isBlank() || accessKeySecret == null
                || accessKeySecret.isBlank() || sign == null || sign.isBlank()) {
            throw new IllegalArgumentException("AccessKey、SecretKey 和短信签名不能为空");
        }
        endpoint();
    }
}
