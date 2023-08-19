package com.fangxuele.tool.push.logic.msgsender;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.WxMaAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.WxMaSubscribeMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;

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

    private WxMaService wxMaService;

    private WxMaSubscribeMsgMaker wxMaSubscribeMsgMaker;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private static Map<Integer, WxMaService> wxMaServiceMap = new HashMap<>();

    public WxMaSubscribeMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        wxMaSubscribeMsgMaker = new WxMaSubscribeMsgMaker(tMsg);
        wxMaService = getWxMaService(tMsg.getAccountId());
        this.dryRun = dryRun;
    }

    public static void removeAccount(Integer accountId) {
        wxMaServiceMap.remove(accountId);
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMaSubscribeMessage wxMaSubscribeMessage = wxMaSubscribeMsgMaker.makeMsg(msgData);
            wxMaSubscribeMessage.setToUser(openId);
            if (dryRun == 1) {
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

    public static WxMaService getWxMaService(Integer accountId) {
        if (wxMaServiceMap.containsKey(accountId)) {
            return wxMaServiceMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            WxMaAccountConfig wxMaAccountConfig = JSON.parseObject(accountConfig, WxMaAccountConfig.class);

            WxMaDefaultConfigImpl configStorage = new WxMaDefaultConfigImpl();
            configStorage.setAppid(wxMaAccountConfig.getAppId());
            configStorage.setSecret(wxMaAccountConfig.getAppSecret());
            configStorage.setToken(wxMaAccountConfig.getToken());
            configStorage.setAesKey(wxMaAccountConfig.getAesKey());
            configStorage.setMsgDataFormat("JSON");
            if (wxMaAccountConfig.isMaUseProxy()) {
                configStorage.setHttpProxyHost(wxMaAccountConfig.getMaProxyHost());
                configStorage.setHttpProxyPort(Integer.parseInt(wxMaAccountConfig.getMaProxyPort()));
                configStorage.setHttpProxyUsername(wxMaAccountConfig.getMaProxyUserName());
                configStorage.setHttpProxyPassword(wxMaAccountConfig.getMaProxyPassword());
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

            WxMaService wxMaService = new WxMaServiceImpl();
            wxMaService.setWxMaConfig(configStorage);

            wxMaServiceMap.put(accountId, wxMaService);
            return wxMaService;
        }
    }
}
