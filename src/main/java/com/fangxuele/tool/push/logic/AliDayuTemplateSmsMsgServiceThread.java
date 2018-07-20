package com.fangxuele.tool.push.logic;

import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.MainWindow;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.taobao.api.response.AlibabaAliqinFcSmsNumSendResponse;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * 阿里模板短信发送服务线程
 * Created by rememberber(https://github.com/rememberber) on 2017/3/29.
 */
public class AliDayuTemplateSmsMsgServiceThread extends BaseMsgServiceThread {

    /**
     * 构造函数
     *
     * @param pageFrom 起始页
     * @param pageTo   截止页
     * @param pageSize 页大小
     */
    public AliDayuTemplateSmsMsgServiceThread(int pageFrom, int pageTo, int pageSize) {
        super(pageFrom, pageTo, pageSize);
    }

    @Override
    public void run() {

        // 初始化当前线程
        initCurrentThread();

        String aliServerUrl = Init.configer.getAliServerUrl();
        String aliAppKey = Init.configer.getAliAppKey();
        String aliAppSecret = Init.configer.getAliAppSecret();

        if (StringUtils.isEmpty(aliServerUrl) || StringUtils.isEmpty(aliAppKey)
                || StringUtils.isEmpty(aliAppSecret)) {
            JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(),
                    "请先在设置中填写并保存阿里大于相关配置！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        TaobaoClient client = new DefaultTaobaoClient(aliServerUrl, aliAppKey, aliAppSecret);

        // 组织模板消息
        AlibabaAliqinFcSmsNumSendRequest alibabaAliqinFcSmsNumSendRequest;

        AlibabaAliqinFcSmsNumSendResponse response;

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
                alibabaAliqinFcSmsNumSendRequest = PushManage.makeAliTemplateMessage(msgData);
                alibabaAliqinFcSmsNumSendRequest.setRecNum(telNum);

                // 空跑控制
                if (!MainWindow.mainWindow.getDryRunCheckBox().isSelected()) {
                    response = client.execute(alibabaAliqinFcSmsNumSendRequest);
                    if (response.getResult() != null && response.getResult().getSuccess()) {
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
                        PushManage.console(new StringBuffer().append("发送失败:").append(response.getBody()).append(";ErrorCode:")
                                .append(response.getErrorCode()).append(";telNum:").append(telNum).toString());

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
