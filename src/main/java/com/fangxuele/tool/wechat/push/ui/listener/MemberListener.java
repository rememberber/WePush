package com.fangxuele.tool.wechat.push.ui.listener;

import com.fangxuele.tool.wechat.push.logic.PushData;
import com.fangxuele.tool.wechat.push.logic.PushManage;
import com.fangxuele.tool.wechat.push.ui.Init;
import com.fangxuele.tool.wechat.push.ui.MainWindow;
import com.fangxuele.tool.wechat.push.util.DbUtilMySQL;
import com.fangxuele.tool.wechat.push.util.SystemUtil;
import com.opencsv.CSVReader;
import com.xiaoleilu.hutool.io.file.FileReader;
import com.xiaoleilu.hutool.log.Log;
import com.xiaoleilu.hutool.log.LogFactory;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpUserList;
import me.chanjar.weixin.mp.bean.tag.WxTagListUser;
import me.chanjar.weixin.mp.bean.tag.WxUserTag;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 准备目标数据tab相关事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/19.
 */
public class MemberListener {
    private static final Log logger = LogFactory.get();

    private static Map<String, Long> userTagMap = new HashMap<>();

    public static void addListeners() {
        // 从文件导入按钮事件
        MainWindow.mainWindow.getImportFromFileButton().addActionListener(e -> new Thread(() -> {
            File file = new File(MainWindow.mainWindow.getMemberFilePathField().getText());
            CSVReader reader = null;
            FileReader fileReader = null;

            int currentImported = 0;

            try {
                MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(true);
                if (file.getName().endsWith(".csv")) {
                    // 可以解决中文乱码问题
                    DataInputStream in = new DataInputStream(new FileInputStream(file));
                    reader = new CSVReader(new InputStreamReader(in, "utf-8"));
                    String[] nextLine;
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                    while ((nextLine = reader.readNext()) != null) {
                        PushData.allUser.add(nextLine);
                        currentImported++;
                        MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                    }
                } else {
                    fileReader = new FileReader(file);
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                    BufferedReader br = fileReader.getReader();
                    String line;
                    while ((line = br.readLine()) != null) {
                        PushData.allUser.add(line.split(","));
                        currentImported++;
                        MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                    }
                }
                MainWindow.mainWindow.getMemberTabImportProgressBar().setMaximum(100);
                MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(100);
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
                MainWindow.mainWindow.getMemberTabImportProgressBar().setMaximum(100);
                MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(100);
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
        }).start());

        // 导入全员按钮事件
        MainWindow.mainWindow.getMemberImportAllButton().addActionListener(e -> new Thread(() -> {
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
        }).start());

        // 刷新可选的标签按钮事件
        MainWindow.mainWindow.getMemberImportTagFreshButton().addActionListener(e -> {
            WxMpService wxMpService = PushManage.getWxMpService();
            if (wxMpService.getWxMpConfigStorage() == null) {
                return;
            }

            try {
                List<WxUserTag> wxUserTagList = wxMpService.getUserTagService().tagGet();

                MainWindow.mainWindow.getMemberImportTagComboBox().removeAllItems();
                userTagMap = new HashMap<>();

                for (WxUserTag wxUserTag : wxUserTagList) {
                    String item = wxUserTag.getName() + "/" + wxUserTag.getCount() + "用户";
                    MainWindow.mainWindow.getMemberImportTagComboBox().addItem(item);
                    userTagMap.put(item, wxUserTag.getId());
                }

            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "刷新失败！", "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            }
        });

        // 导入选择的标签分组用户按钮事件
        MainWindow.mainWindow.getMemberImportTagButton().addActionListener(e -> new Thread(() -> {
            try {
                if (MainWindow.mainWindow.getMemberImportTagComboBox().getSelectedItem() != null
                        && StringUtils.isNotEmpty(MainWindow.mainWindow.getMemberImportTagComboBox().getSelectedItem().toString())) {

                    long selectedTagId = userTagMap.get(MainWindow.mainWindow.getMemberImportTagComboBox().getSelectedItem());
                    getMpUserListByTag(selectedTagId);
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "请先选择需要导入的标签！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            } finally {
                MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
            }
        }).start());

        // 清除按钮事件
        MainWindow.mainWindow.getClearImportButton().addActionListener(e -> {
            int isClear = JOptionPane.showConfirmDialog(MainWindow.mainWindow.getMemberPanel(), "确认清除？", "确认",
                    JOptionPane.INFORMATION_MESSAGE);
            if (isClear == JOptionPane.YES_OPTION) {
                if (PushData.allUser != null) {
                    PushData.allUser.clear();
                    MainWindow.mainWindow.getMemberTabCountLabel().setText("0");
                }
            }
        });

        // 从历史导入按钮事件
        MainWindow.mainWindow.getImportFromHisButton().addActionListener(e -> new Thread(() -> {
            File file = new File(SystemUtil.configHome + "data/push_his" + File.separator
                    + MainWindow.mainWindow.getMemberHisComboBox().getSelectedItem().toString());
            CSVReader reader = null;
            FileReader fileReader = null;

            int currentImported = 0;

            try {
                MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(true);
                // 可以解决中文乱码问题
                DataInputStream in = new DataInputStream(new FileInputStream(file));
                reader = new CSVReader(new InputStreamReader(in, "utf-8"));
                String[] nextLine;
                PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                while ((nextLine = reader.readNext()) != null) {
                    PushData.allUser.add(nextLine);
                    currentImported++;
                    MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                }
                MainWindow.mainWindow.getMemberTabImportProgressBar().setMaximum(100);
                MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(100);
                MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入完成！", "完成",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            } finally {
                MainWindow.mainWindow.getMemberTabImportProgressBar().setMaximum(100);
                MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(100);
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
        }).start());

        // 从sql导入 按钮事件
        MainWindow.mainWindow.getImportFromSqlButton().addActionListener(e -> new Thread(() -> {
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
                        PushData.allUser.add(new String[]{rs.getString(1).trim()});
                        currentImported++;
                        MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(currentImported));
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
                    MainWindow.mainWindow.getMemberTabImportProgressBar().setMaximum(100);
                    MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(100);
                    MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
                }
            }
        }).start());

        // 浏览按钮
        MainWindow.mainWindow.getMemberImportExploreButton().addActionListener(e -> {
            File beforeFile = new File(MainWindow.mainWindow.getMemberFilePathField().getText());
            JFileChooser fileChooser;

            if (beforeFile.exists()) {
                fileChooser = new JFileChooser(beforeFile);
            } else {
                fileChooser = new JFileChooser();
            }

            FileFilter filter = new FileNameExtensionFilter("*.txt,*.csv", "txt", "csv", "TXT", "CSV");
            fileChooser.setFileFilter(filter);

            int approve = fileChooser.showOpenDialog(MainWindow.mainWindow.getSettingPanel());
            if (approve == JFileChooser.APPROVE_OPTION) {
                MainWindow.mainWindow.getMemberFilePathField().setText(fileChooser.getSelectedFile().getAbsolutePath());
            }

        });
    }

    /**
     * 拉取公众平台用户列表
     */
    public static void getMpUserList() throws WxErrorException {
        MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(true);

        WxMpService wxMpService = PushManage.getWxMpService();
        if (wxMpService.getWxMpConfigStorage() == null) {
            return;
        }

        WxMpUserList wxMpUserList = wxMpService.getUserService().userList(null);

        PushManage.console("关注该公众账号的总用户数：" + wxMpUserList.getTotal());
        PushManage.console("拉取的OPENID个数：" + wxMpUserList.getCount());

        MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
        MainWindow.mainWindow.getMemberTabImportProgressBar().setMaximum((int) wxMpUserList.getTotal());
        int importedCount = 0;
        PushData.allUser = Collections.synchronizedList(new ArrayList<>());

        if (wxMpUserList.getCount() == 0) {
            MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(importedCount));
            MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(importedCount);
            return;
        }

        List<String> openIds = wxMpUserList.getOpenids();

        for (String openId : openIds) {
            PushData.allUser.add(new String[]{openId});
            importedCount++;
            MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(importedCount));
            MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(importedCount);
        }

