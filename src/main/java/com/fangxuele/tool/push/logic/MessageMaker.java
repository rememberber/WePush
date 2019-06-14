package com.fangxuele.tool.push.logic;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.form.msg.AliTemplateMsgForm;
import com.fangxuele.tool.push.ui.form.msg.TxYunMsgForm;
import com.fangxuele.tool.push.ui.form.msg.YunpianMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import com.yunpian.sdk.YunpianClient;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.Map;

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