package com.fangxuele.tool.push.ui.form.msg;

import cn.binarywang.wx.miniapp.constant.WxMaConstants;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TMsgKefuMapper;
import com.fangxuele.tool.push.domain.TMsgKefu;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.msgsender.WxMpTemplateMsgSender;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageEditForm;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import me.chanjar.weixin.common.bean.result.WxMediaUploadResult;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * <pre>
 * KefuMsgForm
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/3.
 */
@Getter
public class KefuMsgForm implements IMsgForm {
    private JPanel kefuMsgPanel;
    private JLabel kefuMsgTypeLabel;
    private JComboBox msgKefuMsgTypeComboBox;
    private JLabel kefuMsgTitleLabel;
    private JTextField msgKefuMsgTitleTextField;
    private JLabel kefuMsgPicUrlLabel;
    private JTextField msgKefuPicUrlTextField;
    private JLabel kefuMsgDescLabel;
    private JTextField msgKefuDescTextField;
    private JLabel kefuMsgUrlLabel;
    private JTextField msgKefuUrlTextField;
    private JLabel contentLabel;
    private JTextArea contentTextArea;
    private JLabel kefuMsgAppidLabel;
    private JTextField msgKefuAppidTextField;
    private JTextField msgKefuPagepathTextField;
    private JLabel kefuMsgPagepathLabel;
    private JTextField msgKefuThumbMediaIdTextField;
    private JLabel kefuMsgThumbMediaIdLabel;
    private JButton uploadImageButton;
    private JPanel thumbMediaPanel;

    private static final Log logger = LogFactory.get();
    private static KefuMsgForm kefuMsgForm;

    private static TMsgKefuMapper msgKefuMapper = MybatisUtil.getSqlSession().getMapper(TMsgKefuMapper.class);

