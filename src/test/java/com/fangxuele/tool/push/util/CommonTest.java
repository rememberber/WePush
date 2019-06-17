package com.fangxuele.tool.push.util;

import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import org.junit.Test;

/**
 * <pre>
 * 通用测试
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/17.
 */
public class CommonTest {
    @Test
    public void test() {
        MailAccount account = new MailAccount();
        account.setHost("smtp.yeah.net");
        account.setPort(25);
        account.setAuth(true);
        account.setFrom("hutool@yeah.net");
        account.setUser("hutool");
        account.setPass("q1w2e3");

        MailUtil.send(account, "rememberber@163.com", "测试", "邮件来自Hutool测试", false);
    }
}
