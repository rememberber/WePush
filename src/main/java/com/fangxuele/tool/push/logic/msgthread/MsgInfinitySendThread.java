package com.fangxuele.tool.push.logic.msgthread;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.logic.InfinityTaskRunThread;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.logic.msgsender.SendResult;
import com.fangxuele.tool.push.util.ConsoleUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * 消息异步发送服务线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/7/7.
 */
public class MsgInfinitySendThread extends Thread {

    private IMsgSender iMsgSender;

    private InfinityTaskRunThread infinityTaskRunThread;

    private final String workerName;

    public MsgInfinitySendThread(IMsgSender msgSender, InfinityTaskRunThread infinityTaskRunThread, String workerName) {
        super(workerName);
        this.workerName = workerName;
        this.iMsgSender = msgSender;
        this.infinityTaskRunThread = infinityTaskRunThread;
        infinityTaskRunThread.activeThreadConcurrentLinkedQueue.offer(workerName);
        infinityTaskRunThread.threadStatusMap.put(workerName, true);
    }

    @Override
    public void run() {
        try {
            int batchSize = Math.max(1, iMsgSender.recommendedBatchSize());
            while (infinityTaskRunThread.running
                    && Boolean.TRUE.equals(infinityTaskRunThread.threadStatusMap.get(workerName))
                    && !infinityTaskRunThread.toSendConcurrentLinkedQueue.isEmpty()) {
                List<String[]> batch = new ArrayList<>(batchSize);
                for (int i = 0; i < batchSize; i++) {
                    String[] msgData = infinityTaskRunThread.toSendConcurrentLinkedQueue.poll();
                    if (msgData == null) {
                        break;
                    }
                    batch.add(msgData);
                }
                if (batch.isEmpty()) {
                    continue;
                }
                try {
                    List<SendResult> results = iMsgSender.sendBatch(batch);
                    if (results.size() != batch.size()) {
                        throw new IllegalStateException("批量发送返回结果数量不一致：请求=" + batch.size() + "，结果=" + results.size());
                    }
                    for (int i = 0; i < batch.size(); i++) {
                        recordResult(batch.get(i), results.get(i));
                    }
                } catch (Exception e) {
                    ConsoleUtil.pushLog(infinityTaskRunThread.getLogWriter(), "发送异常：" + ExceptionUtils.getStackTrace(e));
                    for (String[] msgData : batch) {
                        infinityTaskRunThread.increaseFail();
                        infinityTaskRunThread.sendFailList.add(msgData);
                        infinityTaskRunThread.increaseProcessed();
                    }
                }
            }
        } finally {
            infinityTaskRunThread.activeThreadConcurrentLinkedQueue.remove(workerName);
            infinityTaskRunThread.threadStatusMap.put(workerName, false);
        }
    }

    private void recordResult(String[] msgData, SendResult sendResult) {
        if (sendResult.isSuccess()) {
            infinityTaskRunThread.increaseSuccess();
            infinityTaskRunThread.sendSuccessList.add(msgData);
        } else {
            infinityTaskRunThread.increaseFail();
            infinityTaskRunThread.sendFailList.add(msgData);
            ConsoleUtil.pushLog(infinityTaskRunThread.getLogWriter(), "发送失败:" + sendResult.getInfo() + ";msgData:" + JSONUtil.toJsonStr(msgData));
        }
        infinityTaskRunThread.increaseProcessed();
    }
}
