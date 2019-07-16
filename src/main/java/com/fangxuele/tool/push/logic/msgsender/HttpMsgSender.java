package com.fangxuele.tool.push.logic.msgsender;

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

        sendResult.setSuccess(true);
        return sendResult;
    }

    @Override
    public SendResult asyncSend(String[] msgData) {
        return null;
    }
}
