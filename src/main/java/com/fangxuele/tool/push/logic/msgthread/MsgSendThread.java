package com.fangxuele.tool.push.logic.msgthread;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.msgsender.IMsgSender;
import com.fangxuele.tool.push.logic.msgsender.SendResult;
import com.fangxuele.tool.push.ui.form.PushForm;
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

    /**
     * 构造函数
     *
     * @param startIndex 起始页
     * @param endIndex   截止页
     */
    public MsgSendThread(int startIndex, int endIndex, IMsgSender iMsgSender) {
        super(startIndex, endIndex);
        this.iMsgSender = iMsgSender;
    }

    @Override
    public void run() {
        try {
            // 初始化当前线程
            initCurrentThread();

            for (int i = 0; i < list.size(); i++) {
                if (!PushData.running) {
                    // 停止
                    return;
                }

                // 本条消息所需的数据
                String[] msgData = list.get(i);
                SendResult sendResult = iMsgSender.send(msgData);

                if (msgType == MessageTypeEnum.HTTP_CODE && PushControl.saveResponseBody) {
                    String body = sendResult.getInfo() == null ? "" : sendResult.getInfo();
                    msgData = Arrays.append(msgData, body);
                }

                if (sendResult.isSuccess()) {
                    // 总发送成功+1
                    PushData.increaseSuccess();
                    PushForm.getInstance().getPushSuccessCount().setText(String.valueOf(PushData.successRecords));

                    // 当前线程发送成功+1
                    successCountLocal.set(successCountLocal.get() + 1);
                    pushTableLocal.get().setValueAt(successCountLocal.get(), tableRow, 2);

                    // 保存发送成功
                    PushData.sendSuccessList.add(msgData);
                } else {
                    // 总发送失败+1
                    PushData.increaseFail();
                    PushForm.getInstance().getPushFailCount().setText(String.valueOf(PushData.failRecords));

                    // 保存发送失败
                    PushData.sendFailList.add(msgData);

                    // 失败异常信息输出控制台
                    ConsoleUtil.consoleOnly("发送失败:" + sendResult.getInfo() + ";msgData:" + JSONUtil.toJsonPrettyStr(msgData));

                    // 当前线程发送失败+1
                    failCountLocal.set(failCountLocal.get() + 1);
                    pushTableLocal.get().setValueAt(failCountLocal.get(), tableRow, 3);
                }

                // 当前线程进度条
                pushTableLocal.get().setValueAt((int) ((double) (i + 1) / list.size() * 100), tableRow, 5);

                // 总进度条
                PushForm.getInstance().getPushTotalProgressBar().setValue(PushData.successRecords.intValue() + PushData.failRecords.intValue());
            }

            // 当前线程结束
            currentThreadFinish();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        } finally {
            successCountLocal.remove();
            failCountLocal.remove();
            pushTableLocal.remove();
        }

    }

}
