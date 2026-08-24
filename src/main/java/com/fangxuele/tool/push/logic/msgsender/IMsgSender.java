package com.fangxuele.tool.push.logic.msgsender;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 平台原生批量接口建议的批大小。默认逐条发送。
     */
    default int recommendedBatchSize() {
        return 1;
    }

    /**
     * 批量发送并返回与输入顺序一致的结果。实现类仅可在语义等价时合并请求。
     */
    default List<SendResult> sendBatch(List<String[]> batch) {
        List<SendResult> results = new ArrayList<>(batch.size());
        for (String[] msgData : batch) {
            results.add(send(msgData));
        }
        return results;
    }
}
