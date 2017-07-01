package com.fangxuele.tool.wechat.push.logic;

import com.fangxuele.tool.wechat.push.ui.MainWindow;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

/**
 * 客服消息优先发送服务线程
 * Created by rememberber(https://github.com/rememberber) on 2017/3/29.
 */
public class KeFuPriorMsgServiceThread extends BaseMsgServiceThread {

    /**
     * 构造函数
     *
     * @param pageFrom 起始页
     * @param pageTo   截止页
     * @param pageSize 页大小
     */
    public KeFuPriorMsgServiceThread(int pageFrom, int pageTo, int pageSize) {
        super(pageFrom, pageTo, pageSize);
    }

    @Override
    public void run() {

        // 初始化当前线程
        initCurrentThread();

        WxMpKefuMessage wxMpKefuMessage = PushManage.makeKefuMessage();
        WxMpTemplateMessage wxMpTemplateMessage = PushManage.makeTemplateMessage();

        for (int i = 0; i < list.size(); i++) {
            if (!PushData.running) {
                // 停止
                PushData.increaseStopedThread();
                return;
            }

            String openId = list.get(i);
            try {
                wxMpKefuMessage.setToUser(openId);
                wxMpTemplateMessage.setToUser(openId);
                try {// 空跑控制
                    if (!MainWindow.mainWindow.getDryRunCheckBox().isSelected()) {
                        wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
                    }
                } catch (Exception e) {
                    if (!MainWindow.mainWindow.getDryRunCheckBox().isSelected()) {
                        wxMpService.getTemplateMsgService().sendTemplateMsg(wxMpTemplateMessage);
                    }
                }

                // 总发送成功+1
                PushData.increaseSuccess();
                MainWindow.mainWindow.getPushSuccessCount().setText(String.valueOf(PushData.successRecords));

                // 当前线程发送成功+1
                currentThreadSuccessCount++;
                tableModel.setValueAt(currentThreadSuccessCount, tableRow, 2);

                // 保存发送成功
                PushData.sendSuccessList.add(openId);
            } catch (Exception e) {
                // 总发送失败+1
                PushData.increaseFail();
                MainWindow.mainWindow.getPushFailCount().setText(String.valueOf(PushData.failRecords));

                // 保存发送失败
                PushData.sendFailList.add(openId);

                // 失败异常信息输出控制台
                PushManage.console(new StringBuffer().append("发送失败:").append(e.getMessage()).append(";openid:").append(openId).toString());

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
