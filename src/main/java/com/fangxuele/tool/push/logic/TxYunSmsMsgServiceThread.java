package com.fangxuele.tool.push.logic;

import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.MainWindow;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * 腾讯云短信发送服务线程
 * Created by rememberber(https://github.com/rememberber) on 2018/3/16.
 */
public class TxYunSmsMsgServiceThread extends BaseMsgServiceThread {

    /**
     * 构造函数
     *
     * @param pageFrom 起始页
     * @param pageTo   截止页
     * @param pageSize 页大小
     */
    public TxYunSmsMsgServiceThread(int pageFrom, int pageTo, int pageSize) {
        super(pageFrom, pageTo, pageSize);
    }

    @Override
    public void run() {

        // 初始化当前线程
        initCurrentThread();

        String txyunAppId = Init.configer.getTxyunAppId();
        String txyunAppKey = Init.configer.getTxyunAppKey();

        if (StringUtils.isEmpty(txyunAppId) || StringUtils.isEmpty(txyunAppKey)) {
            JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(),
                    "请先在设置中填写并保存腾讯云短信相关配置！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        SmsSingleSender ssender = new SmsSingleSender(Integer.valueOf(txyunAppId), txyunAppKey);
        int templateId = Integer.parseInt(MainWindow.mainWindow.getMsgTemplateIdTextField().getText());
        String smsSign = Init.configer.getAliyunSign();

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
                String[] params = PushManage.makeTxyunMessage(msgData);

                // 空跑控制
                if (!MainWindow.mainWindow.getDryRunCheckBox().isSelected()) {
                    SmsSingleSenderResult result = ssender.sendWithParam("86", telNum,
                            templateId, params, smsSign, "", "");

                    if (result.result == 0) {
                        // 总发送成功+1
                        PushData.increaseSuccess();
                        MainWindow.mainWindow.getPushSuccessCount().setText(String.valueOf(PushData.successRecords));

                        // 当前线程发送成功+1
                        currentThreadSuccessCount++;
                        tableModel.setValueAt(currentThreadSuccessCount, tableRow, 2);

                        // 保存发送成功
                        PushData.sendSuccessList.add(msgData);
                    } else {
                        // 总发送失败+1
                        PushData.increaseFail();
                        MainWindow.mainWindow.getPushFailCount().setText(String.valueOf(PushData.failRecords));

                        // 保存发送失败
                        PushData.sendFailList.add(msgData);

                        // 失败异常信息输出控制台
                        PushManage.console(new StringBuffer().append("发送失败:").append(result.toString())
                                .append(";telNum:").append(telNum).toString());

                        // 当前线程发送失败+1
                        currentThreadFailCount++;
                        tableModel.setValueAt(currentThreadFailCount, tableRow, 3);
                    }
                } else {
                    // 总发送成功+1
                    PushData.increaseSuccess();
                    MainWindow.mainWindow.getPushSuccessCount().setText(String.valueOf(PushData.successRecords));

                    // 当前线程发送成功+1
                    currentThreadSuccessCount++;
                    tableModel.setValueAt(currentThreadSuccessCount, tableRow, 2);

                    // 保存发送成功
                    PushData.sendSuccessList.add(msgData);
                }

            } catch (Exception e) {
                // 总发送失败+1
                PushData.increaseFail();
                MainWindow.mainWindow.getPushFailCount().setText(String.valueOf(PushData.failRecords));

                // 保存发送失败
                PushData.sendFailList.add(msgData);

                // 失败异常信息输出控制台
                PushManage.console(new StringBuffer().append("发送失败:").append(e.getMessage()).append(";telNum:").append(telNum).toString());

                // 当前线程发送失败+1
                currentThreadFailCount++;
                tableModel.setValueAt(currentThreadFailCount, tableRow, 3);
            }
            // 当前线程进度条
            tableModel.setValueAt((int) ((double) (i + 1) / list.size() * 100), tableRow, 5);

            // 总进度条
            MainWindow.mainWindow.getPushTotalProgressBar().setValue((int) (PushData.successRecords + PushData.failRecords));
        }

        // 当前线程结束
        currentThreadFinish();
    }

}
