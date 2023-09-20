package com.fangxuele.tool.push.ui.form.msg;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.dao.TMsgMapper;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgMail;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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

    private JPanel mailPanel;
    private JTextField mailTitleTextField;
    private JEditorPane mailContentPane;
    private JButton fileExploreButton;
    private JLabel uEditorLabel;
    private JTextField mailCcTextField;
    private JTextArea mailFilesTextArea;

    private static MailMsgForm mailMsgForm;
    private static TMsgMapper msgMapper = MybatisUtil.getSqlSession().getMapper(TMsgMapper.class);

    public MailMsgForm() {
        fileExploreButton.addActionListener(e -> {
            JFileChooser fileChooser;

            if (getAttachmentFiles().size() > 0 && getAttachmentFiles().get(0).exists()) {
                fileChooser = new JFileChooser(getAttachmentFiles().get(0));
            } else {
                fileChooser = new JFileChooser();
            }

            fileChooser.setMultiSelectionEnabled(true);

            int approve = fileChooser.showOpenDialog(MessageEditForm.getInstance().getMsgEditorPanel());
            if (approve == JFileChooser.APPROVE_OPTION) {
                appendAttachmentFilePath(fileChooser);
            }
        });
        uEditorLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Desktop desktop = Desktop.getDesktop();
                try {
                    desktop.browse(new URI("http://kindeditor.net/demo.php"));
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
    public void init(Integer msgId) {
        clearAllField();
        TMsg tMsg = msgMapper.selectByPrimaryKey(msgId);
        if (tMsg != null) {
            TMsgMail tMsgMail = JSONUtil.toBean(tMsg.getContent(), TMsgMail.class);
            getInstance().getMailTitleTextField().setText(tMsgMail.getTitle());
            getInstance().getMailCcTextField().setText(tMsgMail.getCc());
            getInstance().getMailFilesTextArea().setText(tMsgMail.getFiles());
            getInstance().getMailContentPane().setText(tMsgMail.getContent());

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            messageEditForm.getMsgNameField().setText(tMsg.getMsgName());
            messageEditForm.getPreviewUserField().setText(tMsg.getPreviewUser());
        }
    }

    @Override
    public void save(Integer accountId, String msgName) {
        boolean existSameMsg = false;
        Integer msgId = null;
        TMsg tMsg = msgMapper.selectByUnique(MessageTypeEnum.EMAIL_CODE, accountId, msgName);
        if (tMsg != null) {
            existSameMsg = true;
            msgId = tMsg.getId();
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }
        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String mailTitle = getInstance().getMailTitleTextField().getText();
            String mailCc = getInstance().getMailCcTextField().getText();
            String mailFiles = getInstance().getMailFilesTextArea().getText();
            String mailContent = getInstance().getMailContentPane().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsg msg = new TMsg();
            TMsgMail tMsgMail = new TMsgMail();
            msg.setMsgType(MessageTypeEnum.EMAIL_CODE);
            msg.setAccountId(accountId);
            msg.setMsgName(msgName);
            tMsgMail.setTitle(mailTitle);
            tMsgMail.setCc(mailCc);
            tMsgMail.setFiles(mailFiles);
            tMsgMail.setContent(mailContent);
            msg.setCreateTime(now);
            msg.setModifiedTime(now);

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            msg.setPreviewUser(messageEditForm.getPreviewUserField().getText());

            msg.setContent(JSONUtil.toJsonStr(tMsgMail));
            if (existSameMsg) {
                msg.setId(msgId);
                msgMapper.updateByPrimaryKeySelective(msg);
            } else {
                msgMapper.insertSelective(msg);
            }

            JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 获取附件文件数组
     *
     * @return
     */
    public List<File> getAttachmentFiles() {
        List<File> files = new ArrayList<>();
        String text = mailFilesTextArea.getText();
        String[] strings = text.split("\\n");
        for (String string : strings) {
            string = string.trim();
            if (StringUtils.isNotEmpty(string)) {
                files.add(new File(string));
            }
        }
        return files;
    }

    /**
     * 添加附件文件路径到附件文本域
     *
     * @param fileChooser
     */
    public void appendAttachmentFilePath(JFileChooser fileChooser) {
        File[] selectedFiles = fileChooser.getSelectedFiles();
        for (File selectedFile : selectedFiles) {
            if (StringUtils.isNotBlank(mailFilesTextArea.getText())) {
                mailFilesTextArea.append("\n");
            }
            mailFilesTextArea.append(selectedFile.getAbsolutePath());
        }
    }

    public static MailMsgForm getInstance() {
        if (mailMsgForm == null) {
            mailMsgForm = new MailMsgForm();
        }
        return mailMsgForm;
    }

    /**
     * 清空所有界面字段
     */
    @Override
    public void clearAllField() {
        getInstance().getMailTitleTextField().setText("");
        getInstance().getMailCcTextField().setText("");
        getInstance().getMailFilesTextArea().setText("");
        getInstance().getMailContentPane().setText("");
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
        mailPanel.setLayout(new GridLayoutManager(5, 3, new Insets(8, 8, 8, 8), -1, -1));
        mailPanel.setMinimumSize(new Dimension(-1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("邮件标题");
        mailPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("附件");
        mailPanel.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mailTitleTextField = new JTextField();
        mailPanel.add(mailTitleTextField, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        mailContentPane = new JEditorPane();
        mailContentPane.setBackground(new Color(-12236470));
        mailContentPane.setContentType("text/html");
        mailContentPane.setText("<html>\r\n  <head>\r\n    \r\n  </head>\r\n  <body>\r\n  </body>\r\n</html>\r\n");
        mailPanel.add(mailContentPane, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("邮件正文(HTML)");
        mailPanel.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        uEditorLabel = new JLabel();
        uEditorLabel.setText("<html><a href=\"http://kindeditor.net/demo.php\">使用KindEditor编辑HTML</a></html>");
        mailPanel.add(uEditorLabel, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileExploreButton = new JButton();
        fileExploreButton.setHorizontalAlignment(0);
        fileExploreButton.setText("添加附件");
        mailPanel.add(fileExploreButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mailCcTextField = new JTextField();
        mailPanel.add(mailCcTextField, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("抄送");
        mailPanel.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mailPanel.add(scrollPane1, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(-1, 60), null, 0, false));
        mailFilesTextArea = new JTextArea();
        scrollPane1.setViewportView(mailFilesTextArea);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mailPanel;
    }

}
