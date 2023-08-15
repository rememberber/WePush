package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.TxYunAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.TxYunMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 腾讯云模板短信发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class TxYunMsgSender implements IMsgSender {
    /**
     * 腾讯云短信sender
     */
    private SmsSingleSender smsSingleSender;

    private TxYunMsgMaker txYunMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static Map<Integer, SmsSingleSender> smsSingleSenderMap = new HashMap<>();

    private TxYunAccountConfig txYunAccountConfig;


    public TxYunMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        txYunMsgMaker = new TxYunMsgMaker(tMsg);
        smsSingleSender = getTxYunSender(tMsg.getAccountId());
        this.dryRun = dryRun;

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        txYunAccountConfig = JSON.parseObject(accountConfig, TxYunAccountConfig.class);
    }

    public static void removeAccount(Integer account1Id) {
        smsSingleSenderMap.remove(account1Id);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            int templateId = txYunMsgMaker.getTemplateId();
            String smsSign = txYunAccountConfig.getSign();
            String[] params = txYunMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                SmsSingleSenderResult result = smsSingleSender.sendWithParam("86", telNum,
                        templateId, params, smsSign, "", "");

                if (result.result == 0) {
                    sendResult.setSuccess(true);
                } else {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(result.toString());
                }
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

    public SmsSingleSender getTxYunSender(Integer accountId) {
        if (smsSingleSenderMap.containsKey(accountId)) {
            return smsSingleSenderMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            TxYunAccountConfig txYunAccountConfig = JSON.parseObject(accountConfig, TxYunAccountConfig.class);

            String txyunAppId = txYunAccountConfig.getAppId();
            String txyunAppKey = txYunAccountConfig.getAppKey();

            SmsSingleSender smsSingleSender = new SmsSingleSender(Integer.parseInt(txyunAppId), txyunAppKey);

            smsSingleSenderMap.put(accountId, smsSingleSender);
            return smsSingleSender;
        }
    }
}