        while (StringUtils.isNotEmpty(wxMpUserList.getNextOpenid())) {
            wxMpUserList = wxMpService.getUserService().userList(wxMpUserList.getNextOpenid());

            PushManage.console("拉取的OPENID个数：" + wxMpUserList.getCount());

            if (wxMpUserList.getCount() == 0) {
                break;
            }
            openIds = wxMpUserList.getOpenids();
            for (String openId : openIds) {
                PushData.allUser.add(new String[]{openId});
                importedCount++;
                MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(importedCount));
                MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(importedCount);
            }
        }

        MainWindow.mainWindow.getMemberTabImportProgressBar().setValue((int) wxMpUserList.getTotal());
    }

    /**
     * 按标签拉取公众平台用户列表
     */
    public static void getMpUserListByTag(Long tagId) throws WxErrorException {
        MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(true);

        WxMpService wxMpService = PushManage.getWxMpService();
        if (wxMpService.getWxMpConfigStorage() == null) {
            return;
        }

        WxTagListUser wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, "");

        PushManage.console("拉取的OPENID个数：" + wxTagListUser.getCount());

        MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
        MainWindow.mainWindow.getMemberTabImportProgressBar().setMaximum((int) wxTagListUser.getCount());
        int importedCount = 0;
        PushData.allUser = Collections.synchronizedList(new ArrayList<>());

        if (wxTagListUser.getCount() == 0) {
            MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(importedCount));
            MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(importedCount);
            return;
        }

        List<String> openIds = wxTagListUser.getData().getOpenidList();

        for (String openId : openIds) {
            PushData.allUser.add(new String[]{openId});
            importedCount++;
            MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(importedCount));
            MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(importedCount);
        }

        while (StringUtils.isNotEmpty(wxTagListUser.getNextOpenid())) {
            wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, wxTagListUser.getNextOpenid());

            MainWindow.mainWindow.getMemberTabImportProgressBar().setMaximum((int) wxTagListUser.getCount());
            int progressValue = 0;
            MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(progressValue);
            PushManage.console("拉取的OPENID个数：" + wxTagListUser.getCount());

            if (wxTagListUser.getCount() == 0) {
                break;
            }
            openIds = wxTagListUser.getData().getOpenidList();
            for (String openId : openIds) {
                PushData.allUser.add(new String[]{openId});
                importedCount++;
                progressValue++;
                MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(importedCount));
                MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(progressValue);
            }

        }

    }
}
