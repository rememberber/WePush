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
    public volatile WxMpService wxMpService;
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
     * 获取微信公众号工具服务
     *
     * @return WxMpService
     */
    public static WxMpService getWxMpService() {
        return null;
    }

    public static WxMpService getWxMpService(Integer accountId) {
        if (wxMpServiceMap.containsKey(accountId)) {
            return wxMpServiceMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            WxMpAccountConfig wxMpAccountConfig = JSON.parseObject(accountConfig, WxMpAccountConfig.class);

            WxMpDefaultConfigImpl wxMpConfigStorage = new WxMpDefaultConfigImpl();
            wxMpConfigStorage.setAppId(wxMpAccountConfig.getAppId());
            wxMpConfigStorage.setSecret(wxMpAccountConfig.getAppSecret());
            wxMpConfigStorage.setToken(wxMpAccountConfig.getToken());
            wxMpConfigStorage.setAesKey(wxMpAccountConfig.getAesKey());
            if (wxMpAccountConfig.isMpUseProxy()) {
                wxMpConfigStorage.setHttpProxyHost(wxMpAccountConfig.getMpProxyHost());
                wxMpConfigStorage.setHttpProxyPort(Integer.parseInt(wxMpAccountConfig.getMpProxyPort()));
                wxMpConfigStorage.setHttpProxyUsername(wxMpAccountConfig.getMpProxyUserName());
                wxMpConfigStorage.setHttpProxyPassword(wxMpAccountConfig.getMpProxyPassword());
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
            WxMpService wxMpService = new WeWxMpServiceImpl(wxMpAccountConfig);
            wxMpService.setWxMpConfigStorage(wxMpConfigStorage);
            wxMpServiceMap.put(accountId, wxMpService);
            return wxMpService;
        }

    }

    public static void removeAccount(Integer accountId) {
        wxMpServiceMap.remove(accountId);
    }

}
