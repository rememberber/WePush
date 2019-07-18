package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fangxuele.tool.push.bean.HttpMsg;
import com.fangxuele.tool.push.logic.msgmaker.HttpMsgMaker;

/**
 * <pre>
 * Http消息发送器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/7/16.
 */
public class HttpMsgSender implements IMsgSender {

    private HttpMsgMaker httpMsgMaker;

    public HttpMsgSender() {
        httpMsgMaker = new HttpMsgMaker();
    }

    @Override
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        HttpMsg httpMsg = httpMsgMaker.makeMsg(msgData);
        switch (HttpMsgMaker.method) {
            case "GET":
                HttpResponse httpResponse = HttpRequest.get(httpMsg.getUrl()).form(httpMsg.getParamMap()).execute();
                System.err.println(httpResponse.body());
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
        sendResult.setSuccess(true);
        return sendResult;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }
}
