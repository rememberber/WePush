package com.fangxuele.tool.push.ui.form.account;

import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.MessageTypeForm;
import com.fangxuele.tool.push.ui.form.msg.AliYunMsgForm;
import com.fangxuele.tool.push.ui.form.msg.KsYunMsgForm;
import org.junit.Test;

import javax.swing.*;
import static org.junit.Assert.*;

public class KsYunAccountFormTest {
    @Test
    public void channelEntryAndEditorsKeepAccountsAndTemplatesSeparate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("金山云短信", MessageTypeEnum.getName(27));
            MessageTypeForm types = MessageTypeForm.getInstance();
            assertEquals("金山云短信", types.getKsYunRadioButton().getText());
            types.getMessageTypePanel().setSize(800, 800);
            types.getMessageTypePanel().doLayout();
            assertNotNull(types.getAliYunRadioButton().getParent());
            KsYunAccountForm config = KsYunAccountForm.getInstance();
            config.getSignTextField().setText("签名");
            config.clear();
            assertEquals("", config.getSignTextField().getText());
            assertEquals("cn-beijing-6", config.getRegionComboBox().getSelectedItem());
            AliYunMsgForm aliyun = AliYunMsgForm.getInstance();
            KsYunMsgForm ksyun = KsYunMsgForm.getInstance();
            aliyun.getMsgTemplateIdTextField().setText("SMS_1");
            ksyun.getMsgTemplateIdTextField().setText("123");
            ksyun.clearAllField();
            assertEquals("SMS_1", aliyun.getMsgTemplateIdTextField().getText());
            ksyun.getTemplateDataNameTextField().setText("name");
            ksyun.getTemplateDataValueTextField().setText("${var1}");
            ksyun.getTemplateMsgDataAddButton().doClick();
            assertEquals("name", ksyun.getTemplateMsgDataTable().getValueAt(0, 0));
            assertEquals(0, aliyun.getTemplateMsgDataTable().getRowCount());
        });
    }
}
