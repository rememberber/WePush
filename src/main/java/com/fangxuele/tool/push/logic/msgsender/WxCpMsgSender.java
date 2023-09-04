package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.WxCpAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.WxCpMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceApacheHttpClientImpl;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;
import me.chanjar.weixin.cp.bean.message.WxCpMessageSendResult;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 微信企业号模板消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/29.
 */
@Slf4j
public class WxCpMsgSender implements IMsgSender {
    private WxCpService wxCpService;
    private final WxCpMsgMaker wxCpMsgMaker;

    private static Map<Integer, WxCpService> wxCpServiceMap = new HashMap<>();

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    public WxCpMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        wxCpMsgMaker = new WxCpMsgMaker(tMsg);
        wxCpService = getWxCpService(tMsg.getAccountId());
        this.dryRun = dryRun;
    }

    public static void removeAccount(Integer account1Id) {
        wxCpServiceMap.remove(account1Id);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxCpMessage wxCpMessage = wxCpMsgMaker.makeMsg(msgData);
            wxCpMessage.setToUser(openId);
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                WxCpMessageSendResult wxCpMessageSendResult = wxCpService.getMessageService().send(wxCpMessage);
                if (wxCpMessageSendResult.getErrCode() != 0 || StringUtils.isNoneEmpty(wxCpMessageSendResult.getInvalidUser())) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(wxCpMessageSendResult.toString());
                    log.error(wxCpMessageSendResult.toString());
                    return sendResult;
                }
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
            return sendResult;
        }

        sendResult.setSuccess(true);
        return sendResult;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }

    public static WxCpService getWxCpService(Integer accountId) {
        if (wxCpServiceMap.containsKey(accountId)) {
            return wxCpServiceMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            WxCpAccountConfig wxCpAccountConfig = JSON.parseObject(accountConfig, WxCpAccountConfig.class);

            WxCpDefaultConfigImpl configStorage = new WxCpDefaultConfigImpl();
            configStorage.setCorpId(wxCpAccountConfig.getCorpId());
            String agentId = wxCpAccountConfig.getAgentId();
            configStorage.setAgentId(Integer.valueOf(agentId));
            configStorage.setCorpSecret(wxCpAccountConfig.getSecret());
            DefaultApacheHttpClientBuilder clientBuilder = DefaultApacheHttpClientBuilder.get();
            //从连接池获取链接的超时时间(单位ms)
            clientBuilder.setConnectionRequestTimeout(10000);
            //建立链接的超时时间(单位ms)
            clientBuilder.setConnectionTimeout(5000);
            //连接池socket超时时间(单位ms)
            clientBuilder.setSoTimeout(5000);
            //空闲链接的超时时间(单位ms)
            clientBuilder.setIdleConnTimeout(60000);
            //空闲链接的检测周期(单位ms)
            clientBuilder.setCheckWaitTime(60000);
            //每路最大连接数
            clientBuilder.setMaxConnPerHost(App.config.getMaxThreads());
            //连接池最大连接数
            clientBuilder.setMaxTotalConn(App.config.getMaxThreads());
            //HttpClient请求时使用的User Agent
//        clientBuilder.setUserAgent(..)
            configStorage.setApacheHttpClientBuilder(clientBuilder);

            if (wxCpAccountConfig.getPrivateDep()) {
                configStorage.setBaseApiUrl(wxCpAccountConfig.getBaseApiUrl());
            }

            WxCpService wxCpService = new WxCpServiceApacheHttpClientImpl();
            wxCpService.setWxCpConfigStorage(configStorage);

            wxCpServiceMap.put(accountId, wxCpService);
            return wxCpService;
        }

    }
}
