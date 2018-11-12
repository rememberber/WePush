package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.fangxuele.tool.push.util.DbUtilMySQL;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.PushManage;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.MainWindow;
import com.fangxuele.tool.push.util.SystemUtil;
import com.opencsv.CSVReader;
import me.chanjar.weixin.common.error.WxErrorException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 准备目标数据tab相关事件监听
 * Created by rememberber(https://github.com/rememberber) on 2017/6/19.
 */
public class MemberListener {
    private static final Log logger = LogFactory.get();

    private static Map<String, Long> userTagMap = new HashMap<>();

    /**
     * 用于导入多个标签的用户时去重判断
     */
    private static Set<String> tagUserSet;

    public static void addListeners() {
        // 从文件导入按钮事件
        MainWindow.mainWindow.getImportFromFileButton().addActionListener(e -> new Thread(() -> {
            File file = new File(MainWindow.mainWindow.getMemberFilePathField().getText());
            CSVReader reader = null;
            FileReader fileReader = null;

            int currentImported = 0;

            try {
                MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(true);
                String fileNameLowerCase = file.getName().toLowerCase();
                if (fileNameLowerCase.endsWith(".csv")) {
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
                } else if (fileNameLowerCase.endsWith(".xlsx") || fileNameLowerCase.endsWith(".xls")) {
                    ExcelReader excelReader = ExcelUtil.getReader(file);
                    List<List<Object>> readAll = excelReader.read(1, Integer.MAX_VALUE);
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                    for (List<Object> objects : readAll) {
                        if (objects != null && objects.size() > 0) {
                            String[] nextLine = new String[objects.size()];
                            for (int i = 0; i < objects.size(); i++) {
                                nextLine[i] = objects.get(i).toString();
                            }
                            PushData.allUser.add(nextLine);
                            currentImported++;
                            MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                        }
                    }
                } else if (fileNameLowerCase.endsWith(".txt")) {
                    fileReader = new FileReader(file);
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                    BufferedReader br = fileReader.getReader();
                    String line;
                    while ((line = br.readLine()) != null) {
                        PushData.allUser.add(line.split(","));
                        currentImported++;
                        MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                    }
                } else {
                    JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "不支持该格式的文件！", "文件格式不支持",
                            JOptionPane.ERROR_MESSAGE);
                    return;
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
                JOptionPane.showMessageDialog(MainWindow.mainWindow.getMemberPanel(), "刷新失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            }
        });

        // 导入选择的标签分组用户按钮事件(取并集)
        MainWindow.mainWindow.getMemberImportTagButton().addActionListener(e -> new Thread(() -> {
            try {
                if (MainWindow.mainWindow.getMemberImportTagComboBox().getSelectedItem() != null
                        && StringUtils.isNotEmpty(MainWindow.mainWindow.getMemberImportTagComboBox().getSelectedItem().toString())) {

                    long selectedTagId = userTagMap.get(MainWindow.mainWindow.getMemberImportTagComboBox().getSelectedItem());
                    getMpUserListByTag(selectedTagId, false);
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
                MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(MainWindow.mainWindow.getMemberTabImportProgressBar().getMaximum());
            }
        }).start());

        // 导入选择的标签分组用户按钮事件(取交集)
        MainWindow.mainWindow.getMemberImportTagRetainButton().addActionListener(e -> new Thread(() -> {
            try {
                if (MainWindow.mainWindow.getMemberImportTagComboBox().getSelectedItem() != null
                        && StringUtils.isNotEmpty(MainWindow.mainWindow.getMemberImportTagComboBox().getSelectedItem().toString())) {

                    long selectedTagId = userTagMap.get(MainWindow.mainWindow.getMemberImportTagComboBox().getSelectedItem());
                    getMpUserListByTag(selectedTagId, true);
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
                MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(MainWindow.mainWindow.getMemberTabImportProgressBar().getMaximum());
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
                tagUserSet = null;
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

            FileFilter filter = new FileNameExtensionFilter("*.txt,*.csv,*.xlsx,*.xls", "txt", "csv", "TXT", "CSV", "xlsx", "xls");
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
     *
     * @param tagId
     * @param retain 是否取交集
     * @throws WxErrorException
     */
    public static void getMpUserListByTag(Long tagId, boolean retain) throws WxErrorException {
        MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(true);

        WxMpService wxMpService = PushManage.getWxMpService();
        if (wxMpService.getWxMpConfigStorage() == null) {
            return;
        }

        WxTagListUser wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, "");

        PushManage.console("拉取的OPENID个数：" + wxTagListUser.getCount());

        if (wxTagListUser.getCount() == 0) {
            return;
        }

        List<String> openIds = wxTagListUser.getData().getOpenidList();

        if (tagUserSet == null) {
            tagUserSet = Collections.synchronizedSet(new HashSet<>());
            tagUserSet.addAll(openIds);
        }

        if (retain) {
            // 取交集
            tagUserSet.retainAll(openIds);
        } else {
            // 无重复并集
            openIds.removeAll(tagUserSet);
            tagUserSet.addAll(openIds);
        }


        while (StringUtils.isNotEmpty(wxTagListUser.getNextOpenid())) {
            wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, wxTagListUser.getNextOpenid());

            PushManage.console("拉取的OPENID个数：" + wxTagListUser.getCount());

            if (wxTagListUser.getCount() == 0) {
                break;
            }
            openIds = wxTagListUser.getData().getOpenidList();

            if (retain) {
                // 取交集
                tagUserSet.retainAll(openIds);
            } else {
                // 无重复并集
                openIds.removeAll(tagUserSet);
                tagUserSet.addAll(openIds);
            }
        }

        PushData.allUser = Collections.synchronizedList(new ArrayList<>());
        for (String openId : tagUserSet) {
            PushData.allUser.add(new String[]{openId});
        }

        MainWindow.mainWindow.getMemberTabCountLabel().setText(String.valueOf(PushData.allUser.size()));
        MainWindow.mainWindow.getMemberTabImportProgressBar().setIndeterminate(false);
        MainWindow.mainWindow.getMemberTabImportProgressBar().setValue(MainWindow.mainWindow.getMemberTabImportProgressBar().getMaximum());

    }
}
