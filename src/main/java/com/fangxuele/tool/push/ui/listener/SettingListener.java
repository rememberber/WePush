package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TWxAccountMapper;
import com.fangxuele.tool.push.domain.TWxAccount;
import com.fangxuele.tool.push.logic.msgsender.AliDayuTemplateMsgSender;
import com.fangxuele.tool.push.logic.msgsender.AliYunMsgSender;
import com.fangxuele.tool.push.logic.msgsender.TxYunMsgSender;
import com.fangxuele.tool.push.logic.msgsender.WxMaTemplateMsgSender;
import com.fangxuele.tool.push.logic.msgsender.WxMpTemplateMsgSender;
import com.fangxuele.tool.push.logic.msgsender.YunPianMsgSender;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.dialog.SwitchWxAccountDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.SettingForm;
import com.fangxuele.tool.push.util.DbUtilMySQL;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.sql.Connection;
import java.util.List;
import java.util.Objects;

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

    private static JPanel settingPanel = SettingForm.settingForm.getSettingPanel();

    public static String wxAccountType;

    private static TWxAccountMapper wxAccountMapper = MybatisUtil.getSqlSession().getMapper(TWxAccountMapper.class);

    public static void addListeners() {

        // 设置-常规-启动时自动检查更新
        SettingForm.settingForm.getAutoCheckUpdateCheckBox().addActionListener(e -> {
            App.config.setAutoCheckUpdate(SettingForm.settingForm.getAutoCheckUpdateCheckBox().isSelected());
            App.config.save();
        });

        // 设置-公众号-保存
        SettingForm.settingForm.getSettingMpInfoSaveButton().addActionListener(e -> {
            try {
                String accountName;
                if (SettingForm.settingForm.getMpAccountSwitchComboBox().getSelectedItem() == null || StringUtils.isEmpty(SettingForm.settingForm.getMpAccountSwitchComboBox().getSelectedItem().toString())) {
                    accountName = "默认账号";
                } else {
                    accountName = SettingForm.settingForm.getMpAccountSwitchComboBox().getSelectedItem().toString();
                }
                App.config.setWechatMpName(accountName);
                App.config.setWechatAppId(SettingForm.settingForm.getWechatAppIdTextField().getText());
                App.config.setWechatAppSecret(new String(SettingForm.settingForm.getWechatAppSecretPasswordField().getPassword()));
                App.config.setWechatToken(new String(SettingForm.settingForm.getWechatTokenPasswordField().getPassword()));
                App.config.setWechatAesKey(new String(SettingForm.settingForm.getWechatAesKeyPasswordField().getPassword()));

                App.config.setMpUseProxy(SettingForm.settingForm.getMpUseProxyCheckBox().isSelected());
                App.config.setMpProxyHost(SettingForm.settingForm.getMpProxyHostTextField().getText());
                App.config.setMpProxyPort(SettingForm.settingForm.getMpProxyPortTextField().getText());
                App.config.setMpProxyUserName(SettingForm.settingForm.getMpProxyUserNameTextField().getText());
                App.config.setMpProxyPassword(SettingForm.settingForm.getMpProxyPasswordTextField().getText());
                App.config.save();

                boolean update = false;
                List<TWxAccount> tWxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(SettingForm.WX_ACCOUNT_TYPE_MP, accountName);
                if (tWxAccountList.size() > 0) {
                    update = true;
                }

                TWxAccount tWxAccount = new TWxAccount();
                String now = SqliteUtil.nowDateForSqlite();
                tWxAccount.setAccountType(SettingForm.WX_ACCOUNT_TYPE_MP);
                tWxAccount.setAccountName(accountName);
                tWxAccount.setAppId(App.config.getWechatAppId());
                tWxAccount.setAppSecret(App.config.getWechatAppSecret());
                tWxAccount.setToken(App.config.getWechatToken());
                tWxAccount.setAesKey(App.config.getWechatAesKey());
                tWxAccount.setModifiedTime(now);
                if (update) {
                    tWxAccount.setId(tWxAccountList.get(0).getId());
                    wxAccountMapper.updateByPrimaryKeySelective(tWxAccount);
                } else {
                    tWxAccount.setCreateTime(now);
                    wxAccountMapper.insert(tWxAccount);
                }

                SettingForm.initSwitchMultiAccount();
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

        // 设置-公众号-多账号管理
        SettingForm.settingForm.getMpAccountManageButton().addActionListener(e -> {
            SwitchWxAccountDialog dialog = new SwitchWxAccountDialog();
            wxAccountType = SettingForm.WX_ACCOUNT_TYPE_MP;
            dialog.renderTable();
            dialog.pack();
            dialog.setVisible(true);
        });

        // 公众号切换账号事件
        SettingForm.settingForm.getMpAccountSwitchComboBox().addItemListener(e -> {
            String accountName = e.getItem().toString();
            List<TWxAccount> wxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(SettingForm.WX_ACCOUNT_TYPE_MP, accountName);
            if (wxAccountList.size() > 0) {
                TWxAccount tWxAccount = wxAccountList.get(0);
                SettingForm.settingForm.getMpAccountSwitchComboBox().setSelectedItem(tWxAccount.getAccountName());
                SettingForm.settingForm.getWechatAppIdTextField().setText(tWxAccount.getAppId());
                SettingForm.settingForm.getWechatAppSecretPasswordField().setText(tWxAccount.getAppSecret());
                SettingForm.settingForm.getWechatTokenPasswordField().setText(tWxAccount.getToken());
                SettingForm.settingForm.getWechatAesKeyPasswordField().setText(tWxAccount.getAesKey());
            }
        });

        // 设置-小程序-保存
        SettingForm.settingForm.getSettingMaInfoSaveButton().addActionListener(e -> {
            try {
                String accountName;
                if (SettingForm.settingForm.getMaAccountSwitchComboBox().getSelectedItem() == null || StringUtils.isEmpty(SettingForm.settingForm.getMaAccountSwitchComboBox().getSelectedItem().toString())) {
                    accountName = "默认账号";
                } else {
                    accountName = SettingForm.settingForm.getMaAccountSwitchComboBox().getSelectedItem().toString();
                }
                App.config.setMiniAppName(accountName);
                App.config.setMiniAppAppId(SettingForm.settingForm.getMiniAppAppIdTextField().getText());
                App.config.setMiniAppAppSecret(new String(SettingForm.settingForm.getMiniAppAppSecretPasswordField().getPassword()));
                App.config.setMiniAppToken(new String(SettingForm.settingForm.getMiniAppTokenPasswordField().getPassword()));
                App.config.setMiniAppAesKey(new String(SettingForm.settingForm.getMiniAppAesKeyPasswordField().getPassword()));

                App.config.setMaUseProxy(SettingForm.settingForm.getMaUseProxyCheckBox().isSelected());
                App.config.setMaProxyHost(SettingForm.settingForm.getMaProxyHostTextField().getText());
                App.config.setMaProxyPort(SettingForm.settingForm.getMaProxyPortTextField().getText());
                App.config.setMaProxyUserName(SettingForm.settingForm.getMaProxyUserNameTextField().getText());
                App.config.setMaProxyPassword(SettingForm.settingForm.getMaProxyPasswordTextField().getText());
                App.config.save();

                boolean update = false;
                List<TWxAccount> tWxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(SettingForm.WX_ACCOUNT_TYPE_MA, accountName);
                if (tWxAccountList.size() > 0) {
                    update = true;
                }

                TWxAccount tWxAccount = new TWxAccount();
                String now = SqliteUtil.nowDateForSqlite();
                tWxAccount.setAccountType(SettingForm.WX_ACCOUNT_TYPE_MA);
                tWxAccount.setAccountName(accountName);
                tWxAccount.setAppId(App.config.getMiniAppAppId());
                tWxAccount.setAppSecret(App.config.getMiniAppAppSecret());
                tWxAccount.setToken(App.config.getMiniAppToken());
                tWxAccount.setAesKey(App.config.getMiniAppAesKey());
                tWxAccount.setModifiedTime(now);
                if (update) {
                    tWxAccount.setId(tWxAccountList.get(0).getId());
                    wxAccountMapper.updateByPrimaryKeySelective(tWxAccount);
                } else {
                    tWxAccount.setCreateTime(now);
                    wxAccountMapper.insert(tWxAccount);
                }

                SettingForm.initSwitchMultiAccount();
                WxMaTemplateMsgSender.wxMaConfigStorage = null;
                WxMaTemplateMsgSender.wxMaService = null;
                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-小程序-多账号管理
        SettingForm.settingForm.getMaAccountManageButton().addActionListener(e -> {
            SwitchWxAccountDialog dialog = new SwitchWxAccountDialog();
            wxAccountType = SettingForm.WX_ACCOUNT_TYPE_MA;
            dialog.renderTable();
            dialog.pack();
            dialog.setVisible(true);
        });

        // 小程序切换账号事件
        SettingForm.settingForm.getMaAccountSwitchComboBox().addItemListener(e -> {
            String accountName = e.getItem().toString();
            List<TWxAccount> wxAccountList = wxAccountMapper.selectByAccountTypeAndAccountName(SettingForm.WX_ACCOUNT_TYPE_MA, accountName);
            if (wxAccountList.size() > 0) {
                TWxAccount tWxAccount = wxAccountList.get(0);
                SettingForm.settingForm.getMaAccountSwitchComboBox().setSelectedItem(tWxAccount.getAccountName());
                SettingForm.settingForm.getMiniAppAppIdTextField().setText(tWxAccount.getAppId());
                SettingForm.settingForm.getMiniAppAppSecretPasswordField().setText(tWxAccount.getAppSecret());
                SettingForm.settingForm.getMiniAppTokenPasswordField().setText(tWxAccount.getToken());
                SettingForm.settingForm.getMiniAppAesKeyPasswordField().setText(tWxAccount.getAesKey());
            }
        });

        // 设置-阿里云短信-保存
        SettingForm.settingForm.getSettingAliyunSaveButton().addActionListener(e -> {
            try {
                App.config.setAliyunAccessKeyId(SettingForm.settingForm.getAliyunAccessKeyIdTextField().getText());
                App.config.setAliyunAccessKeySecret(new String(SettingForm.settingForm.getAliyunAccessKeySecretTextField().getPassword()));
                App.config.setAliyunSign(SettingForm.settingForm.getAliyunSignTextField().getText());
                App.config.save();
                AliYunMsgSender.iAcsClient = null;

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-阿里大于-保存
        SettingForm.settingForm.getSettingAliInfoSaveButton().addActionListener(e -> {
            try {
                App.config.setAliServerUrl(SettingForm.settingForm.getAliServerUrlTextField().getText());
                App.config.setAliAppKey(new String(SettingForm.settingForm.getAliAppKeyPasswordField().getPassword()));
                App.config.setAliAppSecret(new String(SettingForm.settingForm.getAliAppSecretPasswordField().getPassword()));
                App.config.setAliSign(SettingForm.settingForm.getAliSignTextField().getText());
                App.config.save();
                AliDayuTemplateMsgSender.taobaoClient = null;

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-腾讯云短信-保存
        SettingForm.settingForm.getSettingTxyunSaveButton().addActionListener(e -> {
            try {
                App.config.setTxyunAppId(SettingForm.settingForm.getTxyunAppIdTextField().getText());
                App.config.setTxyunAppKey(new String(SettingForm.settingForm.getTxyunAppKeyTextField().getPassword()));
                App.config.setTxyunSign(SettingForm.settingForm.getTxyunSignTextField().getText());
                App.config.save();

                TxYunMsgSender.smsSingleSender = null;

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-云片网短信-保存
        SettingForm.settingForm.getSettingYunpianSaveButton().addActionListener(e -> {
            try {
                App.config.setYunpianApiKey(new String(SettingForm.settingForm.getYunpianApiKeyTextField().getPassword()));
                App.config.save();
                YunPianMsgSender.yunpianClient = null;

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // mysql数据库-测试链接
        SettingForm.settingForm.getSettingTestDbLinkButton().addActionListener(e -> {
            try {
                String dbUrl = SettingForm.settingForm.getMysqlUrlTextField().getText();
                String dbName = SettingForm.settingForm.getMysqlDatabaseTextField().getText();
                String dbUser = SettingForm.settingForm.getMysqlUserTextField().getText();
                String dbPassword = new String(SettingForm.settingForm.getMysqlPasswordField().getPassword());
                if (StringUtils.isEmpty(dbUrl) || StringUtils.isEmpty(dbName)
                        || StringUtils.isEmpty(dbUser) || StringUtils.isEmpty(dbPassword)) {
                    JOptionPane.showMessageDialog(settingPanel,
                            "请先在设置中填写并保存MySQL数据库相关配置！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                DbUtilMySQL dbMySQL = DbUtilMySQL.getInstance();
                Connection conn = dbMySQL.testConnection(dbUrl, dbName, dbUser, dbPassword);
                if (conn == null) {
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
            }
        });

        // mysql数据库-保存
        SettingForm.settingForm.getSettingDbInfoSaveButton().addActionListener(e -> {
            try {
                App.config.setMysqlUrl(SettingForm.settingForm.getMysqlUrlTextField().getText());
                App.config.setMysqlDatabase(SettingForm.settingForm.getMysqlDatabaseTextField().getText());
                App.config.setMysqlUser(SettingForm.settingForm.getMysqlUserTextField().getText());
                App.config.setMysqlPassword(new String(SettingForm.settingForm.getMysqlPasswordField().getPassword()));
                App.config.save();

                JOptionPane.showMessageDialog(settingPanel, "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 外观-保存
        SettingForm.settingForm.getSettingAppearanceSaveButton().addActionListener(e -> {
            try {
                App.config.setTheme(Objects.requireNonNull(SettingForm.settingForm.getSettingThemeComboBox().getSelectedItem()).toString());
                App.config.setFont(Objects.requireNonNull(SettingForm.settingForm.getSettingFontNameComboBox().getSelectedItem()).toString());
                App.config.setFontSize(Integer.parseInt(Objects.requireNonNull(SettingForm.settingForm.getSettingFontSizeComboBox().getSelectedItem()).toString()));
                App.config.save();

                Init.initTheme();
                Init.initGlobalFont();
                SwingUtilities.updateComponentTreeUI(App.mainFrame);
                SwingUtilities.updateComponentTreeUI(MainWindow.mainWindow.getTabbedPane());

                JOptionPane.showMessageDialog(settingPanel, "保存成功！\n\n部分细节将在下次启动时生效！\n\n", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(settingPanel, "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        SettingForm.settingForm.getMpUseProxyCheckBox().addChangeListener(new ChangeListener() {
            /**
             * Invoked when the target of the listener has changed its state.
             *
             * @param e a ChangeEvent object
             */
            @Override
            public void stateChanged(ChangeEvent e) {
                SettingForm.toggleMpProxyPanel();
            }
        });

        SettingForm.settingForm.getMaUseProxyCheckBox().addChangeListener(new ChangeListener() {
            /**
             * Invoked when the target of the listener has changed its state.
             *
             * @param e a ChangeEvent object
             */
            @Override
            public void stateChanged(ChangeEvent e) {
                SettingForm.toggleMaProxyPanel();
            }
        });
    }

}
