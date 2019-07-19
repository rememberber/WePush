package com.fangxuele.tool.push.logic.msgsender;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fangxuele.tool.push.bean.HttpMsg;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.msgmaker.HttpMsgMaker;
import lombok.extern.slf4j.Slf4j;

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
    public SendResult send(String[] msgData) {
        SendResult sendResult = new SendResult();
        try {
            HttpMsg httpMsg = httpMsgMaker.makeMsg(msgData);
            if (PushControl.dryRun) {
                sendResult.setSuccess(true);
                return sendResult;
            } else {
                HttpResponse httpResponse = null;
                switch (HttpMsgMaker.method) {
                    case "GET":
                        httpResponse = HttpRequest.get(httpMsg.getUrl()).form(httpMsg.getParamMap()).execute(true);
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
                if (httpResponse.getStatus() != 200) {
                    sendResult.setSuccess(false);
                    sendResult.setInfo(httpResponse.toString());
                }
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
        return null;
    }
}
