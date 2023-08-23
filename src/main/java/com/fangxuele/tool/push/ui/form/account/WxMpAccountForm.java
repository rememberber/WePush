package com.fangxuele.tool.push.ui.form.account;

import cn.hutool.json.JSONUtil;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.bean.account.WxMpAccountConfig;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.logic.msgsender.WxMpTemplateMsgSender;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.CommonTipsDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.fangxuele.tool.push.util.UIUtil;
import com.fangxuele.tool.push.util.UndoUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

@Getter
@Slf4j
public class WxMpAccountForm implements IAccountForm {
    private JPanel mainPanel;
    private JTextField appIdTextField;
    private JTextField appSecretTextField;
    private JTextField tokenTextField;
    private JTextField aesKeyTextField;
    private JCheckBox mpUseProxyCheckBox;
    private JPanel mpProxyPanel;
    private JTextField mpProxyHostTextField;
    private JTextField mpProxyPortTextField;
    private JTextField mpProxyUserNameTextField;
    private JTextField mpProxyPasswordTextField;
    private JCheckBox useOutSideAccessTokenCheckBox;
    private JPanel mpOutSideAccessTokenPanel;
    private JRadioButton manualAtRadioButton;
    private JRadioButton apiAtRadioButton;
    private JTextField accessTokenTextField;
    private JTextField atExpiresInTextField;
    private JTextField atApiUrlTextField;
    private JLabel manualAtTipsLabel;
    private JLabel apiAtTipsLabel;
    private JLabel outSideAtTipsLabel;

    private static WxMpAccountForm wxMpAccountForm;

    public volatile static WxMpDefaultConfigImpl wxMpConfigStorage;
    public volatile static WxMpService wxMpService;

    @Override
    public void init(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);

            WxMpAccountForm instance = getInstance();
            WxMpAccountConfig wxMpAccountConfig = JSONUtil.toBean(tAccount.getAccountConfig(), WxMpAccountConfig.class);
            instance.getAppIdTextField().setText(wxMpAccountConfig.getAppId());
            instance.getAppSecretTextField().setText(wxMpAccountConfig.getAppSecret());
            instance.getTokenTextField().setText(wxMpAccountConfig.getToken());
            instance.getAesKeyTextField().setText(wxMpAccountConfig.getAesKey());
            instance.getMpUseProxyCheckBox().setSelected(wxMpAccountConfig.isMpUseProxy());
            instance.getMpProxyHostTextField().setText(wxMpAccountConfig.getMpProxyHost());
            instance.getMpProxyPortTextField().setText(wxMpAccountConfig.getMpProxyPort());
            instance.getMpProxyUserNameTextField().setText(wxMpAccountConfig.getMpProxyUserName());
            instance.getMpProxyPasswordTextField().setText(wxMpAccountConfig.getMpProxyPassword());
            instance.getUseOutSideAccessTokenCheckBox().setSelected(wxMpAccountConfig.isMpUseOutSideAt());
            instance.getManualAtRadioButton().setSelected(wxMpAccountConfig.isMpManualAt());
            instance.getApiAtRadioButton().setSelected(wxMpAccountConfig.isMpApiAt());
            instance.getAccessTokenTextField().setText(wxMpAccountConfig.getMpAt());
            instance.getAtExpiresInTextField().setText(wxMpAccountConfig.getMpAtExpiresIn());
            instance.getAtApiUrlTextField().setText(wxMpAccountConfig.getMpAtApiUrl());

