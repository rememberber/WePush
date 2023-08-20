package com.fangxuele.tool.push.bean.account;

import lombok.Data;

/**
 * Email账号配置
 */
@Data
public class EmailAccountConfig {
    private boolean mailStartTLS;
    private boolean mailSSL;
    private String mailHost;
    private String mailPort;
    private String mailFrom;
    private String mailUser;
    private String mailPassword;
}
