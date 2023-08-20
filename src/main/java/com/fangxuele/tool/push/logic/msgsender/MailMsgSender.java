package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.EmailAccountConfig;
import com.fangxuele.tool.push.bean.msg.MailMsg;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.MailMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * E-Mail发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/23.
 */
@Slf4j
public class MailMsgSender implements IMsgSender {

    private MailMsgMaker mailMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static Map<Integer, MailAccount> mailAccountMap = new HashMap<>();

    private MailAccount mailAccount;

    public MailMsgSender() {
    }

    public MailMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        mailMsgMaker = new MailMsgMaker(tMsg);
        mailAccount = getMailAccount(tMsg.getAccountId());
        this.dryRun = dryRun;
    }

    public static void removeAccount(Integer account1Id) {
        mailAccountMap.remove(account1Id);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            MailMsg mailMsg = mailMsgMaker.makeMsg(msgData);
            List<String> tos = Lists.newArrayList();
            tos.add(msgData[0]);
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                List<String> ccList = null;
                if (StringUtils.isNotBlank(mailMsg.getMailCc())) {
                    ccList = Lists.newArrayList();
                    ccList.add(mailMsg.getMailCc());
                }
                if (CollectionUtils.isEmpty(mailMsg.getMailFiles())) {
                    MailUtil.send(mailAccount, tos, ccList, null, mailMsg.getMailTitle(), mailMsg.getMailContent(), true);
                } else {
                    MailUtil.send(mailAccount, tos, ccList, null, mailMsg.getMailTitle(), mailMsg.getMailContent(), true, mailMsg.getMailFiles().toArray(new File[0]));
                }
                sendResult.setSuccess(true);
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }

        return sendResult;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }

    /**
     * 发送推送结果
     *
     * @param tos
     * @return
     */
    public SendResult sendPushResultMail(List<String> tos, String title, String content, File[] files) {
        SendResult sendResult = new SendResult();

        try {
            MailUtil.send(getMailAccount(), tos, title, content, true, files);
            sendResult.setSuccess(true);
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(e.toString());
        }

        return sendResult;
    }

    /**
     * 获取E-Mail发送客户端
     *
     * @return MailAccount
     */
    private MailAccount getMailAccount() {
        if (mailAccount == null) {
            synchronized (MailMsgSender.class) {
                if (mailAccount == null) {
                    String mailHost = App.config.getMailHost();
                    String mailPort = App.config.getMailPort();
                    String mailFrom = App.config.getMailFrom();
                    String mailUser = App.config.getMailUser();
                    String mailPassword = App.config.getMailPassword();

                    mailAccount = new MailAccount();
                    mailAccount.setHost(mailHost);
                    mailAccount.setPort(Integer.valueOf(mailPort));
                    mailAccount.setAuth(true);
                    mailAccount.setFrom(mailFrom);
                    mailAccount.setUser(mailUser);
                    mailAccount.setPass(mailPassword);
                    mailAccount.setSslEnable(App.config.isMailUseSSL());
                    mailAccount.setStarttlsEnable(App.config.isMailUseStartTLS());
                }
            }
        }
        return mailAccount;
    }

    private MailAccount getMailAccount(Integer accountId) {
        if (mailAccountMap.containsKey(accountId)) {
            return mailAccountMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            EmailAccountConfig emailAccountConfig = JSON.parseObject(accountConfig, EmailAccountConfig.class);

            MailAccount mailAccount = new MailAccount();
            mailAccount.setHost(emailAccountConfig.getMailHost());
            mailAccount.setPort(Integer.valueOf(emailAccountConfig.getMailPort()));
            mailAccount.setAuth(true);
            mailAccount.setFrom(emailAccountConfig.getMailFrom());
            mailAccount.setUser(emailAccountConfig.getMailUser());
            mailAccount.setPass(emailAccountConfig.getMailPassword());
            mailAccount.setSslEnable(emailAccountConfig.isMailSSL());
            mailAccount.setStarttlsEnable(emailAccountConfig.isMailStartTLS());

            mailAccountMap.put(accountId, mailAccount);
            return mailAccount;
        }
    }

    public SendResult sendTestMail(EmailAccountConfig emailAccountConfig, String tos) {
        SendResult sendResult = new SendResult();

        try {
            MailAccount mailAccount = new MailAccount();
            mailAccount.setHost(emailAccountConfig.getMailHost());
            mailAccount.setPort(Integer.valueOf(emailAccountConfig.getMailPort()));
            mailAccount.setAuth(true);
            mailAccount.setFrom(emailAccountConfig.getMailFrom());
            mailAccount.setUser(emailAccountConfig.getMailUser());
            mailAccount.setPass(emailAccountConfig.getMailPassword());
            mailAccount.setSslEnable(emailAccountConfig.isMailSSL());
            mailAccount.setStarttlsEnable(emailAccountConfig.isMailStartTLS());

            MailUtil.send(mailAccount, tos, "这是一封来自WePush的测试邮件",
                    "<h1>恭喜，配置正确，邮件发送成功！</h1><p>来自WePush，一款专注于批量推送的小而美的工具。</p>", true);
            sendResult.setSuccess(true);
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(e.toString());
        }

        return sendResult;
    }
}
