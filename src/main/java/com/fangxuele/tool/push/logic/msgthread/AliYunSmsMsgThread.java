package com.fangxuele.tool.push.logic.msgthread;

import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.fangxuele.tool.push.logic.PushControl;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.msgmaker.AliyunMsgMaker;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.util.ConsoleUtil;

/**
 * <pre>
 * 阿里云短信发送服务线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2018/3/16.
 */
public class AliYunSmsMsgThread extends BaseMsgThread {

    /**
     * 构造函数
     *
     * @param startIndex 起始索引
     * @param endIndex   截止索引
     */
    public AliYunSmsMsgThread(int startIndex, int endIndex) {
        super(startIndex, endIndex);
    }

    @Override
    public void run() {
        // 初始化当前线程
        initCurrentThread();

        //初始化acsClient,暂不支持region化
        IAcsClient acsClient = PushControl.getAliyunIAcsClient();

        // 组织模板消息
        SendSmsRequest sendSmsRequest;
        SendSmsResponse response;
        AliyunMsgMaker aliyunMsgMaker = new AliyunMsgMaker();

        for (int i = 0; i < list.size(); i++) {
            if (!PushData.running) {
                // 停止
                PushData.increaseStopedThread();
                return;
            }

            // 本条消息所需的数据
            String[] msgData = list.get(i);
            String telNum = msgData[0];
            try {
                sendSmsRequest = aliyunMsgMaker.makeMsg(msgData);
                sendSmsRequest.setPhoneNumbers(telNum);

                // 空跑控制
                if (!PushForm.pushForm.getDryRunCheckBox().isSelected()) {
                    response = acsClient.getAcsResponse(sendSmsRequest);
                    if (response.getCode() != null && "OK".equals(response.getCode())) {
                        // 总发送成功+1
                        PushData.increaseSuccess();
                        PushForm.pushForm.getPushSuccessCount().setText(String.valueOf(PushData.successRecords));

                        // 当前线程发送成功+1
                        currentThreadSuccessCount++;
                        tableModel.setValueAt(currentThreadSuccessCount, tableRow, 2);

                        // 保存发送成功
                        PushData.sendSuccessList.add(msgData);
                    } else {
                        // 总发送失败+1
                        PushData.increaseFail();
                        PushForm.pushForm.getPushFailCount().setText(String.valueOf(PushData.failRecords));

                        // 保存发送失败
                        PushData.sendFailList.add(msgData);

                        // 失败异常信息输出控制台
                        ConsoleUtil.consoleWithLog("发送失败:" + response.getMessage() + ";ErrorCode:" +
                                response.getCode() + ";telNum:" + telNum);

                        // 当前线程发送失败+1
                        currentThreadFailCount++;
                        tableModel.setValueAt(currentThreadFailCount, tableRow, 3);
                    }
                } else {
                    // 总发送成功+1
                    PushData.increaseSuccess();
                    PushForm.pushForm.getPushSuccessCount().setText(String.valueOf(PushData.successRecords));

                    // 当前线程发送成功+1
                    currentThreadSuccessCount++;
                    tableModel.setValueAt(currentThreadSuccessCount, tableRow, 2);

                    // 保存发送成功
                    PushData.sendSuccessList.add(msgData);
                }

            } catch (Exception e) {
                // 总发送失败+1
                PushData.increaseFail();
                PushForm.pushForm.getPushFailCount().setText(String.valueOf(PushData.failRecords));

                // 保存发送失败
                PushData.sendFailList.add(msgData);

                // 失败异常信息输出控制台
                ConsoleUtil.consoleWithLog("发送失败:" + e.getMessage() + ";telNum:" + telNum);

                // 当前线程发送失败+1
                currentThreadFailCount++;
                tableModel.setValueAt(currentThreadFailCount, tableRow, 3);
            }
            // 当前线程进度条
            tableModel.setValueAt((int) ((double) (i + 1) / list.size() * 100), tableRow, 5);

            // 总进度条
            PushForm.pushForm.getPushTotalProgressBar().setValue(PushData.successRecords.intValue() + PushData.failRecords.intValue());
        }

        // 当前线程结束
        currentThreadFinish();
    }

}
