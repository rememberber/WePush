package com.fangxuele.tool.push.logic;

import cn.binarywang.wx.miniapp.bean.WxMaTemplateData;
import cn.binarywang.wx.miniapp.bean.WxMaTemplateMessage;
import cn.hutool.json.JSONUtil;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.http.MethodType;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.yunpian.sdk.YunpianClient;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateData;
import me.chanjar.weixin.mp.bean.template.WxMpTemplateMessage;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <pre>
 * 组织各种类型的消息内容
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/3/26.
 */
public class MessageMaker {
    /**
     * 组织模板消息-公众号
     *
     * @param msgData 消息数据
     * @return WxMpTemplateMessage
     */
    synchronized static WxMpTemplateMessage makeMpTemplateMessage(String[] msgData) {
        // 拼模板
        WxMpTemplateMessage wxMessageTemplate = WxMpTemplateMessage.builder().build();
        wxMessageTemplate.setTemplateId(MessageEditForm.messageEditForm.getMsgTemplateIdTextField().getText().trim());
        wxMessageTemplate.setUrl(MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().getText().trim());

        String appid = MessageEditForm.messageEditForm.getMsgTemplateMiniAppidTextField().getText().trim();
        String pagePath = MessageEditForm.messageEditForm.getMsgTemplateMiniPagePathTextField().getText().trim();

        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushManage.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }
        pagePath = TemplateUtil.evaluate(pagePath, velocityContext);
        WxMpTemplateMessage.MiniProgram miniProgram = new WxMpTemplateMessage.MiniProgram(appid, pagePath, true);
        wxMessageTemplate.setMiniProgram(miniProgram);

