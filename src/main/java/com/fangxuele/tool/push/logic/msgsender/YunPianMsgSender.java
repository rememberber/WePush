package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.YunPianAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.YunPianMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.yunpian.sdk.YunpianClient;
import com.yunpian.sdk.model.Result;
import com.yunpian.sdk.model.SmsSingleSend;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 云片网短信发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class YunPianMsgSender implements IMsgSender {
    /**
     * 云片网短信client
     */
    public volatile static YunpianClient yunpianClient;

    private YunPianMsgMaker yunPianMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static Map<Integer, YunpianClient> yunpianClientMap = new HashMap<>();


    public YunPianMsgSender() {
    }

    public YunPianMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        yunPianMsgMaker = new YunPianMsgMaker(tMsg);
        yunpianClient = getYunpianClient(tMsg.getAccountId());
        this.dryRun = dryRun;
    }


    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            Map<String, String> params = yunPianMsgMaker.makeMsg(msgData);
            String telNum = msgData[0];
            params.put(YunpianClient.MOBILE, telNum);
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                Result<SmsSingleSend> result = yunpianClient.sms().single_send(params);
                if (result.getCode() == 0) {
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

    /**
     * 获取云片网短信发送客户端
     *
     * @return YunpianClient
     */
    private static YunpianClient getYunpianClient() {
        if (yunpianClient == null) {
            synchronized (YunPianMsgSender.class) {
                if (yunpianClient == null) {
                    String yunpianApiKey = App.config.getYunpianApiKey();

                    yunpianClient = new YunpianClient(yunpianApiKey).init();
                }
            }
        }
        return yunpianClient;
    }


    private YunpianClient getYunpianClient(Integer accountId) {
        if (yunpianClientMap.containsKey(accountId)) {
            return yunpianClientMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            YunPianAccountConfig yunPianAccountConfig = JSON.parseObject(accountConfig, YunPianAccountConfig.class);

            String yunpianApiKey = yunPianAccountConfig.getApiKey();

            yunpianClient = new YunpianClient(yunpianApiKey).init();

            yunpianClientMap.put(accountId, yunpianClient);
            return yunpianClient;
        }

    }
}
