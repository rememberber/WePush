package com.fangxuele.tool.push.logic.msgsender;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.bean.WxMaTemplateMessage;
import cn.binarywang.wx.miniapp.config.WxMaInMemoryConfig;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.WxMaTemplateMsgMaker;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder;

/**
 * <pre>
 * 微信小程序模板消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class WxMaTemplateMsgSender implements IMsgSender {
    public volatile static WxMaService wxMaService;

    public volatile static WxMaInMemoryConfig wxMaConfigStorage;

    private WxMaTemplateMsgMaker wxMaTemplateMsgMaker;

    public WxMaTemplateMsgSender() {
        wxMaTemplateMsgMaker = new WxMaTemplateMsgMaker();
        wxMaService = getWxMaService();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMaTemplateMessage wxMaTemplateMessage = wxMaTemplateMsgMaker.makeMsg(msgData);
            wxMaTemplateMessage.setToUser(openId);
            wxMaTemplateMessage.setFormId(msgData[1]);
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                wxMaService.getMsgService().sendTemplateMsg(wxMaTemplateMessage);
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(e.toString());
            return sendResult;
        }

        sendResult.setSuccess(true);
        return sendResult;
    }

    /**
     * 微信小程序配置
     *
     * @return WxMaInMemoryConfig
     */
    private static WxMaInMemoryConfig wxMaConfigStorage() {
        WxMaInMemoryConfig configStorage = new WxMaInMemoryConfig();
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
        clientBuilder.setMaxConnPerHost(App.config.getMaxThreadPool() * 2);
        //连接池最大连接数
        clientBuilder.setMaxTotalConn(App.config.getMaxThreadPool() * 2);
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
            synchronized (WxMaTemplateMsgSender.class) {
                if (wxMaService == null) {
                    wxMaService = new WxMaServiceImpl();
                }
            }
        }
        if (wxMaConfigStorage == null) {
            synchronized (WxMaTemplateMsgSender.class) {
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
