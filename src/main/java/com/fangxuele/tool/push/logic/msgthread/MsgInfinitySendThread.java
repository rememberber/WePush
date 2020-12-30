package com.fangxuele.tool.push.logic.msgthread;

import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.ui.form.InfinityForm;

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

        for (int i = 0; i < PushData.toSendList.size(); i++) {
            if (!PushData.running) {
                PushData.TO_SEND_COUNT.set(i);
                return;
            }
            // 本条消息所需的数据
            String[] msgData = PushData.toSendList.get(i);
            try {
                iMsgSender.send(msgData);
                PushData.increaseSuccess();
            } catch (Exception e) {
                PushData.increaseFail();
            }
            // 已处理+1
            PushData.increaseProcessed();
            InfinityForm.getInstance().getPushSuccessCount().setText(String.valueOf(PushData.successRecords));

            // 总进度条
            InfinityForm.getInstance().getPushTotalProgressBar().setValue(PushData.processedRecords.intValue());
        }

    }
}