            toggleMpProxyPanel();
            toggleMpOutSideAccessTokenPanel();
        }

        apiAtTipsLabel.setIcon(new FlatSVGIcon("icon/help.svg"));
        manualAtTipsLabel.setIcon(new FlatSVGIcon("icon/help.svg"));
        outSideAtTipsLabel.setIcon(new FlatSVGIcon("icon/help.svg"));

    }

    /**
     * 切换公众号代理设置面板显示/隐藏
     */
    public void toggleMpProxyPanel() {
        boolean mpUseProxy = mpUseProxyCheckBox.isSelected();
        if (mpUseProxy) {
            mpProxyPanel.setVisible(true);
        } else {
            mpProxyPanel.setVisible(false);
        }
    }

    /**
     * 切换使用外部AccessToken面板显示/隐藏
     */
    public void toggleMpOutSideAccessTokenPanel() {
        boolean useOutSideAccessToken = useOutSideAccessTokenCheckBox.isSelected();
        if (useOutSideAccessToken) {
            mpOutSideAccessTokenPanel.setVisible(true);
        } else {
            mpOutSideAccessTokenPanel.setVisible(false);
        }
    }

    @Override
    public void save(String accountName) {
        if (StringUtils.isNotEmpty(accountName)) {

            WxMpAccountForm instance = getInstance();
            TAccount tAccount = accountMapper.selectByMsgTypeAndAccountName(App.config.getMsgType(), accountName);

            boolean existSameAccount = false;

            if (tAccount != null) {
                existSameAccount = true;
            }

            int isCover = JOptionPane.NO_OPTION;
            if (existSameAccount) {
                // 如果存在，是否覆盖
                isCover = JOptionPane.showConfirmDialog(MainWindow.getInstance().getMessagePanel(), "已经存在同名的账号，\n是否覆盖？", "确认",
                        JOptionPane.YES_NO_OPTION);
            }

            if (!existSameAccount || isCover == JOptionPane.YES_OPTION) {

                String now = SqliteUtil.nowDateForSqlite();

                TAccount tAccount1 = new TAccount();
                tAccount1.setMsgType(App.config.getMsgType());
                tAccount1.setAccountName(accountName);

                WxMpAccountConfig wxMpAccountConfig = new WxMpAccountConfig();
                wxMpAccountConfig.setAppId(instance.getAppIdTextField().getText());
                wxMpAccountConfig.setAppSecret(instance.getAppSecretTextField().getText());
                wxMpAccountConfig.setToken(instance.getTokenTextField().getText());
                wxMpAccountConfig.setAesKey(instance.getAesKeyTextField().getText());
                wxMpAccountConfig.setMpUseProxy(instance.getMpUseProxyCheckBox().isSelected());
                wxMpAccountConfig.setMpProxyHost(instance.getMpProxyHostTextField().getText());
                wxMpAccountConfig.setMpProxyPort(instance.getMpProxyPortTextField().getText());
                wxMpAccountConfig.setMpProxyUserName(instance.getMpProxyUserNameTextField().getText());
                wxMpAccountConfig.setMpProxyPassword(instance.getMpProxyPasswordTextField().getText());
                wxMpAccountConfig.setMpUseOutSideAt(instance.getUseOutSideAccessTokenCheckBox().isSelected());
                wxMpAccountConfig.setMpManualAt(instance.getManualAtRadioButton().isSelected());
                wxMpAccountConfig.setMpApiAt(instance.getApiAtRadioButton().isSelected());
                wxMpAccountConfig.setMpAt(instance.getAccessTokenTextField().getText());
                wxMpAccountConfig.setMpAtExpiresIn(instance.getAtExpiresInTextField().getText());
                wxMpAccountConfig.setMpAtApiUrl(instance.getAtApiUrlTextField().getText());

                tAccount1.setAccountConfig(JSONUtil.toJsonStr(wxMpAccountConfig));

                tAccount1.setModifiedTime(now);

                if (existSameAccount) {
                    accountMapper.updateByMsgTypeAndAccountName(tAccount1);
                    WxMpTemplateMsgSender.removeAccount(tAccount.getId());
                } else {
                    tAccount1.setCreateTime(now);
                    accountMapper.insertSelective(tAccount1);
                }

                JOptionPane.showMessageDialog(MainWindow.getInstance().getMainPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        }
    }

    @Override
    public void clear() {
        UIUtil.clearForm(getInstance());
    }

    @Override
    public JPanel getMainPanel() {
        return mainPanel;
    }

    public static WxMpAccountForm getInstance() {
        if (wxMpAccountForm == null) {
            wxMpAccountForm = new WxMpAccountForm();
            UndoUtil.register(wxMpAccountForm);

            wxMpAccountForm.getMpUseProxyCheckBox().addChangeListener(e -> wxMpAccountForm.toggleMpProxyPanel());
            wxMpAccountForm.getUseOutSideAccessTokenCheckBox().addChangeListener(e -> wxMpAccountForm.toggleMpOutSideAccessTokenPanel());
            wxMpAccountForm.getManualAtRadioButton().addChangeListener(e -> {
                boolean isSelected = wxMpAccountForm.getManualAtRadioButton().isSelected();
                if (isSelected) {
                    wxMpAccountForm.getApiAtRadioButton().setSelected(false);
                }
            });
            wxMpAccountForm.getApiAtRadioButton().addChangeListener(e -> {
                boolean isSelected = wxMpAccountForm.getApiAtRadioButton().isSelected();
                if (isSelected) {
                    wxMpAccountForm.getManualAtRadioButton().setSelected(false);
                }
            });

            wxMpAccountForm.getOutSideAtTipsLabel().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    CommonTipsDialog dialog = new CommonTipsDialog();

                    StringBuilder tipsBuilder = new StringBuilder();
                    tipsBuilder.append("<h1>什么场景下需要使用外部AccessToken？</h1>");
                    tipsBuilder.append("<p>调用腾讯公众号接口需要AccessToken，上面配置的AppID、AppSecret等正是为了获得AccessToken；</p>");
                    tipsBuilder.append("<p>由于有些企业已经开发了微信公众号相关的服务，不必再次通过上面的AppID等配置再次获取；</p>");
                    tipsBuilder.append("<p>而且每次获取都会使之前的失效，加上每个公众号每天获取的次数有限；</p>");
                    tipsBuilder.append("<h2>建议每天使用WePush频率很高的时候可以使用此功能</h2>");
                    tipsBuilder.append("<h2>反之，可不用设置</h2>");

                    dialog.setHtmlText(tipsBuilder.toString());
                    dialog.pack();
                    dialog.setVisible(true);

                    super.mousePressed(e);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    JLabel label = (JLabel) e.getComponent();
                    label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    label.setIcon(UiConsts.HELP_FOCUSED_ICON);
                    super.mouseEntered(e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    JLabel label = (JLabel) e.getComponent();
                    label.setIcon(UiConsts.HELP_ICON);
                    super.mouseExited(e);
                }
            });
            wxMpAccountForm.getManualAtTipsLabel().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    CommonTipsDialog dialog = new CommonTipsDialog();

                    StringBuilder tipsBuilder = new StringBuilder();
                    tipsBuilder.append("<h1>这是什么？</h1>");
                    tipsBuilder.append("<h2>手动填写AccessToken和过期时间</h2>");
                    tipsBuilder.append("<h2>建议仅在临时使用一次WePush且使用时间不会很长的时候才使用</h2>");
                    tipsBuilder.append("<p>请向您所在企业的开发人员索取，注意保密</p>");

                    dialog.setHtmlText(tipsBuilder.toString());
                    dialog.pack();
                    dialog.setVisible(true);

                    super.mousePressed(e);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    JLabel label = (JLabel) e.getComponent();
                    label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    label.setIcon(UiConsts.HELP_FOCUSED_ICON);
                    super.mouseEntered(e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    JLabel label = (JLabel) e.getComponent();
                    label.setIcon(UiConsts.HELP_ICON);
                    super.mouseExited(e);
                }
            });
            wxMpAccountForm.getApiAtTipsLabel().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    CommonTipsDialog dialog = new CommonTipsDialog();

                    StringBuilder tipsBuilder = new StringBuilder();
                    tipsBuilder.append("<h1>这是什么？</h1>");
                    tipsBuilder.append("<h2>如果企业已经开发了微信公众号相关的服务，建议使用此项；</h2>");
                    tipsBuilder.append("<p>向您所在企业的开发人员索取该接口；</p>");
                    tipsBuilder.append("<p>接口使用GET请求，返回格式：</p>");
                    tipsBuilder.append("<p>{\"access_token\":\"ACCESS_TOKEN\",\"expires_in\":7200}</p>");
                    tipsBuilder.append("<p>请一定注意接口安全性，且服务端应按照失效时间进行缓存</p>");
                    tipsBuilder.append("<p>例如在接口上添加密钥相关的参数：</p>");
                    tipsBuilder.append("<p>示例：http://mydomain.com/wechat/getAccessToken?secret=jad76^j2#SY</p>");

                    dialog.setHtmlText(tipsBuilder.toString());
                    dialog.pack();
                    dialog.setVisible(true);

                    super.mousePressed(e);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    JLabel label = (JLabel) e.getComponent();
                    label.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    label.setIcon(UiConsts.HELP_FOCUSED_ICON);
                    super.mouseEntered(e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    JLabel label = (JLabel) e.getComponent();
                    label.setIcon(UiConsts.HELP_ICON);
                    super.mouseExited(e);
                }
            });

            wxMpAccountForm.toggleMpProxyPanel();
            wxMpAccountForm.toggleMpOutSideAccessTokenPanel();
        }

        return wxMpAccountForm;
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 5, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("AppId");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appIdTextField = new JTextField();
        panel2.add(appIdTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Token");
        panel2.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tokenTextField = new JTextField();
        panel2.add(tokenTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("AppSecret");
        panel2.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        appSecretTextField = new JTextField();
        panel2.add(appSecretTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("AES Key");
        panel2.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        aesKeyTextField = new JTextField();
        panel2.add(aesKeyTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        Font panel3Font = this.$$$getFont$$$("Microsoft YaHei UI", -1, -1, panel3.getFont());
        if (panel3Font != null) panel3.setFont(panel3Font);
        panel1.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel3.getFont()), null));
        mpUseProxyCheckBox = new JCheckBox();
        mpUseProxyCheckBox.setText("使用HTTP代理");
        panel3.add(mpUseProxyCheckBox, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mpProxyPanel = new JPanel();
        mpProxyPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 26, 0, 0), -1, -1));
        panel3.add(mpProxyPanel, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Host");
        mpProxyPanel.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mpProxyHostTextField = new JTextField();
        mpProxyPanel.add(mpProxyHostTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("端口");
        mpProxyPanel.add(label6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mpProxyPortTextField = new JTextField();
        mpProxyPanel.add(mpProxyPortTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("用户名");
        label7.setToolTipText("选填");
        mpProxyPanel.add(label7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mpProxyUserNameTextField = new JTextField();
        mpProxyUserNameTextField.setToolTipText("选填");
        mpProxyPanel.add(mpProxyUserNameTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("密码");
        label8.setToolTipText("选填");
        mpProxyPanel.add(label8, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mpProxyPasswordTextField = new JTextField();
        mpProxyPasswordTextField.setToolTipText("选填");
        mpProxyPanel.add(mpProxyPasswordTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        useOutSideAccessTokenCheckBox = new JCheckBox();
        useOutSideAccessTokenCheckBox.setText("使用外部AccessToken");
        panel3.add(useOutSideAccessTokenCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mpOutSideAccessTokenPanel = new JPanel();
        mpOutSideAccessTokenPanel.setLayout(new GridLayoutManager(4, 4, new Insets(0, 26, 0, 0), -1, -1));
        panel3.add(mpOutSideAccessTokenPanel, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        manualAtRadioButton = new JRadioButton();
        manualAtRadioButton.setText("手动输入");
        mpOutSideAccessTokenPanel.add(manualAtRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        apiAtRadioButton = new JRadioButton();
        apiAtRadioButton.setText("通过接口获取");
        mpOutSideAccessTokenPanel.add(apiAtRadioButton, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 2, new Insets(0, 25, 0, 0), -1, -1));
        mpOutSideAccessTokenPanel.add(panel4, new GridConstraints(1, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("AccessToken");
        panel4.add(label9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        accessTokenTextField = new JTextField();
        panel4.add(accessTokenTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("有效期(秒)");
        panel4.add(label10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        atExpiresInTextField = new JTextField();
        panel4.add(atExpiresInTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, new Insets(0, 25, 0, 0), -1, -1));
        mpOutSideAccessTokenPanel.add(panel5, new GridConstraints(3, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("接口url");
        label11.setToolTipText("");
        panel5.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        atApiUrlTextField = new JTextField();
        atApiUrlTextField.setToolTipText("");
        panel5.add(atApiUrlTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        manualAtTipsLabel = new JLabel();
        manualAtTipsLabel.setText("");
        mpOutSideAccessTokenPanel.add(manualAtTipsLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        mpOutSideAccessTokenPanel.add(spacer2, new GridConstraints(0, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        apiAtTipsLabel = new JLabel();
        apiAtTipsLabel.setText("");
        mpOutSideAccessTokenPanel.add(apiAtTipsLabel, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        mpOutSideAccessTokenPanel.add(spacer3, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        outSideAtTipsLabel = new JLabel();
        outSideAtTipsLabel.setText("");
        panel3.add(outSideAtTipsLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel3.add(spacer4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
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

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
