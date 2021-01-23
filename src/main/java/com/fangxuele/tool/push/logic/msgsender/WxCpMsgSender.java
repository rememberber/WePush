package com.fangxuele.tool.push.logic.msgsender;

import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TWxCpAppMapper;
import com.fangxuele.tool.push.domain.TWxCpApp;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.WxCpMsgMaker;
import com.fangxuele.tool.push.ui.form.msg.WxCpMsgForm;
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

import java.util.List;

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
    public volatile static WxCpDefaultConfigImpl wxCpConfigStorage;
    public volatile static WxCpService wxCpService;
    private final WxCpMsgMaker wxCpMsgMaker;

    private final static TWxCpAppMapper WX_CP_APP_MAPPER = MybatisUtil.getSqlSession().getMapper(TWxCpAppMapper.class);

    public WxCpMsgSender() {
        wxCpMsgMaker = new WxCpMsgMaker();
        wxCpService = getWxCpService();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxCpMessage wxCpMessage = wxCpMsgMaker.makeMsg(msgData);
            wxCpMessage.setToUser(openId);
            if (PushControl.dryRun) {
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

    /**
     * 微信企业号配置
     *
     * @return WxCpConfigStorage
     */
    private static WxCpDefaultConfigImpl wxCpConfigStorage() {
        WxCpDefaultConfigImpl configStorage = new WxCpDefaultConfigImpl();
        configStorage.setCorpId(App.config.getWxCpCorpId());
        String agentId = WxCpMsgForm.appNameToAgentIdMap.get(WxCpMsgForm.getInstance().getAppNameComboBox().getSelectedItem());
        configStorage.setAgentId(Integer.valueOf(agentId));

        List<TWxCpApp> wxCpAppList = WX_CP_APP_MAPPER.selectByAgentId(agentId);
        if (wxCpAppList.size() > 0) {
            configStorage.setCorpSecret(wxCpAppList.get(0).getSecret());
        }
        if (App.config.isMpUseProxy()) {
            configStorage.setHttpProxyHost(App.config.getMpProxyHost());
            configStorage.setHttpProxyPort(Integer.parseInt(App.config.getMpProxyPort()));
            configStorage.setHttpProxyUsername(App.config.getMpProxyUserName());
            configStorage.setHttpProxyPassword(App.config.getMpProxyPassword());
        }
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
        return configStorage;
    }

    /**
     * 获取微信企业号工具服务
     *
     * @return WxCpService
     */
    public static WxCpService getWxCpService() {
        if (wxCpConfigStorage == null) {
            synchronized (WxCpMsgSender.class) {
                if (wxCpConfigStorage == null) {
                    wxCpConfigStorage = wxCpConfigStorage();
                }
            }
        }
        if (wxCpService == null && wxCpConfigStorage != null) {
            synchronized (PushControl.class) {
                if (wxCpService == null && wxCpConfigStorage != null) {
                    wxCpService = new WxCpServiceApacheHttpClientImpl();
                    wxCpService.setWxCpConfigStorage(wxCpConfigStorage);
                }
            }
        }
        return wxCpService;
    }
}
