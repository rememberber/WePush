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
import me.chanjar.weixin.common.exception.WxErrorException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        List<String[]> msgDataList = new ArrayList<>();

        WxMpTemplateMessage wxMessageTemplate;
        WxMpKefuMessage wxMpKefuMessage;

        WxMpService wxMpService = getWxMpService();

        for (String data : MainWindow.mainWindow.getPreviewUserField().getText().split(";")) {
            msgDataList.add(data.split(","));
        }
        switch (MainWindow.mainWindow.getMsgTypeComboBox().getSelectedItem().toString()) {
            case "模板消息":
                for (String[] msgData : msgDataList) {
                    wxMessageTemplate = makeTemplateMessage(msgData);
                    wxMessageTemplate.setToUser(msgData[0].trim());
                    // ！！！发送模板消息！！！
                    wxMpService.getTemplateMsgService().sendTemplateMsg(wxMessageTemplate);
                }
                break;
            case "客服消息":
                for (String[] msgData : msgDataList) {
                    wxMpKefuMessage = makeKefuMessage(msgData);
                    wxMpKefuMessage.setToUser(msgData[0]);
                    // ！！！发送客服消息！！！
                    wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
                }
                break;
            case "客服消息优先":
                for (String[] msgData : msgDataList) {
                    try {
                        wxMpKefuMessage = makeKefuMessage(msgData);
                        wxMpKefuMessage.setToUser(msgData[0]);
                        // ！！！发送客服消息！！！
                        wxMpService.getKefuService().sendKefuMessage(wxMpKefuMessage);
                    } catch (Exception e) {
                        wxMessageTemplate = makeTemplateMessage(msgData);
                        wxMessageTemplate.setToUser(msgData[0].trim());
                        // ！！！发送模板消息！！！
                        wxMpService.getTemplateMsgService().sendTemplateMsg(wxMessageTemplate);
                    }
                }
                break;
            case "阿里大于模板短信":
                TaobaoClient client = new DefaultTaobaoClient(Init.configer.getAliServerUrl(), Init.configer.getAliAppKey(), Init.configer.getAliAppSecret());
                for (String[] msgData : msgDataList) {
                    AlibabaAliqinFcSmsNumSendRequest request = makeAliTemplateMessage(msgData);
                    request.setRecNum(msgData[0]);
                    AlibabaAliqinFcSmsNumSendResponse response = client.execute(request);
                    if (response.getResult() == null || !response.getResult().getSuccess()) {
                        throw new Exception(new StringBuffer().append(response.getBody()).append(";\n\nErrorCode:")
                                .append(response.getErrorCode()).append(";\n\ntelNum:").append(msgData[0]).toString());
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
     * @param msgData
     * @return
     */
    synchronized public static WxMpTemplateMessage makeTemplateMessage(String[] msgData) {
        // 拼模板
        WxMpTemplateMessage wxMessageTemplate = WxMpTemplateMessage.builder().build();
        wxMessageTemplate.setTemplateId(MainWindow.mainWindow.getMsgTemplateIdTextField().getText().trim());
        wxMessageTemplate.setUrl(MainWindow.mainWindow.getMsgTemplateUrlTextField().getText().trim());

        String appid = MainWindow.mainWindow.getMsgTemplateMiniAppidTextField().getText().trim();
        String pagePath = MainWindow.mainWindow.getMsgTemplateMiniPagePathTextField().getText().trim();
        Pattern p = Pattern.compile("\\{([^{}]+)\\}");
        Matcher matcher = p.matcher(pagePath);
        while (matcher.find()) {
            pagePath = pagePath.replace(matcher.group(0), msgData[Integer.parseInt(matcher.group(1).trim())]);
        }
        WxMpTemplateMessage.MiniProgram miniProgram = new WxMpTemplateMessage.MiniProgram(appid, pagePath);
        wxMessageTemplate.setMiniProgram(miniProgram);

        if (MainWindow.mainWindow.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            Init.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) tableModel.getValueAt(i, 0)).trim();

            String value = ((String) tableModel.getValueAt(i, 1)).replaceAll("\\$ENTER\\$", "\n");
            p = Pattern.compile("\\{([^{}]+)\\}");
            matcher = p.matcher(value);
            while (matcher.find()) {
                value = value.replace(matcher.group(0), msgData[Integer.parseInt(matcher.group(1).trim())]);
            }

            p = Pattern.compile("\\$([^$]+)\\$");
            matcher = p.matcher(value);
            while (matcher.find()) {
                String str = matcher.group(0);
                if (str.startsWith("$NICK_NAME")) {
                    WxMpService wxMpService = getWxMpService();
                    String nickName = "";
                    try {
                        nickName = wxMpService.getUserService().userInfo(msgData[0]).getNickname();
                    } catch (WxErrorException e) {
                        e.printStackTrace();
                    }
                    value = value.replace(str, nickName);
                }
            }

            String color = ((String) tableModel.getValueAt(i, 2)).trim();
            WxMpTemplateData templateData = new WxMpTemplateData(name, value, color);
            wxMessageTemplate.addWxMpTemplateData(templateData);
        }

        return wxMessageTemplate;
    }

    /**
     * 组织客服消息
     *
     * @param msgData
     * @return
     */
    synchronized public static WxMpKefuMessage makeKefuMessage(String[] msgData) {

        WxMpKefuMessage kefuMessage = null;
        if ("图文消息".equals(MainWindow.mainWindow.getMsgKefuMsgTypeComboBox().getSelectedItem().toString())) {
            WxMpKefuMessage.WxArticle article = new WxMpKefuMessage.WxArticle();

            // 标题
            String title = MainWindow.mainWindow.getMsgKefuMsgTitleTextField().getText();
            Pattern p = Pattern.compile("\\{([^{}]+)\\}");
            Matcher matcher = p.matcher(title);
            while (matcher.find()) {
                title = title.replace(matcher.group(0), msgData[Integer.parseInt(matcher.group(1).trim())]);
            }

            p = Pattern.compile("\\$([^$]+)\\$");
            matcher = p.matcher(title);
            while (matcher.find()) {
                String str = matcher.group(0);
                if (str.startsWith("$NICK_NAME")) {
                    WxMpService wxMpService = getWxMpService();
                    String nickName = null;
                    try {
                        nickName = wxMpService.getUserService().userInfo(msgData[0]).getNickname();
                    } catch (WxErrorException e) {
                        e.printStackTrace();
                    }
                    title = title.replace(str, nickName);
                }
            }
            article.setTitle(title);

            // 图片url
            article.setPicUrl(MainWindow.mainWindow.getMsgKefuPicUrlTextField().getText());

            // 描述
            String description = MainWindow.mainWindow.getMsgKefuDescTextField().getText();
            p = Pattern.compile("\\{([^{}]+)\\}");
            matcher = p.matcher(description);
            while (matcher.find()) {
                description = description.replace(matcher.group(0), msgData[Integer.parseInt(matcher.group(1).trim())]);
            }

            p = Pattern.compile("\\$([^$]+)\\$");
            matcher = p.matcher(description);
            while (matcher.find()) {
                String str = matcher.group(0);
                if (str.startsWith("$NICK_NAME")) {
                    WxMpService wxMpService = getWxMpService();
                    String nickName = null;
                    try {
                        nickName = wxMpService.getUserService().userInfo(msgData[0]).getNickname();
                    } catch (WxErrorException e) {
                        e.printStackTrace();
                    }
                    description = description.replace(str, nickName);
                }
            }
            article.setDescription(description);

            // 跳转url
            article.setUrl(MainWindow.mainWindow.getMsgKefuUrlTextField().getText());

            kefuMessage = WxMpKefuMessage.NEWS().addArticle(article).build();
        } else if ("文本消息".equals(MainWindow.mainWindow.getMsgKefuMsgTypeComboBox().getSelectedItem().toString())) {
            String content = MainWindow.mainWindow.getMsgKefuMsgTitleTextField().getText();
            kefuMessage = WxMpKefuMessage.TEXT().content(content).build();
        }

        return kefuMessage;
    }

    /**
     * 组织阿里大于模板短信消息
     *
     * @param msgData
     * @return
     */
    synchronized public static AlibabaAliqinFcSmsNumSendRequest makeAliTemplateMessage(String[] msgData) {
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
            String key = (String) tableModel.getValueAt(i, 0);
            String value = ((String) tableModel.getValueAt(i, 1)).replaceAll("$ENTER$", "\n");
            Pattern p = Pattern.compile("\\{([^{}]+)\\}");
            Matcher matcher = p.matcher(value);
            while (matcher.find()) {
                value = value.replace(matcher.group(0), msgData[Integer.parseInt(matcher.group(1).trim())]);
            }

            paramMap.put(key, value);
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

            for (String[] str : PushData.sendSuccessList) {
                writer.writeNext(str);
            }
            writer.close();
        }

        // 保存未发送
        for (String[] str : PushData.sendSuccessList) {
            PushData.toSendList.remove(str);
        }
        for (String[] str : PushData.sendFailList) {
            PushData.toSendList.remove(str);
        }
        if (PushData.toSendList.size() > 0) {
            File unSendFile = new File(new StringBuilder("data/push_his/").append(msgName).append("-未发送-").append(nowTime).append(".csv").toString());
            if (!unSendFile.exists()) {
                unSendFile.createNewFile();
            }
            writer = new CSVWriter(new FileWriter(unSendFile));
            for (String[] str : PushData.toSendList) {
                writer.writeNext(str);
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
            for (String[] str : PushData.sendFailList) {
                writer.writeNext(str);
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
