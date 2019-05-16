package com.fangxuele.tool.push.logic;

import com.fangxuele.tool.push.ui.form.PushForm;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

/**
 * <pre>
 * 客服消息优先发送服务线程
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/3/29.
 */
public class KeFuPriorMsgServiceThread extends BaseMsgServiceThread {

    /**
     * 构造函数
     *
     * @param pageFrom 起始索引
     * @param pageTo   截止索引
     */
    public KeFuPriorMsgServiceThread(int pageFrom, int pageTo) {
        super(pageFrom, pageTo);
    }

    @Override
    public void run() {

        // 初始化当前线程
        initCurrentThread();

        WxMpKefuMessage wxMpKefuMessage;
        WxMpTemplateMessage wxMpTemplateMessage;

        for (int i = 0; i < list.size(); i++) {
            if (!PushData.running) {
                // 停止
                PushData.increaseStopedThread();
                return;
            }

            // 本条消息所需的数据
            String[] msgData = list.get(i);
            String openId = "";
            try {
                openId = msgData[0];
                wxMpKefuMessage = MessageMaker.makeKefuMessage(msgData);
                wxMpTemplateMessage = MessageMaker.makeMpTemplateMessage(msgData);

                wxMpKefuMessage.setToUser(openId);
                wxMpTemplateMessage.setToUser(openId);
                try {// 空跑控制
                    if (!PushForm.pushForm.getDryRunCheckBox().isSelected()) {
                        wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
                    }
                } catch (Exception e) {
                    if (!PushForm.pushForm.getDryRunCheckBox().isSelected()) {
                        wxMpService.getTemplateMsgService().sendTemplateMsg(wxMpTemplateMessage);
                    }
                }

                // 总发送成功+1
                PushData.increaseSuccess();
                PushForm.pushForm.getPushSuccessCount().setText(String.valueOf(PushData.successRecords));

                // 当前线程发送成功+1
                currentThreadSuccessCount++;
                tableModel.setValueAt(currentThreadSuccessCount, tableRow, 2);

                // 保存发送成功
                PushData.sendSuccessList.add(msgData);
            } catch (Exception e) {
                // 总发送失败+1
                PushData.increaseFail();
                PushForm.pushForm.getPushFailCount().setText(String.valueOf(PushData.failRecords));

                // 保存发送失败
                PushData.sendFailList.add(msgData);

                // 失败异常信息输出控制台
                PushManage.console("发送失败:" + e.getMessage() + ";openid:" + openId);

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
