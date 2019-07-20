package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.ui.form.msg.TxYunMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.commons.compress.utils.Lists;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.List;

/**
 * <pre>
 * 腾讯云模板短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
public class TxYunMsgMaker extends BaseMsgMaker implements IMsgMaker{

    public static int templateId;

    public static List<String> paramList;

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        templateId = Integer.parseInt(TxYunMsgForm.getInstance().getMsgTemplateIdTextField().getText());

        if (TxYunMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            TxYunMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) TxYunMsgForm.getInstance().getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        paramList = Lists.newArrayList();
        for (int i = 0; i < rowCount; i++) {
            String value = ((String) tableModel.getValueAt(i, 1));
            paramList.add(value);
        }
    }

    /**
     * 组织腾讯云短信消息
     *
     * @param msgData 消息信息
     * @return String[]
     */
    @Override
    public String[] makeMsg(String[] msgData) {

        VelocityContext velocityContext = getVelocityContext(msgData);
        for (int i = 0; i < paramList.size(); i++) {
            paramList.set(i, TemplateUtil.evaluate(paramList.get(i), velocityContext));
        }
        String[] paramArray = new String[paramList.size()];
        return paramList.toArray(paramArray);
    }
}
