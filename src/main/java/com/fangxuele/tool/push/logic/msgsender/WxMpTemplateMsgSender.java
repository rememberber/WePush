package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.WxMpAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.WxMpTemplateMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.WeWxMpServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 微信公众号模板消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
@Slf4j
public class WxMpTemplateMsgSender implements IMsgSender {
    public volatile static WxMpDefaultConfigImpl wxMpConfigStorage;
    public volatile static WxMpService wxMpService;
    public volatile static CloseableHttpAsyncClient closeableHttpAsyncClient;
    private WxMpTemplateMsgMaker wxMpTemplateMsgMaker;

    private static Map<Integer, WxMpService> wxMpServiceMap = new HashMap<>();

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    public WxMpTemplateMsgSender() {
    }

    public WxMpTemplateMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        wxMpTemplateMsgMaker = new WxMpTemplateMsgMaker(tMsg);
        wxMpService = getWxMpService(tMsg.getAccountId());
        this.dryRun = dryRun;
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMpTemplateMessage wxMessageTemplate = wxMpTemplateMsgMaker.makeMsg(msgData);
            wxMessageTemplate.setToUser(openId);

            // TODO 上线前删除
            Thread.sleep(10);
            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                wxMpService.getTemplateMsgService().sendTemplateMsg(wxMessageTemplate);
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
     * 微信公众号配置
     *
     * @return WxMpConfigStorage
     */
    private static WxMpDefaultConfigImpl wxMpConfigStorage() {
        WxMpDefaultConfigImpl configStorage = new WxMpDefaultConfigImpl();
        configStorage.setAppId(App.config.getWechatAppId());
        configStorage.setSecret(App.config.getWechatAppSecret());
        configStorage.setToken(App.config.getWechatToken());
        configStorage.setAesKey(App.config.getWechatAesKey());
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
        clientBuilder.setCheckWaitTime(3000);
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
     * 获取微信公众号工具服务
     *
     * @return WxMpService
     */
    public static WxMpService getWxMpService() {
        if (wxMpConfigStorage == null) {
            synchronized (WxMpTemplateMsgSender.class) {
                if (wxMpConfigStorage == null) {
                    wxMpConfigStorage = wxMpConfigStorage();
                }
            }
        }
        if (wxMpService == null && wxMpConfigStorage != null) {
            synchronized (WxMpTemplateMsgSender.class) {
                if (wxMpService == null && wxMpConfigStorage != null) {
                    wxMpService = new WeWxMpServiceImpl();
                    wxMpService.setWxMpConfigStorage(wxMpConfigStorage);
                }
            }
        }
        return wxMpService;
    }

    public static WxMpService getWxMpService(Integer accountId) {
        if (wxMpServiceMap.containsKey(accountId)) {
            return wxMpServiceMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            WxMpAccountConfig wxMpAccountConfig = JSON.parseObject(accountConfig, WxMpAccountConfig.class);

            WxMpDefaultConfigImpl wxMpConfigStorage = wxMpConfigStorage();
            wxMpConfigStorage.setAppId(wxMpAccountConfig.getAppId());
            wxMpConfigStorage.setSecret(wxMpAccountConfig.getAppSecret());
            wxMpConfigStorage.setToken(wxMpAccountConfig.getToken());
            wxMpConfigStorage.setAesKey(wxMpAccountConfig.getAesKey());
            if (App.config.isMpUseProxy()) {
                wxMpConfigStorage.setHttpProxyHost(App.config.getMpProxyHost());
                wxMpConfigStorage.setHttpProxyPort(Integer.parseInt(App.config.getMpProxyPort()));
                wxMpConfigStorage.setHttpProxyUsername(App.config.getMpProxyUserName());
                wxMpConfigStorage.setHttpProxyPassword(App.config.getMpProxyPassword());
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
            clientBuilder.setCheckWaitTime(3000);
            //每路最大连接数
            clientBuilder.setMaxConnPerHost(App.config.getMaxThreads());
            //连接池最大连接数
            clientBuilder.setMaxTotalConn(App.config.getMaxThreads());
            //HttpClient请求时使用的User Agent
//        clientBuilder.setUserAgent(..)
            wxMpConfigStorage.setApacheHttpClientBuilder(clientBuilder);
            WxMpService wxMpService = new WeWxMpServiceImpl();
            wxMpService.setWxMpConfigStorage(wxMpConfigStorage);
            wxMpServiceMap.put(accountId, wxMpService);
            return wxMpService;
        }

    }

    public static void removeAccount(Integer accountId) {
        wxMpServiceMap.remove(accountId);
    }

}
