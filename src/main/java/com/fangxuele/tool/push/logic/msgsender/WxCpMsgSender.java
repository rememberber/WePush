package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.WxCpAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.WxCpMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceOkHttpImpl;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;
import me.chanjar.weixin.cp.bean.message.WxCpMessageSendResult;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.OkHttpClient;

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

    private static final Map<Integer, WxCpService> wxCpServiceMap = new ConcurrentHashMap<>();

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
        HttpClientRegistry.invalidate(MessageTypeEnum.WX_CP_CODE, account1Id);
        ProviderTrafficController.invalidate(MessageTypeEnum.WX_CP_CODE, account1Id);
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
        return wxCpServiceMap.computeIfAbsent(accountId, ignored -> {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            WxCpAccountConfig wxCpAccountConfig = JSON.parseObject(accountConfig, WxCpAccountConfig.class);

            WxCpDefaultConfigImpl configStorage = new WxCpDefaultConfigImpl();
            configStorage.setCorpId(wxCpAccountConfig.getCorpId());
            String agentId = wxCpAccountConfig.getAgentId();
            configStorage.setAgentId(Integer.valueOf(agentId));
            configStorage.setCorpSecret(wxCpAccountConfig.getSecret());

            if (wxCpAccountConfig.getPrivateDep()) {
                configStorage.setBaseApiUrl(wxCpAccountConfig.getBaseApiUrl());
            }

            OkHttpClient sharedClient = HttpClientRegistry.get(MessageTypeEnum.WX_CP_CODE, accountId);
            WxCpService wxCpService = new SharedClientWxCpService(sharedClient);
            wxCpService.setWxCpConfigStorage(configStorage);
            return wxCpService;
        });
    }

    /** 让 WxJava 的企业微信实现复用应用统一的 OkHttp 连接池和协议配置。 */
    private static final class SharedClientWxCpService extends WxCpServiceOkHttpImpl {
        private final OkHttpClient sharedClient;

        private SharedClientWxCpService(OkHttpClient sharedClient) {
            this.sharedClient = sharedClient;
        }

        @Override
        public OkHttpClient getRequestHttpClient() {
            return sharedClient;
        }

        @Override
        public void initHttp() {
            // 客户端由 HttpClientRegistry 管理，不允许 WxJava 另建连接池。
        }
    }
}
