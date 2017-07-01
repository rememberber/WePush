package com.fangxuele.tool.wechat.push.logic;

import com.fangxuele.tool.wechat.push.ui.Init;
import com.fangxuele.tool.wechat.push.ui.MainWindow;
import com.opencsv.CSVWriter;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.taobao.api.response.AlibabaAliqinFcSmsNumSendResponse;
import com.xiaoleilu.hutool.date.DateUtil;
import com.xiaoleilu.hutool.json.JSONUtil;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;
import me.chanjar.weixin.mp.api.WxMpConfigStorage;
import me.chanjar.weixin.mp.api.WxMpInMemoryConfigStorage;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;

import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 推送管理
 * Created by rememberber(https://github.com/rememberber) on 2017/6/19.
 */
public class PushManage {

    private static final Log logger = LogFactory.get();

    /**
     * 预览消息
     *
     * @throws Exception
     */
    public static void preview() throws Exception {

        WxMpTemplateMessage wxMessageTemplate = makeTemplateMessage();
        WxMpKefuMessage wxMpKefuMessage = makeKefuMessage();

        WxMpService wxMpService = getWxMpService();

        switch (MainWindow.mainWindow.getMsgTypeComboBox().getSelectedItem().toString()) {
            case "模板消息":
                String[] toUsers = MainWindow.mainWindow.getPreviewUserField().getText().split(";");
                for (String toUser : toUsers) {
                    wxMessageTemplate.setToUser(toUser);
                    // ！！！发送模板消息！！！
                    wxMpService.getTemplateMsgService().sendTemplateMsg(wxMessageTemplate);
                }
                break;
            case "客服消息":
                toUsers = MainWindow.mainWindow.getPreviewUserField().getText().split(";");
                for (String toUser : toUsers) {
                    wxMpKefuMessage.setToUser(toUser);
                    // ！！！发送客服消息！！！
                    wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
                }
                break;
            case "客服消息优先":
                toUsers = MainWindow.mainWindow.getPreviewUserField().getText().split(";");
                for (String toUser : toUsers) {
                    try {
                        wxMpKefuMessage.setToUser(toUser);
                        // ！！！发送客服消息！！！
                        wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
                    } catch (Exception e) {
                        wxMessageTemplate.setToUser(toUser);
                        // ！！！发送模板消息！！！
                        wxMpService.getTemplateMsgService().sendTemplateMsg(wxMessageTemplate);
                    }
                }
                break;
            case "阿里大于模板短信":
                TaobaoClient client = new DefaultTaobaoClient(Init.configer.getAliServerUrl(), Init.configer.getAliAppKey(), Init.configer.getAliAppSecret());
                AlibabaAliqinFcSmsNumSendRequest request = makeAliTemplateMessage();
                toUsers = MainWindow.mainWindow.getPreviewUserField().getText().split(";");
                for (String toUser : toUsers) {
                    request.setRecNum(toUser);
                    AlibabaAliqinFcSmsNumSendResponse response = client.execute(request);
                    if (response.getResult() == null || !response.getResult().getSuccess()) {
                        throw new Exception(new StringBuffer().append(response.getBody()).append(";\n\nErrorCode:")
                                .append(response.getErrorCode()).append(";\n\ntelNum:").append(toUser).toString());
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 组织模板消息
     *
     * @return
     */
    synchronized public static WxMpTemplateMessage makeTemplateMessage() {
        // 拼模板
        WxMpTemplateMessage wxMessageTemplate = new WxMpTemplateMessage();
        wxMessageTemplate.setTemplateId(MainWindow.mainWindow.getMsgTemplateIdTextField().getText());
        wxMessageTemplate.setUrl(MainWindow.mainWindow.getMsgTemplateUrlTextField().getText());
        if (MainWindow.mainWindow.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            Init.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            WxMpTemplateData templateData = new WxMpTemplateData((String) tableModel.getValueAt(i, 0), ((String) tableModel.getValueAt(i, 1)).replaceAll("#ENTER#", "\n"), ((String) tableModel.getValueAt(i, 2)).trim());
            wxMessageTemplate.addWxMpTemplateData(templateData);
        }

        return wxMessageTemplate;
    }

    /**
     * 组织客服消息
     *
     * @return
     */
    synchronized public static WxMpKefuMessage makeKefuMessage() {

        WxMpKefuMessage kefuMessage = null;
        if ("图文消息".equals(MainWindow.mainWindow.getMsgKefuMsgTypeComboBox().getSelectedItem().toString())) {
            WxMpKefuMessage.WxArticle article = new WxMpKefuMessage.WxArticle();
            article.setTitle(MainWindow.mainWindow.getMsgKefuMsgTitleTextField().getText());
            article.setPicUrl(MainWindow.mainWindow.getMsgKefuPicUrlTextField().getText());
            article.setDescription(MainWindow.mainWindow.getMsgKefuDescTextField().getText());
            article.setUrl(MainWindow.mainWindow.getMsgKefuUrlTextField().getText());
            kefuMessage = WxMpKefuMessage.NEWS().addArticle(article).build();
        } else if ("文本消息".equals(MainWindow.mainWindow.getMsgKefuMsgTypeComboBox().getSelectedItem().toString())) {
            kefuMessage = WxMpKefuMessage.TEXT()
                    .content(MainWindow.mainWindow.getMsgKefuMsgTitleTextField().getText()).build();
        }

        return kefuMessage;
    }

    /**
     * 组织阿里大于模板短信消息
     *
     * @return
     */
    synchronized public static AlibabaAliqinFcSmsNumSendRequest makeAliTemplateMessage() {
        AlibabaAliqinFcSmsNumSendRequest request = new AlibabaAliqinFcSmsNumSendRequest();
        // 用户可以根据该会员ID识别是哪位会员使用了你的应用
        request.setExtend("WePush");
        // 短信类型，传入值请填写normal
        request.setSmsType("normal");

        // 模板参数
        Map<String, String> paramMap = new HashMap<>();

        if (MainWindow.mainWindow.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            Init.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            paramMap.put((String) tableModel.getValueAt(i, 0), ((String) tableModel.getValueAt(i, 1)).replaceAll("#ENTER#", "\n"));
        }

        request.setSmsParamString(JSONUtil.parseFromMap(paramMap).toJSONString(0));

        // 短信签名，传入的短信签名必须是在阿里大鱼“管理中心-短信签名管理”中的可用签名。如“阿里大鱼”已在短信签名管理中通过审核，
        // 则可传入”阿里大鱼“（传参时去掉引号）作为短信签名。短信效果示例：【阿里大鱼】欢迎使用阿里大鱼服务。
        request.setSmsFreeSignName(Init.configer.getAliSign());
        // 短信模板ID，传入的模板必须是在阿里大鱼“管理中心-短信模板管理”中的可用模板。示例：SMS_585014
        request.setSmsTemplateCode(MainWindow.mainWindow.getMsgTemplateIdTextField().getText());

        return request;
    }

    /**
     * 微信公众号配置
     *
     * @return
     */
    private static WxMpConfigStorage wxMpConfigStorage() {
        WxMpInMemoryConfigStorage configStorage = new WxMpInMemoryConfigStorage();
        configStorage.setAppId(Init.configer.getWechatAppId());
        configStorage.setSecret(Init.configer.getWechatAppSecret());
        configStorage.setToken(Init.configer.getWechatToken());
        configStorage.setAesKey(Init.configer.getWechatAesKey());
        return configStorage;
    }

    /**
     * 获取微信工具服务
     *
     * @return
     */
    public static WxMpService getWxMpService() {
        WxMpService wxMpService = new WxMpServiceImpl();
        wxMpService.setWxMpConfigStorage(wxMpConfigStorage());
        return wxMpService;
    }

    /**
     * 推送停止或结束后保存数据
     */
    public static void savePushData() throws IOException {
        File pushHisDir = new File("data/push_his");
        if (!pushHisDir.exists()) {
            pushHisDir.mkdirs();
        }

        String msgName = MainWindow.mainWindow.getMsgNameField().getText();
        String nowTime = DateUtil.now().replaceAll(":", "_");

        String[] strArray;
        CSVWriter writer;

        // 保存已发送
        if (PushData.sendSuccessList.size() > 0) {
            File toSendFile = new File(new StringBuilder("data/push_his/").append(msgName).append("-发送成功-").append(nowTime).append(".csv").toString());
            if (!toSendFile.exists()) {
                toSendFile.createNewFile();
            }
            writer = new CSVWriter(new FileWriter(toSendFile));

            for (String str : PushData.sendSuccessList) {
                strArray = new String[1];
                strArray[0] = str;
                writer.writeNext(strArray);
            }
            writer.close();
        }

        // 保存未发送
        for (String str : PushData.sendSuccessList) {
            PushData.toSendList.remove(str);
        }
        for (String str : PushData.sendFailList) {
            PushData.toSendList.remove(str);
        }
        if (PushData.toSendList.size() > 0) {
            File unSendFile = new File(new StringBuilder("data/push_his/").append(msgName).append("-未发送-").append(nowTime).append(".csv").toString());
            if (!unSendFile.exists()) {
                unSendFile.createNewFile();
            }
            writer = new CSVWriter(new FileWriter(unSendFile));
            for (String str : PushData.toSendList) {
                strArray = new String[1];
                strArray[0] = str;
                writer.writeNext(strArray);
            }
            writer.close();
        }

        // 保存发送失败
        if (PushData.sendFailList.size() > 0) {
            File failSendFile = new File(new StringBuilder("data/push_his/").append(msgName).append("-发送失败-").append(nowTime).append(".csv").toString());
            if (!failSendFile.exists()) {
                failSendFile.createNewFile();
            }
            writer = new CSVWriter(new FileWriter(failSendFile));
            for (String str : PushData.sendFailList) {
                strArray = new String[1];
                strArray[0] = str;
                writer.writeNext(strArray);
            }
            writer.close();
        }

        Init.initMemberTab();
        Init.initSettingTab();
    }

    /**
     * 输出到控制台和log
     *
     * @param log
     */
    public static void console(String log) {
        MainWindow.mainWindow.getPushConsoleTextArea().append(log + "\n");
        MainWindow.mainWindow.getPushConsoleTextArea().setCaretPosition(MainWindow.mainWindow.getPushConsoleTextArea().getText().length());
        logger.warn(log);
    }

}