    public KefuMsgForm() {
        // 客服消息类型切换事件
        msgKefuMsgTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                switchKefuMsgType(e.getItem().toString());
            }
        });
        uploadImageButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            FileFilter filter = new FileNameExtensionFilter("*.bmp,*.gif,*.jpeg,*.jpg,*.png", "bmp", "gif", "jpeg", "jpg", "png", "BMP", "GIF", "JPEG", "JPG", "PNG");
            fileChooser.setFileFilter(filter);

            int approve = fileChooser.showOpenDialog(MessageEditForm.getInstance().getMsgEditorPanel());
            if (approve == JFileChooser.APPROVE_OPTION) {
                try {
                    File selectedFile = fileChooser.getSelectedFile();
                    WxMediaUploadResult wxMediaUploadResult = WxMpTemplateMsgSender.getWxMpService().getMaterialService().mediaUpload(WxMaConstants.MediaType.IMAGE, selectedFile);
                    msgKefuThumbMediaIdTextField.setText(wxMediaUploadResult.getMediaId());
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(kefuMsgPanel, "上传失败！\n\n" + e1.getMessage(), "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(e1);
                }
            }
        });
    }

    @Override
    public void init(String msgName) {
        clearAllField();
        List<TMsgKefu> tMsgKefuList = msgKefuMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.KEFU_CODE, msgName);
        if (tMsgKefuList.size() > 0) {
            TMsgKefu tMsgKefu = tMsgKefuList.get(0);
            String kefuMsgType = tMsgKefu.getKefuMsgType();
            getInstance().getMsgKefuMsgTypeComboBox().setSelectedItem(kefuMsgType);
            if ("文本消息".equals(kefuMsgType)) {
                getInstance().getContentTextArea().setText(tMsgKefu.getContent());
            } else if ("图文消息".equals(kefuMsgType)) {
                getInstance().getMsgKefuMsgTitleTextField().setText(tMsgKefu.getTitle());
            } else if ("小程序卡片消息".equals(kefuMsgType)) {
                getInstance().getMsgKefuMsgTitleTextField().setText(tMsgKefu.getTitle());
                getInstance().getMsgKefuAppidTextField().setText(tMsgKefu.getAppId());
                getInstance().getMsgKefuPagepathTextField().setText(tMsgKefu.getPagePath());
                getInstance().getMsgKefuThumbMediaIdTextField().setText(tMsgKefu.getThumbMediaId());
            }
            getInstance().getMsgKefuPicUrlTextField().setText(tMsgKefu.getImgUrl());
            getInstance().getMsgKefuDescTextField().setText(tMsgKefu.getDescribe());
            getInstance().getMsgKefuUrlTextField().setText(tMsgKefu.getUrl());

            switchKefuMsgType(kefuMsgType);

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            messageEditForm.getMsgNameField().setText(tMsgKefu.getMsgName());
            messageEditForm.getPreviewUserField().setText(tMsgKefu.getPreviewUser());
        } else {
            switchKefuMsgType("图文消息");
        }
    }

    @Override
    public void save(String msgName) {
        boolean existSameMsg = false;

        List<TMsgKefu> tMsgKefuList = msgKefuMapper.selectByMsgTypeAndMsgName(MessageTypeEnum.KEFU_CODE, msgName);
        if (tMsgKefuList.size() > 0) {
            existSameMsg = true;
        }

        int isCover = JOptionPane.NO_OPTION;
        if (existSameMsg) {
            // 如果存在，是否覆盖
            isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的历史消息，\n是否覆盖？", "确认",
                    JOptionPane.YES_NO_OPTION);
        }

        if (!existSameMsg || isCover == JOptionPane.YES_OPTION) {
            String kefuMsgType = Objects.requireNonNull(getInstance().getMsgKefuMsgTypeComboBox().getSelectedItem()).toString();
            String kefuMsgContent = getInstance().getContentTextArea().getText();
            String kefuMsgTitle = getInstance().getMsgKefuMsgTitleTextField().getText();
            String kefuPicUrl = getInstance().getMsgKefuPicUrlTextField().getText();
            String kefuDesc = getInstance().getMsgKefuDescTextField().getText();
            String kefuUrl = getInstance().getMsgKefuUrlTextField().getText();
            String kefuAppId = getInstance().getMsgKefuAppidTextField().getText();
            String kefuPagePath = getInstance().getMsgKefuPagepathTextField().getText();
            String kefuThumbMediaId = getInstance().getMsgKefuThumbMediaIdTextField().getText();

            String now = SqliteUtil.nowDateForSqlite();

            TMsgKefu tMsgKefu = new TMsgKefu();
            tMsgKefu.setMsgType(MessageTypeEnum.KEFU_CODE);
            tMsgKefu.setMsgName(msgName);
            tMsgKefu.setKefuMsgType(kefuMsgType);
            tMsgKefu.setContent(kefuMsgContent);
            tMsgKefu.setTitle(kefuMsgTitle);
            tMsgKefu.setImgUrl(kefuPicUrl);
            tMsgKefu.setDescribe(kefuDesc);
            tMsgKefu.setUrl(kefuUrl);
            tMsgKefu.setModifiedTime(now);
            tMsgKefu.setAppId(kefuAppId);
            tMsgKefu.setPagePath(kefuPagePath);
            tMsgKefu.setThumbMediaId(kefuThumbMediaId);

            MessageEditForm messageEditForm = MessageEditForm.getInstance();
            tMsgKefu.setPreviewUser(messageEditForm.getPreviewUserField().getText());
            tMsgKefu.setWxAccountId(App.config.getWxAccountId());

            if (existSameMsg) {
                msgKefuMapper.updateByMsgTypeAndMsgName(tMsgKefu);
            } else {
                tMsgKefu.setCreateTime(now);
                msgKefuMapper.insertSelective(tMsgKefu);
            }

            JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "保存成功！", "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static KefuMsgForm getInstance() {
        if (kefuMsgForm == null) {
            kefuMsgForm = new KefuMsgForm();
        }
        return kefuMsgForm;
    }

    /**
     * 根据客服消息类型转换界面显示
     *
     * @param msgType 消息类型
     */
    public static void switchKefuMsgType(String msgType) {
        getInstance().getContentLabel().setVisible(false);
        getInstance().getContentTextArea().setVisible(false);
        getInstance().getKefuMsgDescLabel().setVisible(false);
        getInstance().getMsgKefuDescTextField().setVisible(false);
        getInstance().getKefuMsgPicUrlLabel().setVisible(false);
        getInstance().getMsgKefuPicUrlTextField().setVisible(false);
        getInstance().getKefuMsgUrlLabel().setVisible(false);
        getInstance().getMsgKefuUrlTextField().setVisible(false);
        getInstance().getKefuMsgTitleLabel().setVisible(false);
        getInstance().getMsgKefuMsgTitleTextField().setVisible(false);
        getInstance().getKefuMsgAppidLabel().setVisible(false);
        getInstance().getMsgKefuAppidTextField().setVisible(false);
        getInstance().getKefuMsgPagepathLabel().setVisible(false);
        getInstance().getMsgKefuPagepathTextField().setVisible(false);
        getInstance().getKefuMsgThumbMediaIdLabel().setVisible(false);
        getInstance().getThumbMediaPanel().setVisible(false);
        switch (msgType) {
            case "文本消息":
                getInstance().getContentLabel().setVisible(true);
                getInstance().getContentTextArea().setVisible(true);
                break;
            case "图文消息":
                getInstance().getKefuMsgDescLabel().setVisible(true);
                getInstance().getMsgKefuDescTextField().setVisible(true);
                getInstance().getKefuMsgPicUrlLabel().setVisible(true);
                getInstance().getMsgKefuPicUrlTextField().setVisible(true);
                getInstance().getKefuMsgUrlLabel().setVisible(true);
                getInstance().getMsgKefuUrlTextField().setVisible(true);
                getInstance().getKefuMsgTitleLabel().setVisible(true);
                getInstance().getMsgKefuMsgTitleTextField().setVisible(true);
                break;
            case "小程序卡片消息":
                getInstance().getKefuMsgTitleLabel().setVisible(true);
                getInstance().getMsgKefuMsgTitleTextField().setVisible(true);
                getInstance().getKefuMsgAppidLabel().setVisible(true);
                getInstance().getMsgKefuAppidTextField().setVisible(true);
                getInstance().getKefuMsgPagepathLabel().setVisible(true);
                getInstance().getMsgKefuPagepathTextField().setVisible(true);
                getInstance().getKefuMsgThumbMediaIdLabel().setVisible(true);
                getInstance().getThumbMediaPanel().setVisible(true);
                break;
            default:
                break;
        }
    }

    /**
     * 清空所有界面字段
     */
    public static void clearAllField() {
        getInstance().getContentTextArea().setText("");
        getInstance().getMsgKefuMsgTitleTextField().setText("");
        getInstance().getMsgKefuPicUrlTextField().setText("");
        getInstance().getMsgKefuDescTextField().setText("");
        getInstance().getMsgKefuUrlTextField().setText("");
        getInstance().getMsgKefuAppidTextField().setText("");
        getInstance().getMsgKefuPagepathTextField().setText("");
        getInstance().getMsgKefuThumbMediaIdTextField().setText("");
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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        kefuMsgPanel = new JPanel();
        kefuMsgPanel.setLayout(new GridLayoutManager(10, 2, new Insets(10, 15, 0, 0), -1, -1));
        panel1.add(kefuMsgPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        kefuMsgPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "客服消息编辑", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, kefuMsgPanel.getFont()), null));
        kefuMsgTypeLabel = new JLabel();
        kefuMsgTypeLabel.setText("消息类型");
        kefuMsgPanel.add(kefuMsgTypeLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        kefuMsgPanel.add(spacer1, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        msgKefuMsgTypeComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("图文消息");
        defaultComboBoxModel1.addElement("文本消息");
        defaultComboBoxModel1.addElement("小程序卡片消息");
        msgKefuMsgTypeComboBox.setModel(defaultComboBoxModel1);
        kefuMsgPanel.add(msgKefuMsgTypeComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kefuMsgTitleLabel = new JLabel();
        kefuMsgTitleLabel.setText("标题");
        kefuMsgPanel.add(kefuMsgTitleLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuMsgTitleTextField = new JTextField();
        kefuMsgPanel.add(msgKefuMsgTitleTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(380, -1), new Dimension(380, -1), null, 0, false));
        kefuMsgPicUrlLabel = new JLabel();
        kefuMsgPicUrlLabel.setText("图片URL");
        kefuMsgPanel.add(kefuMsgPicUrlLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuPicUrlTextField = new JTextField();
        kefuMsgPanel.add(msgKefuPicUrlTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        kefuMsgDescLabel = new JLabel();
        kefuMsgDescLabel.setText("描述");
        kefuMsgPanel.add(kefuMsgDescLabel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuDescTextField = new JTextField();
        kefuMsgPanel.add(msgKefuDescTextField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        kefuMsgUrlLabel = new JLabel();
        kefuMsgUrlLabel.setText("跳转URL");
        kefuMsgPanel.add(kefuMsgUrlLabel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kefuMsgAppidLabel = new JLabel();
        kefuMsgAppidLabel.setText("小程序appid");
        kefuMsgPanel.add(kefuMsgAppidLabel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kefuMsgPagepathLabel = new JLabel();
        kefuMsgPagepathLabel.setText("小程序页面路径");
        kefuMsgPanel.add(kefuMsgPagepathLabel, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kefuMsgThumbMediaIdLabel = new JLabel();
        kefuMsgThumbMediaIdLabel.setText("卡片图片的媒体ID");
        kefuMsgThumbMediaIdLabel.setToolTipText("thumb_media_id");
        kefuMsgPanel.add(kefuMsgThumbMediaIdLabel, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        msgKefuUrlTextField = new JTextField();
        kefuMsgPanel.add(msgKefuUrlTextField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        msgKefuAppidTextField = new JTextField();
        kefuMsgPanel.add(msgKefuAppidTextField, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        msgKefuPagepathTextField = new JTextField();
        kefuMsgPanel.add(msgKefuPagepathTextField, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        contentLabel = new JLabel();
        contentLabel.setText("内容");
        kefuMsgPanel.add(contentLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contentTextArea = new JTextArea();
        kefuMsgPanel.add(contentTextArea, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        thumbMediaPanel = new JPanel();
        thumbMediaPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        kefuMsgPanel.add(thumbMediaPanel, new GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        msgKefuThumbMediaIdTextField = new JTextField();
        thumbMediaPanel.add(msgKefuThumbMediaIdTextField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        uploadImageButton = new JButton();
        uploadImageButton.setText("上传图片");
        thumbMediaPanel.add(uploadImageButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kefuMsgTypeLabel.setLabelFor(msgKefuMsgTypeComboBox);
        kefuMsgTitleLabel.setLabelFor(msgKefuMsgTitleTextField);
        kefuMsgPicUrlLabel.setLabelFor(msgKefuPicUrlTextField);
        kefuMsgDescLabel.setLabelFor(msgKefuDescTextField);
        kefuMsgUrlLabel.setLabelFor(msgKefuUrlTextField);
        kefuMsgAppidLabel.setLabelFor(msgKefuAppidTextField);
        kefuMsgPagepathLabel.setLabelFor(msgKefuPagepathTextField);
        kefuMsgThumbMediaIdLabel.setLabelFor(msgKefuThumbMediaIdTextField);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

}
