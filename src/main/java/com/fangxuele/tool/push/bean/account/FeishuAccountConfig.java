package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * 飞书自定义机器人账号配置。
 */
@Data
public class FeishuAccountConfig {
    /**
     * 群自定义机器人的 Webhook 地址。
     */
    private String webhook;

    /**
     * 开启“签名校验”后由飞书生成的密钥，可选。
     */
    private String secret;

    /**
     * 开启“关键词”安全设置时配置。文本和富文本消息会自动添加此前缀。
     */
    private String keyword;
}
