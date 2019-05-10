package com.fangxuele.tool.push.logic;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.PushForm;
import com.fangxuele.tool.push.ui.form.SettingForm;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * <pre>
 * 阿里云短信发送服务线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2018/3/16.
 */
public class AliYunSmsMsgServiceThread extends BaseMsgServiceThread {

    /**
     * 构造函数
     *
     * @param startIndex 起始索引
     * @param endIndex   截止索引
     */
    public AliYunSmsMsgServiceThread(int startIndex, int endIndex) {
        super(startIndex, endIndex);
    }

    @Override
    public void run() {

        // 初始化当前线程
        initCurrentThread();

        String aliyunAccessKeyId = Init.config.getAliyunAccessKeyId();
        String aliyunAccessKeySecret = Init.config.getAliyunAccessKeySecret();

        if (StringUtils.isEmpty(aliyunAccessKeyId) || StringUtils.isEmpty(aliyunAccessKeySecret)) {
            JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(),
                    "请先在设置中填写并保存阿里云短信相关配置！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        //初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunAccessKeyId, aliyunAccessKeySecret);
        DefaultProfile.addEndpoint("cn-hangzhou", "Dysmsapi", "cn-hangzhou");
        IAcsClient acsClient = new DefaultAcsClient(profile);

        // 组织模板消息
        SendSmsRequest sendSmsRequest;

        SendSmsResponse response;

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
                sendSmsRequest = MessageMaker.makeAliyunMessage(msgData);
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
                        PushManage.console("发送失败:" + response.getMessage() + ";ErrorCode:" +
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
                PushManage.console("发送失败:" + e.getMessage() + ";telNum:" + telNum);

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
