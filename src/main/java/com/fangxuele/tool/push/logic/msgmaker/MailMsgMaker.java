package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.msg.MailMsg;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgMail;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * E-Mail加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/23.
 */
public class MailMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String mailTitle;
    private String mailCc;
    private List<File> mailFiles;
    private String mailContent;

    public MailMsgMaker(TMsg tMsg) {
        TMsgMail tMsgMail = JSON.parseObject(tMsg.getContent(), TMsgMail.class);
        mailTitle = tMsgMail.getTitle();
        mailCc = tMsgMail.getCc();
        List<File> files = new ArrayList<>();
        String text = tMsgMail.getFiles();
        String[] strings = text.split("\\n");
        for (String string : strings) {
            string = string.trim();
            if (StringUtils.isNotEmpty(string)) {
                files.add(new File(string));
            }
        }
        mailFiles = files;
        mailContent = tMsgMail.getContent();
    }

    /**
     * 组织E-Mail消息
     *
     * @param msgData 消息信息
     * @return MailMsg
     */
    @Override
    public MailMsg makeMsg(String[] msgData) {
        MailMsg mailMsg = new MailMsg();
        VelocityContext velocityContext = getVelocityContext(msgData);
        String title = TemplateUtil.evaluate(mailTitle, velocityContext);
        String cc = TemplateUtil.evaluate(mailCc, velocityContext);
        String content = TemplateUtil.evaluate(mailContent, velocityContext);
        mailMsg.setMailTitle(title);
        mailMsg.setMailCc(cc);
        mailMsg.setMailFiles(mailFiles);
        mailMsg.setMailContent(content);
        return mailMsg;
    }
}
