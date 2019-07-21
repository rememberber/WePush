package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.ui.form.msg.YunpianMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.yunpian.sdk.YunpianClient;
import org.apache.velocity.VelocityContext;

import java.util.HashMap;
import java.util.Map;

/**
 * <pre>
 * 云片网短信加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/14.
 */
public class YunPianMsgMaker extends BaseMsgMaker implements IMsgMaker {

    public static String msgYunpianMsgContent;

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        msgYunpianMsgContent = YunpianMsgForm.getInstance().getMsgYunpianMsgContentTextField().getText();
    }

    /**
     * 组织云片网短信消息
     *
     * @param msgData 消息信息
     * @return Map
     */
    @Override
    public Map<String, String> makeMsg(String[] msgData) {
        Map<String, String> params = new HashMap<>(2);
        VelocityContext velocityContext = getVelocityContext(msgData);
        String text = TemplateUtil.evaluate(msgYunpianMsgContent, velocityContext);
        params.put(YunpianClient.TEXT, text);
        return params;
    }
}
