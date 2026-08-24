package com.fangxuele.tool.push.logic.msgsender;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.WxMpAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.WxMpTemplateMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.fangxuele.tool.push.util.WeWxMpServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private WxMpService wxMpService;
    private WxMpTemplateMsgMaker wxMpTemplateMsgMaker;

    private static final Map<Integer, WxMpService> wxMpServiceMap = new ConcurrentHashMap<>();

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

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
        return wxMpServiceMap.computeIfAbsent(accountId, ignored -> {
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
            HttpClientRegistry.ClientOptions options = HttpClientRegistry.ClientOptions.defaults();
            if (wxMpAccountConfig.isMpUseProxy()) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                        wxMpAccountConfig.getMpProxyHost(), Integer.parseInt(wxMpAccountConfig.getMpProxyPort())));
                options = options.withProxy(proxy, wxMpAccountConfig.getMpProxyUserName(),
                        wxMpAccountConfig.getMpProxyPassword());
            }
            WxMpService wxMpService = new WeWxMpServiceImpl(wxMpAccountConfig,
                    HttpClientRegistry.get(MessageTypeEnum.MP_TEMPLATE_CODE, accountId, options));
            wxMpService.setWxMpConfigStorage(wxMpConfigStorage);
            return wxMpService;
        });

    }

    public static void removeAccount(Integer accountId) {
        wxMpServiceMap.remove(accountId);
        HttpClientRegistry.invalidateAccount(accountId);
        ProviderTrafficController.invalidateAccount(accountId);
        TemplateUtil.clearNickNameCache();
    }

}
