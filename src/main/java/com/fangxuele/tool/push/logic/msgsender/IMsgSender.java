package com.fangxuele.tool.push.logic.msgsender;

/**
 * <pre>
 * 消息发送器接口
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/15.
 */
public interface IMsgSender {

    /**
     * 发送消息
     *
     * @param msgData 消息数据
     */
    SendResult send(String[] msgData);

    /**
     * 异步发送消息
     *
     * @param msgData 消息数据
     */
    SendResult asyncSend(String[] msgData);
}
