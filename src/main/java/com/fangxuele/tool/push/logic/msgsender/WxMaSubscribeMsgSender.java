package com.fangxuele.tool.push.logic.msgsender;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceOkHttpImpl;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.WxMaAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgmaker.WxMaSubscribeMsgMaker;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final Map<Integer, WxMaService> wxMaServiceMap = new ConcurrentHashMap<>();

    public WxMaSubscribeMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        wxMaSubscribeMsgMaker = new WxMaSubscribeMsgMaker(tMsg);
        wxMaService = getWxMaService(tMsg.getAccountId());
        this.dryRun = dryRun;
    }

    public static void removeAccount(Integer accountId) {
        wxMaServiceMap.remove(accountId);
        HttpClientRegistry.invalidateAccount(accountId);
        ProviderTrafficController.invalidateAccount(accountId);
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
        return wxMaServiceMap.computeIfAbsent(accountId, ignored -> {
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
            HttpClientRegistry.ClientOptions options = HttpClientRegistry.ClientOptions.defaults();
            if (wxMaAccountConfig.isMaUseProxy()) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                        wxMaAccountConfig.getMaProxyHost(), Integer.parseInt(wxMaAccountConfig.getMaProxyPort())));
                options = options.withProxy(proxy, wxMaAccountConfig.getMaProxyUserName(),
                        wxMaAccountConfig.getMaProxyPassword());
            }
            OkHttpClient sharedClient = HttpClientRegistry.get(MessageTypeEnum.MA_SUBSCRIBE_CODE, accountId, options);
            WxMaService wxMaService = new SharedClientWxMaService(sharedClient);
            wxMaService.setWxMaConfig(configStorage);
            return wxMaService;
        });
    }

    private static final class SharedClientWxMaService extends WxMaServiceOkHttpImpl {
        private final OkHttpClient sharedClient;

        private SharedClientWxMaService(OkHttpClient sharedClient) {
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
