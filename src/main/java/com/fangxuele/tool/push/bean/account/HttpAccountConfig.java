package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * Http账号配置
 */
@Data
public class HttpAccountConfig {
    private boolean useProxy;
    private String proxyHost;
    private String proxyPort;
    private String proxyUserName;
    private String proxyPassword;
}
