package com.fangxuele.tool.wechat.push.ui.listener;

import com.fangxuele.tool.wechat.push.logic.PushData;
import com.fangxuele.tool.wechat.push.logic.PushManage;
import com.fangxuele.tool.wechat.push.ui.Init;
import com.fangxuele.tool.wechat.push.ui.MainWindow;
import com.fangxuele.tool.wechat.push.util.DbUtilMySQL;
import com.opencsv.CSVReader;
import com.xiaoleilu.hutool.io.file.FileReader;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpUserList;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 准备目标数据tab相关事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/19.
 */
public class MemberListener {
    private static final Log logger = LogFactory.get();

    public static void addListeners() {
        // 从文件导入按钮事件
        MainWindow.mainWindow.getImportFromFileButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        File file = new File(MainWindow.mainWindow.getMemberFilePathField().getText());
                        CSVReader reader = null;
                        FileReader fileReader = null;

                        int currentImported = 0;

                        try {
                            MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(true);
                            if (file.getName().endsWith(".csv")) {
                                reader = new CSVReader(new java.io.FileReader(file));
                                String[] nextLine;
                                PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                                while ((nextLine = reader.readNext()) != null) {
                                    PushData.allUser.add(nextLine[0]);
                                    currentImported++;
                                    MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                                    MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(currentImported);
                                }
                            } else {
                                fileReader = new FileReader(file);
                                PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                                PushData.allUser.addAll(fileReader.readLines());
                                MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(PushData.allUser.size()));
                            }
                            MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
                            JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入完成！", "完成",
                                    JOptionPane.INFORMATION_MESSAGE);

                            Init.configer.setMemberFilePath(MainWindow.mainWindow.getMemberFilePathField().getText());
                            Init.configer.save();
                        } catch (Exception e1) {
                            JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入失败！\n\n" + e1.getMessage(), "失败",
                                    JOptionPane.ERROR_MESSAGE);
                            logger.error(e1);
                            e1.printStackTrace();
                        } finally {
                            MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (IOException e1) {
                                    logger.error(e1);
                                    e1.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();

            }
        });

        // 导入全员按钮事件
        MainWindow.mainWindow.getMemberImportAllButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getMpUserList();
                            JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入完成！", "完成",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } catch (WxErrorException e1) {
                            JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入失败！\n\n" + e1.getMessage(), "失败",
                                    JOptionPane.ERROR_MESSAGE);
                            logger.error(e1);
                            e1.printStackTrace();
                        } finally {
                            MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
                        }
                    }
                }).start();
            }
        });

        // 清除按钮事件
        MainWindow.mainWindow.getClearImportButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int isClear = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getMemberPanel(), "确认清除？", "确认",
                        JOptionPane.INFORMATION_MESSAGE);
                if (isClear == JOptionPane.YES_OPTION) {
                    if (PushData.allUser != null) {
                        PushData.allUser.clear();
                        MainWindow.mainWindow.getMemberTabCountLabel().setText("0");
                    }
                }
            }
        });

        // 从历史导入按钮事件
        MainWindow.mainWindow.getImportFromHisButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        File file = new File("data/push_his/" + MainWindow.mainWindow.getMemberHisComboBox().getSelectedItem().toString());
                        CSVReader reader = null;
                        FileReader fileReader = null;

                        int currentImported = 0;

                        try {
                            MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(true);
                            reader = new CSVReader(new java.io.FileReader(file));
                            String[] nextLine;
                            PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                            while ((nextLine = reader.readNext()) != null) {
                                PushData.allUser.add(nextLine[0]);
                                currentImported++;
                                MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                                MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(currentImported);
                            }
                            MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
                            JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入完成！", "完成",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception e1) {
                            JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入失败！\n\n" + e1.getMessage(), "失败",
                                    JOptionPane.ERROR_MESSAGE);
                            logger.error(e1);
                            e1.printStackTrace();
                        } finally {
                            MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (IOException e1) {
                                    logger.error(e1);
                                    e1.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();
            }
        });

        // 从sql导入 按钮事件
        MainWindow.mainWindow.getImportFromSqlButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MainWindow.mainWindow.getImportFromSqlButton().setEnabled(false);
                        MainWindow.mainWindow.getImportFromSqlButton().updateUI();

                        DbUtilMySQL dbUtilMySQL = DbUtilMySQL.getInstance();// 获取SQLServer连接实例

                        String querySql = MainWindow.mainWindow.getImportFromSqlTextArea().getText();

                        MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(true);
                        if (StringUtils.isNotEmpty(querySql)) {
                            try {
                                ResultSet rs = dbUtilMySQL.executeQuery(querySql);// 表查询
                                PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                                int currentImported = 0;

                                while (rs.next()) {
                                    PushData.allUser.add(rs.getString(1));
                                    currentImported++;
                                    MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                                    MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(currentImported);
                                }

                                JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入完成！", "完成",
                                        JOptionPane.INFORMATION_MESSAGE);

                                Init.configer.setMemberSql(querySql);
                                Init.configer.save();
                            } catch (Exception e1) {
                                JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入失败！\n\n" + e1.getMessage(), "失败",
                                        JOptionPane.ERROR_MESSAGE);
                                logger.error(e1);
                                e1.printStackTrace();
                            } finally {
                                MainWindow.mainWindow.getImportFromSqlButton().setEnabled(true);
                                MainWindow.mainWindow.getImportFromSqlButton().updateUI();
                                MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
                            }
                        }
                    }
                }).start();
            }
        });
    }

    /**
     * 拉取公众平台用户列表
     */
    public static void getMpUserList() throws WxErrorException {
        MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(true);

        WxMpService wxMpService = PushManage.getWxMpService();

        WxMpUserList wxMpUserList = wxMpService.getUserService().userList(null);

        PushManage.console("关注该公众账号的总用户数：" + wxMpUserList.getTotal());
        PushManage.console("拉取的OPENID个数：" + wxMpUserList.getCount());

        MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
        MainWindow.mainWindow.getMemberTabImportProgressBar().setMaximum((int) wxMpUserList.getTotal());
        int importedCount = 0;

        List<String> openIds = wxMpUserList.getOpenids();

        PushData.allUser = Collections.synchronizedList(new ArrayList<>());
        PushData.allUser.addAll(openIds);

        importedCount += wxMpUserList.getCount();
        MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(importedCount));
        MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(importedCount);

        while (StringUtils.isNotEmpty(wxMpUserList.getNextOpenid())) {
            wxMpUserList = wxMpService.getUserService().userList(wxMpUserList.getNextOpenid());

            PushManage.console("拉取的OPENID个数：" + wxMpUserList.getCount());

            if (wxMpUserList.getCount() == 0) {
                break;
            }
            openIds = wxMpUserList.getOpenids();

            PushData.allUser.addAll(openIds);
            importedCount += wxMpUserList.getCount();
            MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(importedCount));
            MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(importedCount);
        }

        MainWindow.mainWindow.getMemberTabImportProgressBar().setValue((int) wxMpUserList.getTotal());
    }
}
