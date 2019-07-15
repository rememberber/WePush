package com.fangxuele.tool.push.logic.msgmaker;

import cn.hutool.json.JSONUtil;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.http.MethodType;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.ui.form.msg.AliYunMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.commons.compress.utils.Lists;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 阿里云模板短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
public class AliyunMsgMaker extends BaseMsgMaker implements IMsgMaker {

    public static String templateId;

    public static List<TemplateData> templateDataList;

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        templateId = AliYunMsgForm.getInstance().getMsgTemplateIdTextField().getText();

        if (AliYunMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            AliYunMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) AliYunMsgForm.getInstance().getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        TemplateData templateData;
        templateDataList = Lists.newArrayList();
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) tableModel.getValueAt(i, 0)).trim();
            String value = ((String) tableModel.getValueAt(i, 1)).trim();
            templateData = new TemplateData();
            templateData.setName(name);
            templateData.setValue(value);
            templateDataList.add(templateData);
        }

    }

    /**
     * 组织阿里云短信消息
     *
     * @param msgData 消息信息
     * @return SendSmsRequest
     */
    @Override
    public SendSmsRequest makeMsg(String[] msgData) {
        SendSmsRequest request = new SendSmsRequest();
        //使用post提交
        request.setSysMethod(MethodType.POST);
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(App.config.getAliyunSign());

        // 模板参数
        Map<String, String> paramMap = new HashMap<>(10);
        VelocityContext velocityContext = getVelocityContext(msgData);

        for (TemplateData templateData : templateDataList) {
            paramMap.put(templateData.getName(), TemplateUtil.evaluate(templateData.getValue(), velocityContext));
        }

        request.setTemplateParam(JSONUtil.parseFromMap(paramMap).toJSONString(0));

        // 短信模板ID，传入的模板必须是在阿里阿里云短信中的可用模板。示例：SMS_585014
        request.setTemplateCode(templateId);

        return request;
    }
}
