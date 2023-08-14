package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.TemplateData;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgMpSubscribe;
import com.fangxuele.tool.push.ui.form.msg.MpSubscribeMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import me.chanjar.weixin.mp.bean.subscribe.WxMpSubscribeMessage;
import org.apache.commons.compress.utils.Lists;
import org.apache.velocity.VelocityContext;

import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 公众号订阅通知加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2021/3/23.
 */
public class WxMpSubscribeMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String templateId;

    private String templateUrl;

    private String miniAppId;

    private String miniAppPagePath;

    private List<TemplateData> templateDataList;

    public WxMpSubscribeMsgMaker(TMsg tMsg) {
        TMsgMpSubscribe tMsgMpSubscribe = JSON.parseObject(tMsg.getContent(), TMsgMpSubscribe.class);
        this.templateId = tMsgMpSubscribe.getTemplateId();
        this.templateUrl = tMsgMpSubscribe.getUrl();
        this.miniAppId = tMsgMpSubscribe.getMaAppid();
        this.miniAppPagePath = tMsgMpSubscribe.getMaPagePath();
        this.templateDataList = tMsgMpSubscribe.getTemplateDataList();
    }

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        templateId = MpSubscribeMsgForm.getInstance().getMsgTemplateIdTextField().getText().trim();
        templateUrl = MpSubscribeMsgForm.getInstance().getMsgTemplateUrlTextField().getText().trim();
        miniAppId = MpSubscribeMsgForm.getInstance().getMsgTemplateMiniAppidTextField().getText().trim();
        miniAppPagePath = MpSubscribeMsgForm.getInstance().getMsgTemplateMiniPagePathTextField().getText().trim();

        if (MpSubscribeMsgForm.getInstance().getTemplateMsgDataTable().getModel().getRowCount() == 0) {
            MpSubscribeMsgForm.initTemplateDataTable();
        }

        DefaultTableModel tableModel = (DefaultTableModel) MpSubscribeMsgForm.getInstance().getTemplateMsgDataTable().getModel();
        int rowCount = tableModel.getRowCount();
        TemplateData templateData;
        templateDataList = Lists.newArrayList();
        for (int i = 0; i < rowCount; i++) {
            String name = ((String) tableModel.getValueAt(i, 0)).trim();
            String value = ((String) tableModel.getValueAt(i, 1)).trim();
            String color = ((String) tableModel.getValueAt(i, 2)).trim();
            templateData = new TemplateData();
            templateData.setName(name);
            templateData.setValue(value);
            templateData.setColor(color);
            templateDataList.add(templateData);
        }

    }

    /**
     * 组织订阅通知-公众号
     *
     * @param msgData 消息数据
     * @return WxMpSubscribeMessage
     */
    @Override
    public WxMpSubscribeMessage makeMsg(String[] msgData) {
        // 拼模板
        WxMpSubscribeMessage wxMessageTemplate = new WxMpSubscribeMessage();
        wxMessageTemplate.setTemplateId(templateId);

        VelocityContext velocityContext = getVelocityContext(msgData);
        String templateUrlEvaluated = TemplateUtil.evaluate(templateUrl, velocityContext);
        wxMessageTemplate.setPage(templateUrlEvaluated);
        String miniAppPagePathEvaluated = TemplateUtil.evaluate(miniAppPagePath, velocityContext);
        WxMpSubscribeMessage.MiniProgram miniProgram = new WxMpSubscribeMessage.MiniProgram(miniAppId, miniAppPagePathEvaluated, false);
        wxMessageTemplate.setMiniProgram(miniProgram);

        Map<String, String> dataMap = new HashMap<>(10);
        for (TemplateData templateData : templateDataList) {
            dataMap.put(templateData.getName(), TemplateUtil.evaluate(templateData.getValue(), velocityContext));
        }

        wxMessageTemplate.setDataMap(dataMap);

        return wxMessageTemplate;
    }
}
