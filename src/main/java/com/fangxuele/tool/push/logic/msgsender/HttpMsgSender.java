package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.HttpAccountConfig;
import com.fangxuele.tool.push.bean.msg.HttpMsg;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.logic.msgmaker.HttpMsgMaker;
import com.fangxuele.tool.push.util.MybatisUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    private HttpMsgMaker httpMsgMaker;

    private static OkHttpClient okHttpClient;
    private static Map<Integer, OkHttpClient> okHttpClientMap = new HashMap<>();

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    private Integer dryRun;

    private HttpAccountConfig httpAccountConfig;

    public HttpMsgSender(Integer msgId, Integer dryRun) {
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        httpMsgMaker = new HttpMsgMaker(tMsg);
        okHttpClient = getOkHttpClient(tMsg.getAccountId());
        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        httpAccountConfig = JSON.parseObject(accountConfig, HttpAccountConfig.class);
        this.dryRun = dryRun;
    }

    public static void removeAccount(Integer tAccount1Id) {
        okHttpClientMap.remove(tAccount1Id);
    }

    @Override
    public HttpSendResult send(String[] msgData) {
        return sendUseOkHttp(msgData);
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }

    public HttpSendResult sendUseHutool(String[] msgData) {
        HttpSendResult sendResult = new HttpSendResult();
        HttpResponse httpResponse;
        try {
            HttpMsg httpMsg = httpMsgMaker.makeMsg(msgData);
            HttpRequest httpRequest;
            switch (httpMsgMaker.getMethod()) {
                case "GET":
                    httpRequest = HttpRequest.get(httpMsg.getUrl());
                    break;
                case "POST":
                    httpRequest = HttpRequest.post(httpMsg.getUrl());
                    break;
                case "PUT":
                    httpRequest = HttpRequest.put(httpMsg.getUrl());
                    break;
                case "PATCH":
                    httpRequest = HttpRequest.patch(httpMsg.getUrl());
                    break;
                case "DELETE":
                    httpRequest = HttpRequest.delete(httpMsg.getUrl());
                    break;
                case "HEAD":
                    httpRequest = HttpRequest.head(httpMsg.getUrl());
                    break;
                case "OPTIONS":
                    httpRequest = HttpRequest.options(httpMsg.getUrl());
                    break;
                default:
                    httpRequest = HttpRequest.get(httpMsg.getUrl()).form(httpMsg.getParamMap());
            }
            if (httpMsg.getParamMap() != null && !httpMsg.getParamMap().isEmpty()) {
                httpRequest.form(httpMsg.getParamMap());
            }
            if (httpMsg.getHeaderMap() != null && !httpMsg.getHeaderMap().isEmpty()) {
                for (Map.Entry<String, Object> entry : httpMsg.getHeaderMap().entrySet()) {
                    httpRequest.header(entry.getKey(), (String) entry.getValue());
                }
            }
            if (httpMsg.getCookies() != null && !httpMsg.getCookies().isEmpty()) {
                HttpCookie[] cookies = ArrayUtil.toArray(httpMsg.getCookies(), HttpCookie.class);
                httpRequest.cookie(cookies);
            }
            if (StringUtils.isNotEmpty(httpMsg.getBody())) {
                httpRequest.body(httpMsg.getBody());
            }
            if (httpAccountConfig.isUseProxy()) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpAccountConfig.getProxyHost(), Integer.parseInt(httpAccountConfig.getProxyPort())));
                httpRequest.setProxy(proxy);
            }

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                httpResponse = httpRequest.execute();
                if (!httpResponse.isOk()) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(httpResponse.toString());
                    return sendResult;
                }
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.toString());
            log.error(ExceptionUtils.getStackTrace(e));
            return sendResult;
        }
        StringBuilder headerBuilder = StrUtil.builder();
        for (Map.Entry<String, List<String>> entry : httpResponse.headers().entrySet()) {
            headerBuilder.append(entry).append(StrUtil.CRLF);
        }
        sendResult.setHeaders(headerBuilder.toString());

        String body = httpResponse.body();
        sendResult.setInfo(body);
        if (body != null && body.startsWith("{") && body.endsWith("}")) {
            try {
                body = JSONUtil.toJsonPrettyStr(body);
            } catch (Exception e) {
                log.error(e.toString());
            }
        }
        sendResult.setBody(body);

        StringBuilder cookiesBuilder = StrUtil.builder();
        List<String> headerList = httpResponse.headerList(Header.SET_COOKIE.toString());
        if (headerList != null) {
            for (String cookieStr : headerList) {
                cookiesBuilder.append(cookieStr).append(StrUtil.CRLF);
            }
        }

        sendResult.setCookies(cookiesBuilder.toString());

        sendResult.setSuccess(true);
        return sendResult;
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
                    return sendUseHutool(msgData);
                default:
                    requestBuilder.url(httpMsg.getUrl());
            }

            Request request = requestBuilder.build();

            if (dryRun == 1) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                Response response = okHttpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(response.toString());
                    return sendResult;
                }

                String responseBody = "";
                if (response.body() != null) {
                    responseBody = response.body().string();
                }
                sendResult.setInfo(responseBody);
                if (responseBody.startsWith("{") && responseBody.endsWith("}")) {
                    try {
                        responseBody = JSONUtil.toJsonPrettyStr(responseBody);
                    } catch (Exception e) {
                        log.error(e.toString());
                    }
                }
                sendResult.setBody(responseBody);

                sendResult.setHeaders(response.headers().toString());

                StringBuilder cookiesBuilder = StrUtil.builder();
                List<String> headerList = response.headers(Header.SET_COOKIE.toString());
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

    public static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            synchronized (HttpMsgSender.class) {
                if (okHttpClient == null) {
                    OkHttpClient.Builder builder = new OkHttpClient.Builder();
                    builder.connectTimeout(3, TimeUnit.MINUTES);

                    ConnectionPool pool = new ConnectionPool(App.config.getMaxThreads(), 10, TimeUnit.MINUTES);
                    builder.connectionPool(pool);
                    okHttpClient = builder.build();
                }
            }
        }
        return okHttpClient;
    }

    private OkHttpClient getOkHttpClient(Integer accountId) {
        if (okHttpClientMap.containsKey(accountId)) {
            return okHttpClientMap.get(accountId);
        } else {
            TAccount tAccount = accountMapper.selectByPrimaryKey(accountId);
            String accountConfig = tAccount.getAccountConfig();
            HttpAccountConfig httpAccountConfig = JSON.parseObject(accountConfig, HttpAccountConfig.class);
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(3, TimeUnit.MINUTES);
            if (httpAccountConfig.isUseProxy()) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpAccountConfig.getProxyHost(), Integer.parseInt(httpAccountConfig.getProxyPort())));
                builder.proxy(proxy);
            }

            ConnectionPool pool = new ConnectionPool(App.config.getMaxThreads(), 10, TimeUnit.MINUTES);
            builder.connectionPool(pool);
            OkHttpClient okHttpClient = builder.build();

            okHttpClientMap.put(accountId, okHttpClient);
            return okHttpClient;
        }

    }
}
