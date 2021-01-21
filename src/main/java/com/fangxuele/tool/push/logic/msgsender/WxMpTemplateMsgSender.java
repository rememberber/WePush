package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.logic.BoostPushRunThread;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.msgmaker.WxMpTemplateMsgMaker;
import com.fangxuele.tool.push.ui.form.BoostForm;
import com.fangxuele.tool.push.util.ConsoleUtil;
import com.fangxuele.tool.push.util.WeWxMpServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.util.http.apache.DefaultApacheHttpClientBuilder;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;

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
    public volatile static WxMpDefaultConfigImpl wxMpConfigStorage;
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
            log.error(ExceptionUtils.getStackTrace(e));
            return sendResult;
        }

        sendResult.setSuccess(true);
        return sendResult;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        SendResult sendResult = new SendResult();
        BoostForm boostForm = BoostForm.getInstance();

        try {
            if (PushControl.dryRun) {
                // 已成功+1
                PushData.increaseSuccess();
                boostForm.getSuccessCountLabel().setText(String.valueOf(PushData.successRecords));
                // 保存发送成功
                PushData.sendSuccessList.add(msgData);
                // 总进度条
                boostForm.getCompletedProgressBar().setValue(PushData.successRecords.intValue() + PushData.failRecords.intValue());
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                String openId = msgData[0];
                WxMpTemplateMessage wxMessageTemplate = wxMpTemplateMsgMaker.makeMsg(msgData);
                wxMessageTemplate.setToUser(openId);

                String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + wxMpService.getAccessToken();
                HttpPost httpPost = new HttpPost(url);
                StringEntity entity = new StringEntity(wxMessageTemplate.toJson(), Consts.UTF_8);
                httpPost.setEntity(entity);
                if (wxMpService.getRequestHttp().getRequestHttpProxy() != null) {
                    RequestConfig config = RequestConfig.custom().setProxy((HttpHost) wxMpService.getRequestHttp().getRequestHttpProxy()).build();
                    httpPost.setConfig(config);
                }
                Future<HttpResponse> httpResponseFuture = getCloseableHttpAsyncClient().execute(httpPost, new CallBack(msgData));
                BoostPushRunThread.futureList.add(httpResponseFuture);
            }
        } catch (Exception e) {
            // 总发送失败+1
            PushData.increaseFail();
            boostForm.getFailCountLabel().setText(String.valueOf(PushData.failRecords));

            // 保存发送失败
            PushData.sendFailList.add(msgData);

            // 失败异常信息输出控制台
            ConsoleUtil.boostConsoleOnly("发送失败:" + e.toString() + ";msgData:" + JSONUtil.toJsonPrettyStr(msgData));
            // 总进度条
            boostForm.getCompletedProgressBar().setValue(PushData.successRecords.intValue() + PushData.failRecords.intValue());

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

    public static CloseableHttpAsyncClient getCloseableHttpAsyncClient() throws IOReactorException {
        if (closeableHttpAsyncClient == null) {
            synchronized (WxMpTemplateMsgSender.class) {
                if (closeableHttpAsyncClient == null) {
                    RequestConfig requestConfig = RequestConfig.custom()
                            .setConnectTimeout(-1)
                            .setSocketTimeout(-1)
                            .setConnectionRequestTimeout(-1)
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

    static class CallBack implements FutureCallback<HttpResponse> {

        String[] msgData;

        CallBack(String[] msgData) {
            this.msgData = msgData;
        }

        @Override
        public void completed(HttpResponse httpResponse) {
            BoostForm boostForm = BoostForm.getInstance();

            try {
                String response = EntityUtils.toString(httpResponse.getEntity(), Consts.UTF_8);
                if (response.isEmpty()) {
                    // 总发送失败+1
                    PushData.increaseFail();
                    boostForm.getFailCountLabel().setText(String.valueOf(PushData.failRecords));

                    // 保存发送失败
                    PushData.sendFailList.add(msgData);

                    // 失败异常信息输出控制台
                    ConsoleUtil.boostConsoleOnly("发送失败:" + WxError.builder().errorCode(9999).errorMsg("无响应内容").build() + ";msgData:" + JSONUtil.toJsonPrettyStr(msgData));
                    // 总进度条
                    boostForm.getCompletedProgressBar().setValue(PushData.successRecords.intValue() + PushData.failRecords.intValue());
                } else {
                    WxError error = WxError.fromJson(response);
                    if (error.getErrorCode() != 0) {
                        // 总发送失败+1
                        PushData.increaseFail();
                        boostForm.getFailCountLabel().setText(String.valueOf(PushData.failRecords));

                        // 保存发送失败
                        PushData.sendFailList.add(msgData);

                        // 失败异常信息输出控制台
                        ConsoleUtil.boostConsoleOnly("发送失败:" + error + ";msgData:" + JSONUtil.toJsonPrettyStr(msgData));
                        // 总进度条
                        boostForm.getCompletedProgressBar().setValue(PushData.successRecords.intValue() + PushData.failRecords.intValue());
                    } else {
                        // 已成功+1
                        PushData.increaseSuccess();
                        boostForm.getSuccessCountLabel().setText(String.valueOf(PushData.successRecords));

                        // 保存发送成功
                        PushData.sendSuccessList.add(msgData);
                        // 总进度条
                        boostForm.getCompletedProgressBar().setValue(PushData.successRecords.intValue() + PushData.failRecords.intValue());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void failed(Exception e) {
            BoostForm boostForm = BoostForm.getInstance();

            // 总发送失败+1
            PushData.increaseFail();
            boostForm.getFailCountLabel().setText(String.valueOf(PushData.failRecords));

            // 保存发送失败
            PushData.sendFailList.add(msgData);

            // 失败异常信息输出控制台
            ConsoleUtil.boostConsoleOnly("发送失败:" + e.toString() + ";msgData:" + JSONUtil.toJsonPrettyStr(msgData));
            // 总进度条
            boostForm.getCompletedProgressBar().setValue(PushData.successRecords.intValue() + PushData.failRecords.intValue());
        }

        @Override
        public void cancelled() {
            PushData.TO_SEND_COUNT.getAndDecrement();
        }
    }
}
