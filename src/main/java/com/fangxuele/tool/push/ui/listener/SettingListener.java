package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MessageManageForm;
import com.fangxuele.tool.push.ui.form.SettingForm;
import com.fangxuele.tool.push.util.DbUtilMySQL;
import com.fangxuele.tool.push.util.SystemUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.sql.Connection;
import java.util.Map;
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

    private static boolean selectAllToggle = false;

    public static void addListeners() {

        // 设置-常规-启动时自动检查更新
        SettingForm.settingForm.getAutoCheckUpdateCheckBox().addActionListener(e -> {
            Init.config.setAutoCheckUpdate(SettingForm.settingForm.getAutoCheckUpdateCheckBox().isSelected());
            Init.config.save();
        });

        // 设置-公众号-保存
        SettingForm.settingForm.getSettingMpInfoSaveButton().addActionListener(e -> {
            try {
                Init.config.setWechatAppId(SettingForm.settingForm.getWechatAppIdTextField().getText());
                Init.config.setWechatAppSecret(new String(SettingForm.settingForm.getWechatAppSecretPasswordField().getPassword()));
                Init.config.setWechatToken(new String(SettingForm.settingForm.getWechatTokenPasswordField().getPassword()));
                Init.config.setWechatAesKey(new String(SettingForm.settingForm.getWechatAesKeyPasswordField().getPassword()));
                Init.config.save();

                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-小程序-保存
        SettingForm.settingForm.getSettingMaInfoSaveButton().addActionListener(e -> {
            try {
                Init.config.setMiniAppAppId(SettingForm.settingForm.getMiniAppAppIdTextField().getText());
                Init.config.setMiniAppAppSecret(new String(SettingForm.settingForm.getMiniAppAppSecretPasswordField().getPassword()));
                Init.config.setMiniAppToken(new String(SettingForm.settingForm.getMiniAppTokenPasswordField().getPassword()));
                Init.config.setMiniAppAesKey(new String(SettingForm.settingForm.getMiniAppAesKeyPasswordField().getPassword()));
                Init.config.save();

                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-阿里云短信-保存
        SettingForm.settingForm.getSettingAliyunSaveButton().addActionListener(e -> {
            try {
                Init.config.setAliyunAccessKeyId(SettingForm.settingForm.getAliyunAccessKeyIdTextField().getText());
                Init.config.setAliyunAccessKeySecret(new String(SettingForm.settingForm.getAliyunAccessKeySecretTextField().getPassword()));
                Init.config.setAliyunSign(SettingForm.settingForm.getAliyunSignTextField().getText());
                Init.config.save();

                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-阿里大于-保存
        SettingForm.settingForm.getSettingAliInfoSaveButton().addActionListener(e -> {
            try {
                Init.config.setAliServerUrl(SettingForm.settingForm.getAliServerUrlTextField().getText());
                Init.config.setAliAppKey(new String(SettingForm.settingForm.getAliAppKeyPasswordField().getPassword()));
                Init.config.setAliAppSecret(new String(SettingForm.settingForm.getAliAppSecretPasswordField().getPassword()));
                Init.config.setAliSign(SettingForm.settingForm.getAliSignTextField().getText());
                Init.config.save();

                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-腾讯云短信-保存
        SettingForm.settingForm.getSettingTxyunSaveButton().addActionListener(e -> {
            try {
                Init.config.setTxyunAppId(SettingForm.settingForm.getTxyunAppIdTextField().getText());
                Init.config.setTxyunAppKey(new String(SettingForm.settingForm.getTxyunAppKeyTextField().getPassword()));
                Init.config.setTxyunSign(SettingForm.settingForm.getTxyunSignTextField().getText());
                Init.config.save();

                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-云片网短信-保存
        SettingForm.settingForm.getSettingYunpianSaveButton().addActionListener(e -> {
            try {
                Init.config.setYunpianApiKey(new String(SettingForm.settingForm.getYunpianApiKeyTextField().getPassword()));
                Init.config.save();

                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // mysql数据库-测试链接
        SettingForm.settingForm.getSettingTestDbLinkButton().addActionListener(e -> {
            try {
                DbUtilMySQL dbMySQL = DbUtilMySQL.getInstance();
                String dbUrl = SettingForm.settingForm.getMysqlUrlTextField().getText();
                String dbName = SettingForm.settingForm.getMysqlDatabaseTextField().getText();
                String dbUser = SettingForm.settingForm.getMysqlUserTextField().getText();
                String dbPassword = new String(SettingForm.settingForm.getMysqlPasswordField().getPassword());
                Connection conn = dbMySQL.testConnection(dbUrl, dbName, dbUser, dbPassword);
                if (conn == null) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "连接失败", "失败",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "连接成功！", "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "连接失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // mysql数据库-保存
        SettingForm.settingForm.getSettingDbInfoSaveButton().addActionListener(e -> {
            try {
                Init.config.setMysqlUrl(SettingForm.settingForm.getMysqlUrlTextField().getText());
                Init.config.setMysqlDatabase(SettingForm.settingForm.getMysqlDatabaseTextField().getText());
                Init.config.setMysqlUser(SettingForm.settingForm.getMysqlUserTextField().getText());
                Init.config.setMysqlPassword(new String(SettingForm.settingForm.getMysqlPasswordField().getPassword()));
                Init.config.save();

                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 外观-保存
        SettingForm.settingForm.getSettingAppearanceSaveButton().addActionListener(e -> {
            try {
                Init.config.setTheme(Objects.requireNonNull(SettingForm.settingForm.getSettingThemeComboBox().getSelectedItem()).toString());
                Init.config.setFont(Objects.requireNonNull(SettingForm.settingForm.getSettingFontNameComboBox().getSelectedItem()).toString());
                Init.config.setFontSize(Integer.parseInt(Objects.requireNonNull(SettingForm.settingForm.getSettingFontSizeComboBox().getSelectedItem()).toString()));
                Init.config.save();

                Init.initTheme();
                Init.initGlobalFont();
                SwingUtilities.updateComponentTreeUI(App.mainFrame);
                SwingUtilities.updateComponentTreeUI(MainWindow.mainWindow.getTabbedPane());

                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存成功！\n\n部分细节将在下次启动时生效！\n\n", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 历史消息管理-全选
        MessageManageForm.messageManageForm.getMsgHisTableSelectAllButton().addActionListener(e -> ThreadUtil.execute(() -> {
            toggleSelectAll();
            DefaultTableModel tableModel = (DefaultTableModel) MessageManageForm.messageManageForm.getMsgHistable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                tableModel.setValueAt(selectAllToggle, i, 0);
            }
        }));

        // 历史消息管理-删除
        MessageManageForm.messageManageForm.getMsgHisTableDeleteButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                DefaultTableModel tableModel = (DefaultTableModel) MessageManageForm.messageManageForm.getMsgHistable()
                        .getModel();
                int rowCount = tableModel.getRowCount();

                int selectedCount = 0;
                for (int i = 0; i < rowCount; i++) {
                    boolean isSelected = (boolean) tableModel.getValueAt(i, 0);
                    if (isSelected) {
                        selectedCount++;
                    }
                }

                if (selectedCount == 0) {
                    JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(SettingForm.settingForm.getSettingPanel(), "确认删除？", "确认",
                            JOptionPane.YES_NO_OPTION);
                    if (isDelete == JOptionPane.YES_OPTION) {
                        Map<String, String[]> msgMap = Init.msgHisManager.readMsgHis();
                        for (int i = 0; i < rowCount; ) {
                            boolean delete = (boolean) tableModel.getValueAt(i, 0);
                            if (delete) {
                                String msgName = (String) tableModel.getValueAt(i, 1);
                                if (msgMap.containsKey(msgName)) {
                                    msgMap.remove(msgName);
                                    File msgTemplateDataFile = new File(SystemUtil.configHome + "data"
                                            + File.separator + "template_data" + File.separator + msgName + ".csv");
                                    if (msgTemplateDataFile.exists()) {
                                        msgTemplateDataFile.delete();
                                    }
                                }
                                tableModel.removeRow(i);
                                MessageManageForm.messageManageForm.getMsgHistable().updateUI();
                                i = 0;
                                rowCount = tableModel.getRowCount();
                            } else {
                                i++;
                            }
                        }
                        Init.msgHisManager.writeMsgHis(msgMap);

                        Init.initMsgTab(null);
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(SettingForm.settingForm.getSettingPanel(), "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }));

    }

    /**
     * 切换全选/全不选
     */
    private static void toggleSelectAll() {
        selectAllToggle = !selectAllToggle;
    }
}