        if (MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            Init.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) tableModel.getValueAt(i, 0)).trim();

            String value = ((String) tableModel.getValueAt(i, 1));
            value = TemplateUtil.evaluate(value, velocityContext);

            String color = ((String) tableModel.getValueAt(i, 2)).trim();
            WxMpTemplateData templateData = new WxMpTemplateData(name, value, color);
            wxMessageTemplate.addData(templateData);
        }

        return wxMessageTemplate;
    }

    /**
     * 组织模板消息-小程序
     *
     * @param msgData 消息信息
     * @return WxMaTemplateMessage
     */
    synchronized static WxMaTemplateMessage makeMaTemplateMessage(String[] msgData) {
        // 拼模板
        WxMaTemplateMessage wxMessageTemplate = WxMaTemplateMessage.builder().build();
        wxMessageTemplate.setTemplateId(MessageEditForm.messageEditForm.getMsgTemplateIdTextField().getText().trim());
        wxMessageTemplate.setPage(MessageEditForm.messageEditForm.getMsgTemplateUrlTextField().getText().trim());
        wxMessageTemplate.setEmphasisKeyword(MessageEditForm.messageEditForm.getMsgTemplateKeyWordTextField().getText().trim() + ".DATA");

        if (MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            Init.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();

        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushManage.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) tableModel.getValueAt(i, 0)).trim();

            String value = ((String) tableModel.getValueAt(i, 1));
            value = TemplateUtil.evaluate(value, velocityContext);

            String color = ((String) tableModel.getValueAt(i, 2)).trim();
            WxMaTemplateData templateData = new WxMaTemplateData(name, value, color);
            wxMessageTemplate.addData(templateData);
        }

        return wxMessageTemplate;
    }

    /**
     * 组织客服消息
     *
     * @param msgData 消息信息
     * @return WxMpKefuMessage
     */
    synchronized static WxMpKefuMessage makeKefuMessage(String[] msgData) {

        WxMpKefuMessage kefuMessage = null;
        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushManage.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }
        if ("图文消息".equals(Objects.requireNonNull(MessageEditForm.messageEditForm.getMsgKefuMsgTypeComboBox().getSelectedItem()).toString())) {
            WxMpKefuMessage.WxArticle article = new WxMpKefuMessage.WxArticle();

            // 标题
            String title = MessageEditForm.messageEditForm.getMsgKefuMsgTitleTextField().getText();
            title = TemplateUtil.evaluate(title, velocityContext);
            article.setTitle(title);

            // 图片url
            article.setPicUrl(MessageEditForm.messageEditForm.getMsgKefuPicUrlTextField().getText());

            // 描述
            String description = MessageEditForm.messageEditForm.getMsgKefuDescTextField().getText();
            description = TemplateUtil.evaluate(description, velocityContext);
            article.setDescription(description);

            // 跳转url
            article.setUrl(MessageEditForm.messageEditForm.getMsgKefuUrlTextField().getText());

            kefuMessage = WxMpKefuMessage.NEWS().addArticle(article).build();
        } else if ("文本消息".equals(MessageEditForm.messageEditForm.getMsgKefuMsgTypeComboBox().getSelectedItem().toString())) {
            String content = MessageEditForm.messageEditForm.getMsgKefuMsgTitleTextField().getText();
            content = TemplateUtil.evaluate(content, velocityContext);
            kefuMessage = WxMpKefuMessage.TEXT().content(content).build();
        }

        return kefuMessage;
    }

    /**
     * 组织阿里云短信消息
     *
     * @param msgData 消息信息
     * @return SendSmsRequest
     */
    synchronized static SendSmsRequest makeAliyunMessage(String[] msgData) {
        SendSmsRequest request = new SendSmsRequest();
        //使用post提交
        request.setMethod(MethodType.POST);
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(Init.configer.getAliyunSign());

        // 模板参数
        Map<String, String> paramMap = new HashMap<String, String>();

        if (MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            Init.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();

        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushManage.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }
        for (int i = 0; i < rowCount; i++) {
            String key = (String) tableModel.getValueAt(i, 0);
            String value = ((String) tableModel.getValueAt(i, 1));
            value = TemplateUtil.evaluate(value, velocityContext);

            paramMap.put(key, value);
        }

        request.setTemplateParam(JSONUtil.parseFromMap(paramMap).toJSONString(0));

        // 短信模板ID，传入的模板必须是在阿里阿里云短信中的可用模板。示例：SMS_585014
        request.setTemplateCode(MessageEditForm.messageEditForm.getMsgTemplateIdTextField().getText());

        return request;
    }

    /**
     * 组织阿里大于模板短信消息
     *
     * @param msgData 消息信息
     * @return AlibabaAliqinFcSmsNumSendRequest
     */
    synchronized static AlibabaAliqinFcSmsNumSendRequest makeAliTemplateMessage(String[] msgData) {
        AlibabaAliqinFcSmsNumSendRequest request = new AlibabaAliqinFcSmsNumSendRequest();
        // 用户可以根据该会员ID识别是哪位会员使用了你的应用
        request.setExtend("WePush");
        // 短信类型，传入值请填写normal
        request.setSmsType("normal");

        // 模板参数
        Map<String, String> paramMap = new HashMap<String, String>();

        if (MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            Init.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushManage.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }
        for (int i = 0; i < rowCount; i++) {
            String key = (String) tableModel.getValueAt(i, 0);
            String value = ((String) tableModel.getValueAt(i, 1));
            value = TemplateUtil.evaluate(value, velocityContext);

            paramMap.put(key, value);
        }

        request.setSmsParamString(JSONUtil.parseFromMap(paramMap).toJSONString(0));

        // 短信签名，传入的短信签名必须是在阿里大鱼“管理中心-短信签名管理”中的可用签名。如“阿里大鱼”已在短信签名管理中通过审核，
        // 则可传入”阿里大鱼“（传参时去掉引号）作为短信签名。短信效果示例：【阿里大鱼】欢迎使用阿里大鱼服务。
        request.setSmsFreeSignName(Init.configer.getAliSign());
        // 短信模板ID，传入的模板必须是在阿里大鱼“管理中心-短信模板管理”中的可用模板。示例：SMS_585014
        request.setSmsTemplateCode(MessageEditForm.messageEditForm.getMsgTemplateIdTextField().getText());

        return request;
    }

    /**
     * 组织腾讯云短信消息
     *
     * @param msgData 消息信息
     * @return String[]
     */
    synchronized static String[] makeTxyunMessage(String[] msgData) {
        if (MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            Init.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MessageEditForm.messageEditForm.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        String[] params = new String[rowCount];

        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushManage.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }
        for (int i = 0; i < rowCount; i++) {
            String value = ((String) tableModel.getValueAt(i, 1));
            value = TemplateUtil.evaluate(value, velocityContext);

            params[i] = value;
        }

        return params;
    }

    /**
     * 组织云片网短信消息
     *
     * @param msgData 消息信息
     * @return Map
     */
    synchronized static Map<String, String> makeYunpianMessage(String[] msgData) {
        Map<String, String> params = new HashMap<String, String>(2);

        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushManage.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }

        String text = MessageEditForm.messageEditForm.getMsgYunpianMsgContentTextField().getText();
        text = TemplateUtil.evaluate(text, velocityContext);

        params.put(YunpianClient.TEXT, text);
        return params;
    }
}