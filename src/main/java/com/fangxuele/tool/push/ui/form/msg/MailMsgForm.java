package com.fangxuele.tool.push.ui.form.msg;

import com.fangxuele.tool.push.dao.TMsgMailMapper;
import com.fangxuele.tool.push.domain.TMsgMail;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * <pre>
 * MailMsgForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/6/23.
 */
@Getter
public class MailMsgForm implements IMsgForm {

    public static MailMsgForm mailMsgForm = new MailMsgForm();
    private JPanel mailPanel;
    private JTextField mailTitleTextField;
    private JTextField mailFilesTextField;
    private JEditorPane mailContentPane;
    private JButton fileExploreButton;
    private JLabel uEditorLabel;
    private JTextField mailCcTextField;

    private static TMsgMailMapper msgMailMapper = MybatisUtil.getSqlSession().getMapper(TMsgMailMapper.class);

    public MailMsgForm() {
        fileExploreButton.addActionListener(e -> {
            File beforeFile = new File(mailFilesTextField.getText());
            JFileChooser fileChooser;

            if (beforeFile.exists()) {
                fileChooser = new JFileChooser(beforeFile);
            } else {
                fileChooser = new JFileChooser();
            }

            int approve = fileChooser.showOpenDialog(MessageEditForm.messageEditForm.getMsgEditorPanel());
            if (approve == JFileChooser.APPROVE_OPTION) {
                mailFilesTextField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        uEditorLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("https://ueditor.baidu.com/website/onlinedemo.html"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
                super.mousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
                super.mouseEntered(e);
            }
        });
    }

    @Override
    public void init(String msgName) {
        clearAllField();
        List<TMsgMail> tMsgMailList = msgMailMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.EMAIL_CODE, msgName);
        if (tMsgMailList.size() > 0) {
            TMsgMail tMsgMail = tMsgMailList.get(0);
            mailMsgForm.getMailTitleTextField().setText(tMsgMail.getTitle());
            mailMsgForm.getMailCcTextField().setText(tMsgMail.getCc());
            mailMsgForm.getMailFilesTextField().setText(tMsgMail.getFiles());
            mailMsgForm.getMailContentPane().setText(tMsgMail.getContent());
        }
    }

    @Override
    public void save(String msgName) {
        boolean existSameMsg = false;

        List<TMsgMail> tMsgMailList = msgMailMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.EMAIL_CODE, msgName);
        if (tMsgMailList.size() > 0) {
            existSameMsg = true;
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }
        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String mailTitle = mailMsgForm.getMailTitleTextField().getText();
            String mailCc = mailMsgForm.getMailCcTextField().getText();
            String mailFiles = mailMsgForm.getMailFilesTextField().getText();
            String mailContent = mailMsgForm.getMailContentPane().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsgMail tMsgMail = new TMsgMail();
            tMsgMail.setMsgType(MessageTypeEnum.EMAIL_CODE);
            tMsgMail.setMsgName(msgName);
            tMsgMail.setTitle(mailTitle);
            tMsgMail.setCc(mailCc);
            tMsgMail.setFiles(mailFiles);
            tMsgMail.setContent(mailContent);
            tMsgMail.setCreateTime(now);
            tMsgMail.setModifiedTime(now);

            if (existSameMsg) {
                msgMailMapper.updateByMsgTypeAndMsgName(tMsgMail);
            } else {
                msgMailMapper.insertSelective(tMsgMail);
            }

            JOptionPane.showMessageDialog(MainWindow.mainWindow.getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        mailMsgForm.getMailTitleTextField().setText("");
        mailMsgForm.getMailCcTextField().setText("");
        mailMsgForm.getMailFilesTextField().setText("");
        mailMsgForm.getMailContentPane().setText("");
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mailPanel = new JPanel();
        mailPanel.setLayout(new GridLayoutManager(5, 4, new Insets(8, 8, 8, 8), -1, -1));
        mailPanel.setMinimumSize(new Dimension(-1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("邮件标题");
        mailPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("附件");
        mailPanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mailTitleTextField = new JTextField();
        mailPanel.add(mailTitleTextField, new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailContentPane = new JEditorPane();
        mailContentPane.setBackground(new Color(-12236470));
        mailContentPane.setContentType("text/html");
        mailContentPane.setText("<html>\r\n  <head>\r\n    \r\n  </head>\r\n  <body>\r\n  </body>\r\n</html>\r\n");
        mailPanel.add(mailContentPane, new GridConstraints(4, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("邮件正文(HTML)");
        mailPanel.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        uEditorLabel = new JLabel();
        uEditorLabel.setText("<html><a href=\"https://ueditor.baidu.com/website/onlinedemo.html\">使用UEditor编辑HTML</a></html>");
        mailPanel.add(uEditorLabel, new GridConstraints(3, 1, 1, 3, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileExploreButton = new JButton();
        fileExploreButton.setHorizontalAlignment(0);
        fileExploreButton.setText("浏览");
        mailPanel.add(fileExploreButton, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mailFilesTextField = new JTextField();
        mailPanel.add(mailFilesTextField, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailCcTextField = new JTextField();
        mailPanel.add(mailCcTextField, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("抄送");
        mailPanel.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mailPanel;
    }

}
