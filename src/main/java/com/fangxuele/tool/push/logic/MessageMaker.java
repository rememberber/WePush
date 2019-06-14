package com.fangxuele.tool.push.logic;

import com.fangxuele.tool.push.ui.form.msg.TxYunMsgForm;
import com.fangxuele.tool.push.ui.form.msg.YunpianMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
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