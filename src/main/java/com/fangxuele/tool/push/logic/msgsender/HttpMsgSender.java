package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.HttpMsg;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.HttpMsgMaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Map;

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

    public volatile static Proxy proxy;

    public HttpMsgSender() {
        httpMsgMaker = new HttpMsgMaker();
    }

    @Override
    public HttpSendResult send(String[] msgData) {
        HttpSendResult sendResult = new HttpSendResult();
        HttpResponse httpResponse;
        try {
            HttpMsg httpMsg = httpMsgMaker.makeMsg(msgData);
            HttpRequest httpRequest;
            switch (HttpMsgMaker.method) {
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
                for (HttpCookie cookie : httpMsg.getCookies()) {
                    httpRequest.cookie(cookie);
                }
            }
            if (StringUtils.isNotEmpty(httpMsg.getBody())) {
                httpRequest.body(httpMsg.getBody());
            }
            if (App.config.isHttpUseProxy()) {
                httpRequest.setProxy(getProxy());
            }

            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                httpResponse = httpRequest.execute(true);
                if (!httpResponse.isOk()) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(httpResponse.toString());
                    return sendResult;
                }
            }
        } catch (Exception e) {
            sendResult.setSuccess(false);
            sendResult.setInfo(e.getMessage());
            log.error(e.toString());
            return sendResult;
        }
        StringBuilder headerBuilder = StrUtil.builder();
        for (Map.Entry<String, List<String>> entry : httpResponse.headers().entrySet()) {
            headerBuilder.append(entry).append(StrUtil.CRLF);
        }
        sendResult.setHeader(headerBuilder.toString());

        String body = httpResponse.body();
        if (body.startsWith("{") && body.endsWith("}")) {
            try {
                body = JSONUtil.toJsonPrettyStr(body);
            } catch (Exception e) {
                log.error(e.toString());
            }
        }
        sendResult.setBody(body);

        sendResult.setSuccess(true);
        return sendResult;
    }

    private static Proxy getProxy() {
        if (proxy == null) {
            synchronized (HttpMsgSender.class) {
                if (proxy == null) {
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(App.config.getHttpProxyHost(), Integer.parseInt(App.config.getHttpProxyPort())));
                }
            }
        }
        return proxy;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }
}
