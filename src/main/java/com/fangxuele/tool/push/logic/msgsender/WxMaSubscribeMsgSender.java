package com.fangxuele.tool.push.logic.msgsender;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.WxMaSubscribeMsgMaker;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * <pre>
 * 微信小程序订阅消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/11/29.
 */
@Slf4j
public class WxMaSubscribeMsgSender implements IMsgSender {

    public volatile static WxMaService wxMaService;

    public volatile static WxMaDefaultConfigImpl wxMaConfigStorage;

    private WxMaSubscribeMsgMaker wxMaSubscribeMsgMaker;

    public WxMaSubscribeMsgSender() {
        wxMaSubscribeMsgMaker = new WxMaSubscribeMsgMaker();
        wxMaService = getWxMaService();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMaSubscribeMessage wxMaSubscribeMessage = wxMaSubscribeMsgMaker.makeMsg(msgData);
            wxMaSubscribeMessage.setToUser(openId);
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                wxMaService.getMsgService().sendSubscribeMsg(wxMaSubscribeMessage);
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
     * 微信小程序配置
     *
     * @return WxMaInMemoryConfig
     */
    private static WxMaDefaultConfigImpl wxMaConfigStorage() {
        WxMaDefaultConfigImpl configStorage = new WxMaDefaultConfigImpl();
        configStorage.setAppid(App.config.getMiniAppAppId());
        configStorage.setSecret(App.config.getMiniAppAppSecret());
        configStorage.setToken(App.config.getMiniAppToken());
        configStorage.setAesKey(App.config.getMiniAppAesKey());
        configStorage.setMsgDataFormat("JSON");
        if (App.config.isMaUseProxy()) {
            configStorage.setHttpProxyHost(App.config.getMaProxyHost());
            configStorage.setHttpProxyPort(Integer.parseInt(App.config.getMaProxyPort()));
            configStorage.setHttpProxyUsername(App.config.getMaProxyUserName());
            configStorage.setHttpProxyPassword(App.config.getMaProxyPassword());
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
     * 获取微信小程序工具服务
     *
     * @return WxMaService
     */
    static WxMaService getWxMaService() {
        if (wxMaService == null) {
            synchronized (WxMaSubscribeMsgSender.class) {
                if (wxMaService == null) {
                    wxMaService = new WxMaServiceImpl();
                }
            }
        }
        if (wxMaConfigStorage == null) {
            synchronized (WxMaSubscribeMsgSender.class) {
                if (wxMaConfigStorage == null) {
                    wxMaConfigStorage = wxMaConfigStorage();
                    if (wxMaConfigStorage != null) {
                        wxMaService.setWxMaConfig(wxMaConfigStorage);
                    }
                }
            }
        }
        return wxMaService;
    }
}
