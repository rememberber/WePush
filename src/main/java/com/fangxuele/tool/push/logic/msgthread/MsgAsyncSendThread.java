package com.fangxuele.tool.push.logic.msgthread;

import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.logic.msgsender.SendResult;
import com.fangxuele.tool.push.ui.form.BoostForm;

/**
 * <pre>
 * 消息异步发送服务线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/7/7.
 */
public class MsgAsyncSendThread extends Thread {

    private IMsgSender iMsgSender;

    public MsgAsyncSendThread(IMsgSender msgSender) {
        this.iMsgSender = msgSender;
    }

    @Override
    public void run() {

        for (int i = 0; i < PushData.toSendList.size(); i++) {

            // 本条消息所需的数据
            String[] msgData = PushData.toSendList.get(i);
            SendResult sendResult = iMsgSender.asyncSend(msgData);
            // 已处理+1
            PushData.increaseProcessed();
            BoostForm.boostForm.getProcessedCountLabel().setText(String.valueOf(PushData.processedRecords));

            // 总进度条
            BoostForm.boostForm.getProcessedProgressBar().setValue(PushData.processedRecords.intValue());
        }

    }
}
