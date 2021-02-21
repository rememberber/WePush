package com.fangxuele.tool.push.logic.msgmaker;

import com.fangxuele.tool.push.bean.MailMsg;
import com.fangxuele.tool.push.ui.form.msg.MailMsgForm;
import com.fangxuele.tool.push.util.TemplateUtil;
import org.apache.velocity.VelocityContext;

import java.io.File;
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

    public static String mailTitle;
    public static String mailCc;
    public static List<File> mailFiles;
    public static String mailContent;

    /**
     * 准备(界面字段等)
     */
    @Override
    public void prepare() {
        mailTitle = MailMsgForm.getInstance().getMailTitleTextField().getText();
        mailCc = MailMsgForm.getInstance().getMailCcTextField().getText();
        mailFiles = MailMsgForm.getInstance().getAttachmentFiles();
        mailContent = MailMsgForm.getInstance().getMailContentPane().getText();
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
