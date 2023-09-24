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

            for (int i = 0; i < list.size(); i++) {
                if (!taskRunThread.running) {
                    // 停止
                    return;
                }

                // 间隔推送
                if (isIntervalPush) {
                    Thread.sleep(tTask.getIntervalTime() * 1000);
                }

                // 本条消息所需的数据
                String[] msgData = list.get(i);
                SendResult sendResult = iMsgSender.send(msgData);

                if (tTask.getMsgType() == MessageTypeEnum.HTTP_CODE && tTask.getSaveResult() == 1) {
                    String body = sendResult.getInfo() == null ? "" : sendResult.getInfo();
                    msgData = Arrays.append(msgData, body);
                }

                if (sendResult.isSuccess()) {
                    // 总发送成功+1
                    taskRunThread.increaseSuccess();

                    // 保存发送成功
                    taskRunThread.sendSuccessList.add(msgData);
                } else {
                    // 总发送失败+1
                    taskRunThread.increaseFail();

                    // 保存发送失败
                    taskRunThread.sendFailList.add(msgData);

                    // 失败异常信息输出控制台
                    ConsoleUtil.pushLog(taskRunThread.getLogWriter(), "发送失败:" + sendResult.getInfo() + ";msgData:" + JSONUtil.toJsonPrettyStr(msgData));
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
