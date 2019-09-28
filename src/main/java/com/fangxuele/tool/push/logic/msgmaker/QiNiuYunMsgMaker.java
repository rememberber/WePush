package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.ui.form.msg.QiNiuYunMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.google.common.collect.Maps;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.Map;

/**
 * <pre>
 * 七牛云模板短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
public class QiNiuYunMsgMaker extends BaseMsgMaker implements IMsgMaker {

    public static String templateId;

    public static Map<String, String> paramMap;

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        templateId = QiNiuYunMsgForm.getInstance().getMsgTemplateIdTextField().getText();

        if (QiNiuYunMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            QiNiuYunMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) QiNiuYunMsgForm.getInstance().getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        paramMap = Maps.newHashMap();
        for (int i = 0; i < rowCount; i++) {
            String key = ((String) tableModel.getValueAt(i, 0));
            String value = ((String) tableModel.getValueAt(i, 1));
            paramMap.put(key, value);
        }
    }

    /**
     * 组织七牛云短信消息
     *
     * @param msgData 消息信息
     * @return String[]
     */
    @Override
    public Map<String, String> makeMsg(String[] msgData) {

        VelocityContext velocityContext = getVelocityContext(msgData);
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            entry.setValue(TemplateUtil.evaluate(entry.getValue(), velocityContext));
        }
        return paramMap;
    }
}
