package com.fangxuele.tool.push.logic.msgthread;

import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.ui.form.InfinityForm;
import com.fangxuele.tool.push.util.ConsoleUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

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

    public MsgInfinitySendThread(IMsgSender msgSender) {
        this.iMsgSender = msgSender;
    }

    @Override
    public void run() {

        while (PushData.running && !PushData.toSendConcurrentLinkedQueue.isEmpty()) {
            try {
                String[] msgData = PushData.toSendConcurrentLinkedQueue.poll();
                iMsgSender.send(msgData);
                PushData.increaseSuccess();
            } catch (Exception e) {
                PushData.increaseFail();
                InfinityForm.getInstance().getPushFailCount().setText(String.valueOf(PushData.failRecords));
                ConsoleUtil.infinityConsoleWithLog("发送异常：" + ExceptionUtils.getStackTrace(e));
            }
            // 已处理+1
            PushData.increaseProcessed();
            InfinityForm.getInstance().getPushSuccessCount().setText(String.valueOf(PushData.successRecords));

            // 总进度条
            InfinityForm.getInstance().getPushTotalProgressBar().setValue(PushData.processedRecords.intValue());
        }

    }
}
