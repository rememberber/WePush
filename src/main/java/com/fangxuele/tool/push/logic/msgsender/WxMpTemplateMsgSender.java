package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.msgmaker.WxMpTemplateMsgMaker;
import com.fangxuele.tool.push.ui.form.BoostForm;
import com.fangxuele.tool.push.util.ConsoleUtil;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder;
import me.chanjar.weixin.mp.api.WxMpInMemoryConfigStorage;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.Future;

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
    public volatile static WxMpInMemoryConfigStorage wxMpConfigStorage;
    public volatile static WxMpService wxMpService;
    public volatile static CloseableHttpAsyncClient closeableHttpAsyncClient;
    private WxMpTemplateMsgMaker wxMpTemplateMsgMaker;

    public WxMpTemplateMsgSender() {
        wxMpTemplateMsgMaker = new WxMpTemplateMsgMaker();
        wxMpService = getWxMpService();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            String openId = msgData[0];
            WxMpTemplateMessage wxMessageTemplate = wxMpTemplateMsgMaker.makeMsg(msgData);
            wxMessageTemplate.setToUser(openId);
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                wxMpService.getTemplateMsgService().sendTemplateMsg(wxMessageTemplate);
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

    @Override
    public SendResult asyncSend(String[] msgData) {
        SendResult sendResult = new SendResult();

        try {
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                String openId = msgData[0];
                WxMpTemplateMessage wxMessageTemplate = wxMpTemplateMsgMaker.makeMsg(msgData);
                wxMessageTemplate.setToUser(openId);

                String url = "https://api.weixin.qq.com/cgi-bin/message/template/send";
                // TODO
                Future<HttpResponse> httpResponseFuture = getCloseableHttpAsyncClient().execute(new HttpPost(url), new Back(msgData));
                if (!PushData.running) {
                    httpResponseFuture.cancel(true);
                }
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(e.toString());
            return sendResult;
        }

        return sendResult;
    }

    /**
     * 微信公众号配置
     *
     * @return WxMpConfigStorage
     */
    private static WxMpInMemoryConfigStorage wxMpConfigStorage() {
        WxMpInMemoryConfigStorage configStorage = new WxMpInMemoryConfigStorage();
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
                    wxMpService = new WxMpServiceImpl();
                    wxMpService.setWxMpConfigStorage(wxMpConfigStorage);
                }
            }
        }
        return wxMpService;
    }

    public static CloseableHttpAsyncClient getCloseableHttpAsyncClient() throws IOReactorException {
        if (closeableHttpAsyncClient == null) {
            synchronized (WxMpTemplateMsgSender.class) {
                if (closeableHttpAsyncClient == null) {
                    RequestConfig requestConfig = RequestConfig.custom()
                            .setConnectTimeout(500000)
                            .setSocketTimeout(500000)
                            .setConnectionRequestTimeout(500000)
                            .build();

                    //配置io线程
                    IOReactorConfig ioReactorConfig = IOReactorConfig.custom().
                            setIoThreadCount(Runtime.getRuntime().availableProcessors())
                            .setSoKeepAlive(true).setConnectTimeout(-1).setSoTimeout(-1)
                            .build();
                    //设置连接池大小
                    ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
                    PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor);
                    //最大连接数
                    connManager.setMaxTotal(5000);
                    //per route最大连接数
                    connManager.setDefaultMaxPerRoute(5000);

                    closeableHttpAsyncClient = HttpAsyncClients.custom().
                            setConnectionManager(connManager)
                            .setDefaultRequestConfig(requestConfig)
                            .build();

                    closeableHttpAsyncClient.start();
                }
            }
        }
        return closeableHttpAsyncClient;
    }

    static class Back implements FutureCallback<HttpResponse> {

        String[] msgData;

        Back(String[] msgData) {
            this.msgData = msgData;
        }

        @Override
        public void completed(HttpResponse httpResponse) {
            try {
                EntityUtils.toString(httpResponse.getEntity());
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 已成功+1
            PushData.increaseSuccess();
            BoostForm.boostForm.getSuccessCountLabel().setText(String.valueOf(PushData.successRecords));

            // 保存发送成功
            PushData.sendSuccessList.add(msgData);
            // 总进度条
            BoostForm.boostForm.getCompletedProgressBar().setValue(PushData.successRecords.intValue() + PushData.failRecords.intValue());
        }

        @Override
        public void failed(Exception e) {
            // 总发送失败+1
            PushData.increaseFail();
            BoostForm.boostForm.getFailCountLabel().setText(String.valueOf(PushData.failRecords));

            // 保存发送失败
            PushData.sendFailList.add(msgData);

            // 失败异常信息输出控制台
            ConsoleUtil.boostConsoleOnly("发送失败:" + e.toString() + ";msgData:" + JSONUtil.toJsonPrettyStr(msgData));
            // 总进度条
            BoostForm.boostForm.getCompletedProgressBar().setValue(PushData.successRecords.intValue() + PushData.failRecords.intValue());
        }

        @Override
        public void cancelled() {

        }
    }
}
