package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgSms;
import com.fangxuele.tool.push.ui.form.msg.BdYunMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 百度云模板短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
@Getter
public class BdYunMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String templateId;

    private Map<String, String> paramMap;

    public BdYunMsgMaker(TMsg tMsg) {
        TMsgSms tMsgSms = JSON.parseObject(tMsg.getContent(), TMsgSms.class);
        templateId = tMsgSms.getTemplateId();
        paramMap = new HashMap<>();
        for (TemplateData templateData : tMsgSms.getTemplateDataList()) {
            paramMap.put(templateData.getName(), templateData.getValue());
        }
    }

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        templateId = BdYunMsgForm.getInstance().getMsgTemplateIdTextField().getText();

        if (BdYunMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            BdYunMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) BdYunMsgForm.getInstance().getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        paramMap = Maps.newHashMap();
        for (int i = 0; i < rowCount; i++) {
            String key = ((String) tableModel.getValueAt(i, 0));
            String value = ((String) tableModel.getValueAt(i, 1));
            paramMap.put(key, value);
        }
    }

    /**
     * 组织百度云短信消息
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
