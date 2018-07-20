package com.fangxuele.tool.push.ui.listener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.util.DbUtilMySQL;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.MainWindow;
import com.fangxuele.tool.push.util.SystemUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.sql.Connection;
import java.util.Map;

/**
 * 设置tab相关事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/16.
 */
public class SettingListener {
    private static final Log logger = LogFactory.get();

    private static boolean selectAllToggle = false;

    public static void addListeners() {

        // 设置-常规-启动时自动检查更新
        MainWindow.mainWindow.getAutoCheckUpdateCheckBox().addActionListener(e -> {
            Init.configer.setAutoCheckUpdate(MainWindow.mainWindow.getAutoCheckUpdateCheckBox().isSelected());
            Init.configer.save();
        });

        // 设置-公众号-保存
        MainWindow.mainWindow.getSettingMpInfoSaveButton().addActionListener(e -> {
            try {
                Init.configer.setWechatAppId(MainWindow.mainWindow.getWechatAppIdTextField().getText());
                Init.configer.setWechatAppSecret(new String(MainWindow.mainWindow.getWechatAppSecretPasswordField().getPassword()));
                Init.configer.setWechatToken(new String(MainWindow.mainWindow.getWechatTokenPasswordField().getPassword()));
                Init.configer.setWechatAesKey(new String(MainWindow.mainWindow.getWechatAesKeyPasswordField().getPassword()));
                Init.configer.save();

                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-小程序-保存
        MainWindow.mainWindow.getSettingMaInfoSaveButton().addActionListener(e -> {
            try {
                Init.configer.setMiniAppAppId(MainWindow.mainWindow.getMiniAppAppIdTextField().getText());
                Init.configer.setMiniAppAppSecret(new String(MainWindow.mainWindow.getMiniAppAppSecretPasswordField().getPassword()));
                Init.configer.setMiniAppToken(new String(MainWindow.mainWindow.getMiniAppTokenPasswordField().getPassword()));
                Init.configer.setMiniAppAesKey(new String(MainWindow.mainWindow.getMiniAppAesKeyPasswordField().getPassword()));
                Init.configer.save();

                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-阿里云短信-保存
        MainWindow.mainWindow.getSettingAliyunSaveButton().addActionListener(e -> {
            try {
                Init.configer.setAliyunAccessKeyId(MainWindow.mainWindow.getAliyunAccessKeyIdTextField().getText());
                Init.configer.setAliyunAccessKeySecret(new String(MainWindow.mainWindow.getAliyunAccessKeySecretTextField().getPassword()));
                Init.configer.setAliyunSign(MainWindow.mainWindow.getAliyunSignTextField().getText());
                Init.configer.save();

                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-阿里大于-保存
        MainWindow.mainWindow.getSettingAliInfoSaveButton().addActionListener(e -> {
            try {
                Init.configer.setAliServerUrl(MainWindow.mainWindow.getAliServerUrlTextField().getText());
                Init.configer.setAliAppKey(new String(MainWindow.mainWindow.getAliAppKeyPasswordField().getPassword()));
                Init.configer.setAliAppSecret(new String(MainWindow.mainWindow.getAliAppSecretPasswordField().getPassword()));
                Init.configer.setAliSign(MainWindow.mainWindow.getAliSignTextField().getText());
                Init.configer.save();

                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-腾讯云短信-保存
        MainWindow.mainWindow.getSettingTxyunSaveButton().addActionListener(e -> {
            try {
                Init.configer.setTxyunAppId(MainWindow.mainWindow.getTxyunAppIdTextField().getText());
                Init.configer.setTxyunAppKey(new String(MainWindow.mainWindow.getTxyunAppKeyTextField().getPassword()));
                Init.configer.setTxyunSign(MainWindow.mainWindow.getTxyunSignTextField().getText());
                Init.configer.save();

                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 设置-云片网短信-保存
        MainWindow.mainWindow.getSettingYunpianSaveButton().addActionListener(e -> {
            try {
                Init.configer.setYunpianApiKey(new String(MainWindow.mainWindow.getYunpianApiKeyTextField().getPassword()));
                Init.configer.save();

                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // mysql数据库-测试链接
        MainWindow.mainWindow.getSettingTestDbLinkButton().addActionListener(e -> {
            try {
                DbUtilMySQL dbMySQL = DbUtilMySQL.getInstance();
                String DBUrl = MainWindow.mainWindow.getMysqlUrlTextField().getText();
                String DBName = MainWindow.mainWindow.getMysqlDatabaseTextField().getText();
                String DBUser = MainWindow.mainWindow.getMysqlUserTextField().getText();
                String DBPassword = new String(MainWindow.mainWindow.getMysqlPasswordField().getPassword());
                Connection conn = dbMySQL.testConnection(DBUrl, DBName, DBUser, DBPassword);
                if (conn == null) {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "连接失败", "失败",
                            JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "连接成功！", "成功",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "连接失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // mysql数据库-保存
        MainWindow.mainWindow.getSettingDbInfoSaveButton().addActionListener(e -> {
            try {
                Init.configer.setMysqlUrl(MainWindow.mainWindow.getMysqlUrlTextField().getText());
                Init.configer.setMysqlDatabase(MainWindow.mainWindow.getMysqlDatabaseTextField().getText());
                Init.configer.setMysqlUser(MainWindow.mainWindow.getMysqlUserTextField().getText());
                Init.configer.setMysqlPassword(new String(MainWindow.mainWindow.getMysqlPasswordField().getPassword()));
                Init.configer.save();

                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存成功！", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 外观-保存
        MainWindow.mainWindow.getSettingAppearanceSaveButton().addActionListener(e -> {
            try {
                Init.configer.setTheme(MainWindow.mainWindow.getSettingThemeComboBox().getSelectedItem().toString());
                Init.configer.setFont(MainWindow.mainWindow.getSettingFontNameComboBox().getSelectedItem().toString());
                Init.configer.setFontSize(Integer.parseInt(MainWindow.mainWindow.getSettingFontSizeComboBox().getSelectedItem().toString()));
                Init.configer.save();

                Init.initTheme();
                Init.initGlobalFont();
                SwingUtilities.updateComponentTreeUI(MainWindow.frame);
                SwingUtilities.updateComponentTreeUI(MainWindow.mainWindow.getTabbedPane());

                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存成功！\n\n部分细节将在下次启动时生效！\n\n", "成功",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "保存失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        });

        // 历史消息管理-全选
        MainWindow.mainWindow.getMsgHisTableSelectAllButton().addActionListener(e -> new Thread(() -> {
            toggleSelectAll();
            DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getMsgHistable()
                    .getModel();
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                tableModel.setValueAt(selectAllToggle, i, 0);
            }
        }).start());

        // 历史消息管理-删除
        MainWindow.mainWindow.getMsgHisTableDeleteButton().addActionListener(e -> new Thread(() -> {
            try {
                DefaultTableModel tableModel = (DefaultTableModel) MainWindow.mainWindow.getMsgHistable()
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
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getSettingPanel(), "确认删除？", "确认",
                            JOptionPane.INFORMATION_MESSAGE);
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
                                MainWindow.mainWindow.getMsgHistable().updateUI();
                                i = 0;
                                rowCount = tableModel.getRowCount();
                                continue;
                            } else {
                                i++;
                            }
                        }
                        Init.msgHisManager.writeMsgHis(msgMap);

                        Init.initMsgTab(null);
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getSettingPanel(), "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }).start());

    }

    /**
     * 切换全选/全不选
     *
     * @return
     */
    private static void toggleSelectAll() {
        if (!selectAllToggle) {
            selectAllToggle = true;
        } else {
            selectAllToggle = false;
        }
    }
}
