package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.CarrierSmsAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsRequestFactory;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsResponseParser;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsSessionRegistry;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsSubmitResult;
import com.fangxuele.tool.push.logic.carriersms.CarrierSmsErrorTranslator;
import com.fangxuele.tool.push.logic.msgmaker.CarrierSmsMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.zx.sms.BaseMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;

/** Classic 运营商协议短信发送器。 */
@Slf4j
public class CarrierSmsMsgSender implements IMsgSender {
    private static final TAccountMapper ACCOUNT_MAPPER = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static final TMsgMapper MSG_MAPPER = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private final int accountId;
    private final int dryRun;
    private final CarrierSmsAccountConfig accountConfig;
    private final CarrierSmsMsgMaker msgMaker;

    public CarrierSmsMsgSender(Integer msgId, Integer dryRun) {
        TMsg message = MSG_MAPPER.selectByPrimaryKey(msgId);
        if (message == null) {
            throw new IllegalArgumentException("消息不存在，msgId=" + msgId);
        }
        TAccount account = ACCOUNT_MAPPER.selectByPrimaryKey(message.getAccountId());
        if (account == null) {
            throw new IllegalArgumentException("短信账号不存在，accountId=" + message.getAccountId());
        }
        this.accountId = account.getId();
        this.dryRun = dryRun == null ? 0 : dryRun;
        this.msgMaker = new CarrierSmsMsgMaker(message);
        this.accountConfig = JSON.parseObject(account.getAccountConfig(), CarrierSmsAccountConfig.class);
        if (accountConfig == null) {
            throw new IllegalArgumentException("运营商短信账号配置为空");
        }
        List<String> errors = accountConfig.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("；", errors));
        }
    }

    CarrierSmsMsgSender(int accountId, int dryRun, CarrierSmsAccountConfig accountConfig,
                        CarrierSmsMsgMaker msgMaker) {
        this.accountId = accountId;
        this.dryRun = dryRun;
        this.accountConfig = accountConfig;
        this.msgMaker = msgMaker;
        List<String> errors = accountConfig.validate();
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("；", errors));
        }
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult result = new SendResult();
        try {
            if (msgData == null || msgData.length == 0) {
                throw new IllegalArgumentException("接收号码不能为空");
            }
            String mobile = msgData[0] == null ? "" : msgData[0].trim();
            String content = msgMaker.makeMsg(msgData);
            if (dryRun == 1) {
                result.setSuccess(true);
                result.setInfo("模拟发送：" + accountConfig.getProtocol() + " 未建立网络连接");
                return result;
            }

            BaseMessage request = CarrierSmsRequestFactory.create(accountConfig, mobile, content);
            long startedAt = System.nanoTime();
            List<BaseMessage> responses = CarrierSmsSessionRegistry.submit(accountId, accountConfig, request);
            CarrierSmsSubmitResult submitResult = CarrierSmsResponseParser.parse(accountConfig.getProtocol(), responses);
            result.setSuccess(submitResult.success());
            result.setInfo(submitResult.info());
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            if (submitResult.success()) {
                log.info("运营商短信网关已受理，protocol={}，accountId={}，fragments={}，elapsedMs={}",
                        accountConfig.getProtocol(), accountId, responses.size(), elapsedMillis);
            } else {
                log.error("{}", submitResult.info());
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setInfo(CarrierSmsErrorTranslator.connectionFailure(accountConfig.getProtocol(), e));
            log.error("运营商协议短信发送失败，protocol={}, accountId={}", accountConfig.getProtocol(), accountId,
                    ExceptionUtils.getRootCause(e) == null ? e : ExceptionUtils.getRootCause(e));
        }
        return result;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return send(msgData);
    }

    public static void removeAccount(Integer accountId) {
        CarrierSmsSessionRegistry.invalidate(accountId);
        ProviderTrafficController.invalidate(MessageTypeEnum.CARRIER_SMS_CODE, accountId);
    }
}
