package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.HttpAccountConfig;
import com.fangxuele.tool.push.bean.msg.HttpMsg;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.HttpMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.HttpClientRegistry;
import com.fangxuele.tool.push.util.OkHttpRequestUtil;
import com.fangxuele.tool.push.util.ProxyUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Map;
import java.time.Duration;

/**
 * <pre>
 * Http消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/7/16.
 */
@Slf4j
public class HttpMsgSender implements IMsgSender {

    private final HttpMsgMaker httpMsgMaker;

    private final OkHttpClient okHttpClient;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private final Integer dryRun;

    private final HttpAccountConfig httpAccountConfig;

    public HttpMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        httpMsgMaker = new HttpMsgMaker(tMsg);
        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        httpAccountConfig = JSON.parseObject(accountConfig, HttpAccountConfig.class);
        HttpClientRegistry.ClientOptions options = HttpClientRegistry.ClientOptions.defaults()
                .withTimeouts(Duration.ofMinutes(3), Duration.ofMinutes(3), Duration.ofMinutes(3), Duration.ofMinutes(5));
        if (httpAccountConfig.isUseProxy()) {
            Proxy proxy = new Proxy(ProxyUtil.getProxyType(httpAccountConfig.getProxyType()),
                    new InetSocketAddress(httpAccountConfig.getProxyHost(), Integer.parseInt(httpAccountConfig.getProxyPort())));
            options = options.withProxy(proxy, httpAccountConfig.getProxyUserName(), httpAccountConfig.getProxyPassword());
        }
        okHttpClient = HttpClientRegistry.get(tMsg.getMsgType(), tMsg.getAccountId(), options);
        this.dryRun = dryRun;
    }

    public static void removeAccount(Integer tAccount1Id) {
        HttpClientRegistry.invalidate(com.fangxuele.tool.push.logic.MessageTypeEnum.HTTP_CODE, tAccount1Id);
        ProviderTrafficController.invalidate(com.fangxuele.tool.push.logic.MessageTypeEnum.HTTP_CODE, tAccount1Id);
    }

    @Override
    public HttpSendResult send(String[] msgData) {
        return sendUseOkHttp(msgData);
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }

    public HttpSendResult sendUseOkHttp(String[] msgData) {
        HttpSendResult sendResult = new HttpSendResult();
        try {
            HttpMsg httpMsg = httpMsgMaker.makeMsg(msgData);

            Request.Builder requestBuilder = new Request.Builder();

            RequestBody requestBody = RequestBody.create("", MediaType.get("text/plain"));
            if (!"GET".equals(httpMsgMaker.getMethod()) && httpMsg.getParamMap() != null && !httpMsg.getParamMap().isEmpty()) {
                FormBody.Builder formBodyBuilder = new FormBody.Builder();
                for (Map.Entry<String, Object> paramEntry : httpMsg.getParamMap().entrySet()) {
                    formBodyBuilder.add(paramEntry.getKey(), (String) paramEntry.getValue());
                }
                requestBody = formBodyBuilder.build();
            } else if (!"GET".equals(httpMsgMaker.getMethod()) && StringUtils.isNotEmpty(httpMsg.getBody())) {
                String bodyType = httpMsgMaker.getBodyType();
                MediaType mediaType = MediaType.get(bodyType + "; charset=utf-8");
                requestBody = RequestBody.create(httpMsg.getBody(), mediaType);
            }

            if (httpMsg.getHeaderMap() != null && !httpMsg.getHeaderMap().isEmpty()) {
                for (Map.Entry<String, Object> headerEntry : httpMsg.getHeaderMap().entrySet()) {
                    requestBuilder.addHeader(headerEntry.getKey(), (String) headerEntry.getValue());
                }
            }
            if (httpMsg.getCookies() != null && !httpMsg.getCookies().isEmpty()) {
                requestBuilder.addHeader(Header.COOKIE.toString(), cookieHeader(httpMsg.getCookies()));
            }
            switch (httpMsgMaker.getMethod()) {
                case "GET":
                    HttpUrl.Builder urlBuilder = HttpUrl.parse(httpMsg.getUrl()).newBuilder();
                    if (httpMsg.getParamMap() != null && !httpMsg.getParamMap().isEmpty()) {
                        for (Map.Entry<String, Object> paramEntry : httpMsg.getParamMap().entrySet()) {
                            urlBuilder.addQueryParameter(paramEntry.getKey(), (String) paramEntry.getValue());
                        }
                    }
                    requestBuilder.url(urlBuilder.build()).get();
                    break;
                case "POST":
                    requestBuilder.url(httpMsg.getUrl()).post(requestBody);
                    break;
                case "PUT":
                    requestBuilder.url(httpMsg.getUrl()).put(requestBody);
                    break;
                case "PATCH":
                    requestBuilder.url(httpMsg.getUrl()).patch(requestBody);
                    break;
                case "DELETE":
                    requestBuilder.url(httpMsg.getUrl()).delete(requestBody);
                    break;
                case "HEAD":
                    requestBuilder.url(httpMsg.getUrl()).head();
                    break;
                case "OPTIONS":
                    requestBuilder.url(httpMsg.getUrl()).method("OPTIONS", requestBody);
                    break;
                default:
                    requestBuilder.url(httpMsg.getUrl());
            }

            Request request = requestBuilder.build();

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                OkHttpRequestUtil.ResponseData response = OkHttpRequestUtil.execute(okHttpClient, request);
                sendResult.setHttpStatus(response.statusCode());
                sendResult.setRetryAfterMillis(response.retryAfterMillis());
                if (!response.isSuccessful()) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(response.body());
                    return sendResult;
                }

                String responseBody = response.body();
                sendResult.setInfo(responseBody);
                sendResult.setBody(responseBody);

                sendResult.setHeaders(response.headers().toString());

                StringBuilder cookiesBuilder = StrUtil.builder();
                List<String> headerList = response.headers().values(Header.SET_COOKIE.toString());
                for (String cookieStr : headerList) {
                    cookiesBuilder.append(cookieStr).append(StrUtil.CRLF);
                }

                sendResult.setCookies(cookiesBuilder.toString());

                sendResult.setSuccess(true);
                return sendResult;
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
            return sendResult;
        }
    }

    private String cookieHeader(List<HttpCookie> cookies) {
        StringBuilder cookieHeader = new StringBuilder();
        for (int i = 0, size = cookies.size(); i < size; i++) {
            if (i > 0) {
                cookieHeader.append("; ");
            }
            HttpCookie cookie = cookies.get(i);
            cookieHeader.append(cookie.getName()).append('=').append(cookie.getValue());
        }
        return cookieHeader.toString();
    }

}
