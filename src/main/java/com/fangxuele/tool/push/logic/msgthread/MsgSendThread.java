package com.fangxuele.tool.push.logic.msgthread;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.domain.TTask;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.TaskRunThread;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.logic.msgsender.SendResult;
import com.fangxuele.tool.push.util.ConsoleUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bouncycastle.util.Arrays;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * 消息发送服务线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/3/29.
 */
public class MsgSendThread extends BaseMsgThread {

    private IMsgSender iMsgSender;

    public MsgSendThread(int startIndex, int endIndex, IMsgSender msgSender, TaskRunThread taskRunThread) {
        super(startIndex, endIndex, taskRunThread);
        this.iMsgSender = msgSender;
    }

    @Override
    public void run() {
        try {
            // 初始化当前线程
            initCurrentThread();
            TTask tTask = taskRunThread.getTTask();

            // 间隔推送
            boolean isIntervalPush = tTask.getIntervalPush() != null && tTask.getIntervalPush() == 1 && tTask.getIntervalTime() != null;

            int batchSize = isIntervalPush ? 1 : Math.max(1, iMsgSender.recommendedBatchSize());
            for (int offset = 0; offset < list.size(); offset += batchSize) {
                if (!taskRunThread.running) {
                    // 停止
                    return;
                }

                // 间隔推送
                if (isIntervalPush) {
                    Thread.sleep(tTask.getIntervalTime() * 1000);
                }

                int batchEnd = Math.min(list.size(), offset + batchSize);
                List<String[]> batch = new ArrayList<>(list.subList(offset, batchEnd));
                List<SendResult> results = iMsgSender.sendBatch(batch);
                if (results.size() != batch.size()) {
                    throw new IllegalStateException("批量发送返回结果数量不一致：请求=" + batch.size() + "，结果=" + results.size());
                }

                for (int i = 0; i < batch.size(); i++) {
                    String[] msgData = batch.get(i);
                    SendResult sendResult = results.get(i);

                    if (tTask.getMsgType() == MessageTypeEnum.HTTP_CODE && tTask.getSaveResult() == 1) {
                        String body = sendResult.getInfo() == null ? "" : sendResult.getInfo();
                        msgData = Arrays.append(msgData, body);
                    }

                    if (sendResult.isSuccess()) {
                        taskRunThread.increaseSuccess();
                        taskRunThread.sendSuccessList.add(msgData);
                    } else {
                        taskRunThread.increaseFail();
                        taskRunThread.sendFailList.add(msgData);

                        ConsoleUtil.pushLog(taskRunThread.getLogWriter(), "发送失败:" + sendResult.getInfo() + ";msgData:" + JSONUtil.toJsonStr(msgData));
                    }
                }
            }

            // 当前线程结束
            currentThreadFinish();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            // 线程结束，处理完毕的线程数+1
            taskRunThread.finishedThreadCount.incrementAndGet();
        }
    }

}
