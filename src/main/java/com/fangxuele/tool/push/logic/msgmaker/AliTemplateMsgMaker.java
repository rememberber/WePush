package com.fangxuele.tool.push.logic.msgmaker;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.ui.form.msg.AliTemplateMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.taobao.api.request.AlibabaAliqinFcSmsNumSendRequest;
import org.apache.commons.compress.utils.Lists;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 阿里大于模板短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
public class AliTemplateMsgMaker extends BaseMsgMaker implements IMsgMaker{

    public static String templateId;

    public static List<TemplateData> templateDataList;

    /**
     * 准备(界面字段等)
     */
    public static void prepare() {
        templateId = AliTemplateMsgForm.aliTemplateMsgForm.getMsgTemplateIdTextField().getText();

        if (AliTemplateMsgForm.aliTemplateMsgForm.getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            AliTemplateMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) AliTemplateMsgForm.aliTemplateMsgForm.getTemplateMsgDataTable().getModel();
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
     * 组织阿里大于模板短信消息
     *
     * @param msgData 消息信息
     * @return AlibabaAliqinFcSmsNumSendRequest
     */
    @Override
    public AlibabaAliqinFcSmsNumSendRequest makeMsg(String[] msgData) {
        AlibabaAliqinFcSmsNumSendRequest request = new AlibabaAliqinFcSmsNumSendRequest();
        // 用户可以根据该会员ID识别是哪位会员使用了你的应用
        request.setExtend("WePush");
        // 短信类型，传入值请填写normal
        request.setSmsType("normal");

        // 模板参数
        Map<String, String> paramMap = new HashMap<String, String>();
        VelocityContext velocityContext = getVelocityContext(msgData);

        for (TemplateData templateData : templateDataList) {
            paramMap.put(templateData.getName(), TemplateUtil.evaluate(templateData.getValue(), velocityContext));
        }

        request.setSmsParamString(JSONUtil.parseFromMap(paramMap).toJSONString(0));

        // 短信签名，传入的短信签名必须是在阿里大鱼“管理中心-短信签名管理”中的可用签名。如“阿里大鱼”已在短信签名管理中通过审核，
        // 则可传入”阿里大鱼“（传参时去掉引号）作为短信签名。短信效果示例：【阿里大鱼】欢迎使用阿里大鱼服务。
        request.setSmsFreeSignName(App.config.getAliSign());
        // 短信模板ID，传入的模板必须是在阿里大鱼“管理中心-短信模板管理”中的可用模板。示例：SMS_585014
        request.setSmsTemplateCode(templateId);

        return request;
    }
}
