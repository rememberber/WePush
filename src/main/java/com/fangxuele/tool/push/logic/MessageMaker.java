package com.fangxuele.tool.push.logic;

import cn.binarywang.wx.miniapp.bean.WxMaTemplateData;
import cn.binarywang.wx.miniapp.bean.WxMaTemplateMessage;
import cn.hutool.json.JSONUtil;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.http.MethodType;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.form.msg.AliTemplateMsgForm;
import com.fangxuele.tool.push.ui.form.msg.AliYunMsgForm;
import com.fangxuele.tool.push.ui.form.msg.KefuMsgForm;
import com.fangxuele.tool.push.ui.form.msg.MaTemplateMsgForm;
import com.fangxuele.tool.push.ui.form.msg.TxYunMsgForm;
import com.fangxuele.tool.push.ui.form.msg.YunpianMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.yunpian.sdk.YunpianClient;
import me.chanjar.weixin.mp.bean.kefu.WxMpKefuMessage;
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
     * 组织模板消息-小程序
     *
     * @param msgData 消息信息
     * @return WxMaTemplateMessage
     */
    public synchronized static WxMaTemplateMessage makeMaTemplateMessage(String[] msgData) {
        // 拼模板
        WxMaTemplateMessage wxMessageTemplate = WxMaTemplateMessage.builder().build();
        wxMessageTemplate.setTemplateId(MaTemplateMsgForm.maTemplateMsgForm.getMsgTemplateIdTextField().getText().trim());
        wxMessageTemplate.setPage(MaTemplateMsgForm.maTemplateMsgForm.getMsgTemplateUrlTextField().getText().trim());
        wxMessageTemplate.setEmphasisKeyword(MaTemplateMsgForm.maTemplateMsgForm.getMsgTemplateKeyWordTextField().getText().trim() + ".DATA");

        if (MaTemplateMsgForm.maTemplateMsgForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            MaTemplateMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MaTemplateMsgForm.maTemplateMsgForm.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();

        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushControl.TEMPLATE_VAR_PREFIX + i, msgData[i]);
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
    public synchronized static WxMpKefuMessage makeKefuMessage(String[] msgData) {

        WxMpKefuMessage kefuMessage = null;
        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushControl.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }
        if ("图文消息".equals(Objects.requireNonNull(KefuMsgForm.kefuMsgForm.getMsgKefuMsgTypeComboBox().getSelectedItem()).toString())) {
            WxMpKefuMessage.WxArticle article = new WxMpKefuMessage.WxArticle();

            // 标题
            String title = KefuMsgForm.kefuMsgForm.getMsgKefuMsgTitleTextField().getText();
            title = TemplateUtil.evaluate(title, velocityContext);
            article.setTitle(title);

            // 图片url
            article.setPicUrl(KefuMsgForm.kefuMsgForm.getMsgKefuPicUrlTextField().getText());

            // 描述
            String description = KefuMsgForm.kefuMsgForm.getMsgKefuDescTextField().getText();
            description = TemplateUtil.evaluate(description, velocityContext);
            article.setDescription(description);

            // 跳转url
            article.setUrl(KefuMsgForm.kefuMsgForm.getMsgKefuUrlTextField().getText());

            kefuMessage = WxMpKefuMessage.NEWS().addArticle(article).build();
        } else if ("文本消息".equals(KefuMsgForm.kefuMsgForm.getMsgKefuMsgTypeComboBox().getSelectedItem().toString())) {
            String content = KefuMsgForm.kefuMsgForm.getMsgKefuMsgTitleTextField().getText();
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
    public synchronized static SendSmsRequest makeAliyunMessage(String[] msgData) {
        SendSmsRequest request = new SendSmsRequest();
        //使用post提交
        request.setSysMethod(MethodType.POST);
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(App.config.getAliyunSign());

        // 模板参数
        Map<String, String> paramMap = new HashMap<String, String>();

        if (AliYunMsgForm.aliYunMsgForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            AliYunMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) AliYunMsgForm.aliYunMsgForm.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();

        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushControl.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }
        for (int i = 0; i < rowCount; i++) {
            String key = (String) tableModel.getValueAt(i, 0);
            String value = ((String) tableModel.getValueAt(i, 1));
            value = TemplateUtil.evaluate(value, velocityContext);

            paramMap.put(key, value);
        }

        request.setTemplateParam(JSONUtil.parseFromMap(paramMap).toJSONString(0));

        // 短信模板ID，传入的模板必须是在阿里阿里云短信中的可用模板。示例：SMS_585014
        request.setTemplateCode(AliYunMsgForm.aliYunMsgForm.getMsgTemplateIdTextField().getText());

        return request;
    }

    /**
     * 组织阿里大于模板短信消息
     *
     * @param msgData 消息信息
     * @return AlibabaAliqinFcSmsNumSendRequest
     */
    public synchronized static AlibabaAliqinFcSmsNumSendRequest makeAliTemplateMessage(String[] msgData) {
        AlibabaAliqinFcSmsNumSendRequest request = new AlibabaAliqinFcSmsNumSendRequest();
        // 用户可以根据该会员ID识别是哪位会员使用了你的应用
        request.setExtend("WePush");
        // 短信类型，传入值请填写normal
        request.setSmsType("normal");

        // 模板参数
        Map<String, String> paramMap = new HashMap<String, String>();

        if (AliTemplateMsgForm.aliTemplateMsgForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            AliTemplateMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) AliTemplateMsgForm.aliTemplateMsgForm.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushControl.TEMPLATE_VAR_PREFIX + i, msgData[i]);
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
        request.setSmsFreeSignName(App.config.getAliSign());
        // 短信模板ID，传入的模板必须是在阿里大鱼“管理中心-短信模板管理”中的可用模板。示例：SMS_585014
        request.setSmsTemplateCode(AliTemplateMsgForm.aliTemplateMsgForm.getMsgTemplateIdTextField().getText());

        return request;
    }

    /**
     * 组织腾讯云短信消息
     *
     * @param msgData 消息信息
     * @return String[]
     */
    public synchronized static String[] makeTxyunMessage(String[] msgData) {
        if (TxYunMsgForm.txYunMsgForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            TxYunMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) TxYunMsgForm.txYunMsgForm.getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        String[] params = new String[rowCount];

        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushControl.TEMPLATE_VAR_PREFIX + i, msgData[i]);
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
    public synchronized static Map<String, String> makeYunpianMessage(String[] msgData) {
        Map<String, String> params = new HashMap<String, String>(2);

        VelocityContext velocityContext = new VelocityContext();
        for (int i = 0; i < msgData.length; i++) {
            velocityContext.put(PushControl.TEMPLATE_VAR_PREFIX + i, msgData[i]);
        }

        String text = YunpianMsgForm.yunpianMsgForm.getMsgYunpianMsgContentTextField().getText();
        text = TemplateUtil.evaluate(text, velocityContext);

        params.put(YunpianClient.TEXT, text);
        return params;
    }
}