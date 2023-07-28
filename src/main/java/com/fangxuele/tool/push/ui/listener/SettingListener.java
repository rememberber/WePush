package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TWxAccountMapper;
import com.fangxuele.tool.push.logic.msgsender.HttpMsgSender;
import com.fangxuele.tool.push.logic.msgsender.MailMsgSender;
import com.fangxuele.tool.push.logic.msgsender.WxMaSubscribeMsgSender;
import com.fangxuele.tool.push.logic.msgsender.WxMpTemplateMsgSender;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.CommonTipsDialog;
import com.fangxuele.tool.push.ui.dialog.MailTestDialog;
import com.fangxuele.tool.push.ui.form.SettingForm;
import com.fangxuele.tool.push.util.HikariUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <pre>
 * 设置tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/16.
 */
public class SettingListener {
    private static final Log logger = LogFactory.get();

    public static String wxAccountType;

    private static TWxAccountMapper wxAccountMapper = MybatisUtil.getSqlSession().getMapper(TWxAccountMapper.class);

    public static void addListeners() {
        SettingForm settingForm = SettingForm.getInstance();
        JPanel settingPanel = settingForm.getSettingPanel();

        // 设置-常规-启动时自动检查更新
        settingForm.getAutoCheckUpdateCheckBox().addActionListener(e -> {
            App.config.setAutoCheckUpdate(settingForm.getAutoCheckUpdateCheckBox().isSelected());
            App.config.save();
        });
        // 设置-常规-显示系统托盘图标
        settingForm.getUseTrayCheckBox().addActionListener(e -> {
            App.config.setUseTray(settingForm.getUseTrayCheckBox().isSelected());
            App.config.save();
            if (App.tray == null && App.config.isUseTray()) {
                Init.initTray();
            } else if (App.tray != null && !App.config.isUseTray()) {
                App.tray.remove(App.trayIcon);
                App.trayIcon = null;
                App.tray = null;
            }
        });
        // 设置-常规-关闭窗口时最小化到系统托盘
        settingForm.getCloseToTrayCheckBox().addActionListener(e -> {
            App.config.setCloseToTray(settingForm.getCloseToTrayCheckBox().isSelected());
            App.config.save();
        });
        // 设置-常规-默认最大化窗口
        settingForm.getDefaultMaxWindowCheckBox().addActionListener(e -> {
            App.config.setDefaultMaxWindow(settingForm.getDefaultMaxWindowCheckBox().isSelected());
            App.config.save();
        });

        // 设置-常规-最大线程数
        settingForm.getMaxThreadsSaveButton().addActionListener(e -> {
            try {
                App.config.setMaxThreads(Integer.valueOf(settingForm.getMaxThreadsTextField().getText()));
                App.config.save();
                PushListener.refreshPushInfo();

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-公众号-保存
        settingForm.getSettingMpInfoSaveButton().addActionListener(e -> {
            try {

                App.config.setMpUseProxy(settingForm.getMpUseProxyCheckBox().isSelected());
                App.config.setMpProxyHost(settingForm.getMpProxyHostTextField().getText());
                App.config.setMpProxyPort(settingForm.getMpProxyPortTextField().getText());
                App.config.setMpProxyUserName(settingForm.getMpProxyUserNameTextField().getText());
                App.config.setMpProxyPassword(settingForm.getMpProxyPasswordTextField().getText());

                App.config.setMpUseOutSideAt(settingForm.getUseOutSideAccessTokenCheckBox().isSelected());
                App.config.setMpManualAt(settingForm.getManualAtRadioButton().isSelected());
                App.config.setMpApiAt(settingForm.getApiAtRadioButton().isSelected());
                App.config.setMpAt(settingForm.getAccessTokenTextField().getText());
                App.config.setMpAtExpiresIn(settingForm.getAtExpiresInTextField().getText());
                App.config.setMpAtApiUrl(settingForm.getAtApiUrlTextField().getText());

                App.config.save();

                WxMpTemplateMsgSender.wxMpConfigStorage = null;
                WxMpTemplateMsgSender.wxMpService = null;
                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-小程序-保存
        settingForm.getSettingMaInfoSaveButton().addActionListener(e -> {
            try {
                App.config.setMaUseProxy(settingForm.getMaUseProxyCheckBox().isSelected());
                App.config.setMaProxyHost(settingForm.getMaProxyHostTextField().getText());
                App.config.setMaProxyPort(settingForm.getMaProxyPortTextField().getText());
                App.config.setMaProxyUserName(settingForm.getMaProxyUserNameTextField().getText());
                App.config.setMaProxyPassword(settingForm.getMaProxyPasswordTextField().getText());
                App.config.save();

                WxMaSubscribeMsgSender.wxMaConfigStorage = null;
                WxMaSubscribeMsgSender.wxMaService = null;
                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        settingForm.getHttpSaveButton().addActionListener(e -> {
            try {
                App.config.setHttpUseProxy(settingForm.getHttpUseProxyCheckBox().isSelected());
                App.config.setHttpProxyHost(settingForm.getHttpProxyHostTextField().getText());
                App.config.setHttpProxyPort(settingForm.getHttpProxyPortTextField().getText());
                App.config.setHttpProxyUserName(settingForm.getHttpProxyUserTextField().getText());
                App.config.setHttpProxyPassword(settingForm.getHttpProxyPasswordTextField().getText());
                App.config.save();

                HttpMsgSender.proxy = null;
                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // E-Mail测试
        settingForm.getTestMailButton().addActionListener(e -> {
            App.config.setMailHost(settingForm.getMailHostTextField().getText());
            App.config.setMailPort(settingForm.getMailPortTextField().getText());
            App.config.setMailFrom(settingForm.getMailFromTextField().getText());
            App.config.setMailUser(settingForm.getMailUserTextField().getText());
            App.config.setMailPassword(new String(settingForm.getMailPasswordField().getPassword()));
            App.config.setMailUseStartTLS(settingForm.getMailStartTLSCheckBox().isSelected());
            App.config.setMailUseSSL(settingForm.getMailSSLCheckBox().isSelected());
            MailMsgSender.mailAccount = null;

            MailTestDialog mailTestDialog = new MailTestDialog();
            mailTestDialog.pack();
            mailTestDialog.setVisible(true);
        });

        // E-Mail保存
        settingForm.getSaveMailButton().addActionListener(e -> {
            try {
                App.config.setMailHost(settingForm.getMailHostTextField().getText());
                App.config.setMailPort(settingForm.getMailPortTextField().getText());
                App.config.setMailFrom(settingForm.getMailFromTextField().getText());
                App.config.setMailUser(settingForm.getMailUserTextField().getText());
                App.config.setMailPassword(new String(settingForm.getMailPasswordField().getPassword()));
                App.config.setMailUseStartTLS(settingForm.getMailStartTLSCheckBox().isSelected());
                App.config.setMailUseSSL(settingForm.getMailSSLCheckBox().isSelected());
                App.config.save();

                MailMsgSender.mailAccount = null;

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // mysql数据库-测试链接
        settingForm.getSettingTestDbLinkButton().addActionListener(e -> {
            HikariDataSource hikariDataSource = null;
            try {
                String dbUrl = settingForm.getMysqlUrlTextField().getText();
                String dbUser = settingForm.getMysqlUserTextField().getText();
                String dbPassword = new String(settingForm.getMysqlPasswordField().getPassword());
                if (StringUtils.isBlank(dbUrl)) {
                    settingForm.getMysqlUrlTextField().grabFocus();
                    return;
                }
                if (StringUtils.isBlank(dbUser)) {
                    settingForm.getMysqlUserTextField().grabFocus();
                    return;
                }
                if (StringUtils.isBlank(dbPassword)) {
                    settingForm.getMysqlPasswordField().grabFocus();
                    return;
                }
                hikariDataSource = new HikariDataSource();
                hikariDataSource.setJdbcUrl("jdbc:mysql://" + dbUrl);
                hikariDataSource.setUsername(dbUser);
                hikariDataSource.setPassword(dbPassword);
                if (hikariDataSource.getConnection() == null) {
                    JOptionPane.showMessageDialog(settingPanel, "连接失败", "失败",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(settingPanel, "连接成功！", "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "连接失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            } finally {
                if (hikariDataSource != null) {
                    try {
                        hikariDataSource.close();
                    } catch (Exception e2) {
                        logger.error(e2);
                    }
                }
            }
        });

        // mysql数据库-保存
        settingForm.getSettingDbInfoSaveButton().addActionListener(e -> {
            try {
                App.config.setMysqlUrl(settingForm.getMysqlUrlTextField().getText());
                App.config.setMysqlUser(settingForm.getMysqlUserTextField().getText());
                App.config.setMysqlPassword(new String(settingForm.getMysqlPasswordField().getPassword()));
                App.config.save();

                if (HikariUtil.getHikariDataSource() != null) {
                    HikariUtil.getHikariDataSource().close();
                }

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        settingForm.getMpUseProxyCheckBox().addChangeListener(e -> SettingForm.toggleMpProxyPanel());
        settingForm.getMaUseProxyCheckBox().addChangeListener(e -> SettingForm.toggleMaProxyPanel());
        settingForm.getHttpUseProxyCheckBox().addChangeListener(e -> SettingForm.toggleHttpProxyPanel());
        settingForm.getUseOutSideAccessTokenCheckBox().addChangeListener(e -> SettingForm.toggleMpOutSideAccessTokenPanel());
        settingForm.getManualAtRadioButton().addChangeListener(e -> {
            boolean isSelected = settingForm.getManualAtRadioButton().isSelected();
            if (isSelected) {
                settingForm.getApiAtRadioButton().setSelected(false);
            }
        });
        settingForm.getApiAtRadioButton().addChangeListener(e -> {
            boolean isSelected = settingForm.getApiAtRadioButton().isSelected();
            if (isSelected) {
                settingForm.getManualAtRadioButton().setSelected(false);
            }
        });

        settingForm.getOutSideAtTipsLabel().addMouseListener(new MouseAdapter() {
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
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
        settingForm.getManualAtTipsLabel().addMouseListener(new MouseAdapter() {
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
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
        settingForm.getApiAtTipsLabel().addMouseListener(new MouseAdapter() {
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
                label.setIcon(new ImageIcon(UiConsts.HELP_FOCUSED_ICON));
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JLabel label = (JLabel) e.getComponent();
                label.setIcon(new ImageIcon(UiConsts.HELP_ICON));
                super.mouseExited(e);
            }
        });
    }

}
