package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.bean.HttpMsg;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.HttpMsgMaker;
import lombok.extern.slf4j.Slf4j;

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

    public HttpMsgSender() {
        httpMsgMaker = new HttpMsgMaker();
    }

    @Override
    public HttpSendResult send(String[] msgData) {
        HttpSendResult sendResult = new HttpSendResult();
        HttpResponse httpResponse = null;
        try {
            HttpMsg httpMsg = httpMsgMaker.makeMsg(msgData);
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                switch (HttpMsgMaker.method) {
                    case "GET":
                        httpResponse = HttpRequest.get(httpMsg.getUrl()).form(httpMsg.getParamMap()).execute(true);
                        break;
                    case "POST":
                        break;
                    case "PUT":
                        break;
                    case "PATCH":
                        break;
                    case "DELETE":
                        break;
                    case "HEAD":
                        break;
                    case "OPTIONS":
                        break;
                    default:
                }
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
        headerBuilder.append("Request Headers: ").append(StrUtil.CRLF);
        for (Map.Entry<String, List<String>> entry : httpResponse.headers().entrySet()) {
            headerBuilder.append("    ").append(entry).append(StrUtil.CRLF);
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

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }
}
