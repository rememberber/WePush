package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.PushManage;
import com.fangxuele.tool.push.ui.Init;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MemberForm;
import com.fangxuele.tool.push.util.CharSetUtil;
import com.fangxuele.tool.push.util.DbUtilMySQL;
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
 * <pre>
 * 准备目标数据tab相关事件监听
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2017/6/19.
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
        MemberForm.memberForm.getImportFromFileButton().addActionListener(e -> ThreadUtil.execute(() -> {
            File file = new File(MemberForm.memberForm.getMemberFilePathField().getText());
            CSVReader reader = null;
            FileReader fileReader = null;

            int currentImported = 0;

            try {
                MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(true);
                String fileNameLowerCase = file.getName().toLowerCase();
                if (fileNameLowerCase.endsWith(".csv")) {
                    // 可以解决中文乱码问题
                    DataInputStream in = new DataInputStream(new FileInputStream(file));
                    reader = new CSVReader(new InputStreamReader(in, CharSetUtil.getCharSet(file)));
                    String[] nextLine;
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                    while ((nextLine = reader.readNext()) != null) {
                        PushData.allUser.add(nextLine);
                        currentImported++;
                        MemberForm.memberForm.getMemberTabCountLabel().setText(String.valueOf(currentImported));
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
                            MemberForm.memberForm.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                        }
                    }
                } else if (fileNameLowerCase.endsWith(".txt")) {
                    fileReader = new FileReader(file, CharSetUtil.getCharSetName(file));
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                    BufferedReader br = fileReader.getReader();
                    String line;
                    while ((line = br.readLine()) != null) {
                        PushData.allUser.add(line.split(","));
                        currentImported++;
                        MemberForm.memberForm.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                    }
                } else {
                    JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "不支持该格式的文件！", "文件格式不支持",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                MemberForm.memberForm.getMemberTabImportProgressBar().setMaximum(100);
                MemberForm.memberForm.getMemberTabImportProgressBar().setValue(100);
                MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(false);
                JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "导入完成！", "完成",
                        JOptionPane.INFORMATION_MESSAGE);

                Init.config.setMemberFilePath(MemberForm.memberForm.getMemberFilePathField().getText());
                Init.config.save();
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            } finally {
                MemberForm.memberForm.getMemberTabImportProgressBar().setMaximum(100);
                MemberForm.memberForm.getMemberTabImportProgressBar().setValue(100);
                MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(false);
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        logger.error(e1);
                        e1.printStackTrace();
                    }
                }
            }
        }));

        // 导入全员按钮事件
        MemberForm.memberForm.getMemberImportAllButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                getMpUserList();
                JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "导入完成！", "完成",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            } finally {
                MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(false);
            }
        }));

        // 刷新可选的标签按钮事件
        MemberForm.memberForm.getMemberImportTagFreshButton().addActionListener(e -> {
            WxMpService wxMpService = PushManage.getWxMpService();
            if (wxMpService.getWxMpConfigStorage() == null) {
                return;
            }

            try {
                List<WxUserTag> wxUserTagList = wxMpService.getUserTagService().tagGet();

                MemberForm.memberForm.getMemberImportTagComboBox().removeAllItems();
                userTagMap = new HashMap<>();

                for (WxUserTag wxUserTag : wxUserTagList) {
                    String item = wxUserTag.getName() + "/" + wxUserTag.getCount() + "用户";
                    MemberForm.memberForm.getMemberImportTagComboBox().addItem(item);
                    userTagMap.put(item, wxUserTag.getId());
                }

            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "刷新失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            }
        });

        // 导入选择的标签分组用户按钮事件(取并集)
        MemberForm.memberForm.getMemberImportTagButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                if (MemberForm.memberForm.getMemberImportTagComboBox().getSelectedItem() != null
                        && StringUtils.isNotEmpty(MemberForm.memberForm.getMemberImportTagComboBox().getSelectedItem().toString())) {

                    long selectedTagId = userTagMap.get(MemberForm.memberForm.getMemberImportTagComboBox().getSelectedItem());
                    getMpUserListByTag(selectedTagId, false);
                    JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "请先选择需要导入的标签！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            } finally {
                MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(false);
                MemberForm.memberForm.getMemberTabImportProgressBar().setValue(MemberForm.memberForm.getMemberTabImportProgressBar().getMaximum());
            }
        }));

        // 导入选择的标签分组用户按钮事件(取交集)
        MemberForm.memberForm.getMemberImportTagRetainButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                if (MemberForm.memberForm.getMemberImportTagComboBox().getSelectedItem() != null
                        && StringUtils.isNotEmpty(MemberForm.memberForm.getMemberImportTagComboBox().getSelectedItem().toString())) {

                    long selectedTagId = userTagMap.get(MemberForm.memberForm.getMemberImportTagComboBox().getSelectedItem());
                    getMpUserListByTag(selectedTagId, true);
                    JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "请先选择需要导入的标签！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            } finally {
                MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(false);
                MemberForm.memberForm.getMemberTabImportProgressBar().setValue(MemberForm.memberForm.getMemberTabImportProgressBar().getMaximum());
            }
        }));

        // 清除按钮事件
        MemberForm.memberForm.getClearImportButton().addActionListener(e -> {
            int isClear = JOptionPane.showConfirmDialog(MemberForm.memberForm.getMemberPanelRight(), "确认清除？", "确认",
                    JOptionPane.YES_NO_OPTION);
            if (isClear == JOptionPane.YES_OPTION) {
                if (PushData.allUser != null) {
                    PushData.allUser.clear();
                    MemberForm.memberForm.getMemberTabCountLabel().setText("0");
                }
                tagUserSet = null;
            }
        });

        // 从sql导入 按钮事件
        MemberForm.memberForm.getImportFromSqlButton().addActionListener(e -> ThreadUtil.execute(() -> {
            MemberForm.memberForm.getImportFromSqlButton().setEnabled(false);
            MemberForm.memberForm.getImportFromSqlButton().updateUI();

            DbUtilMySQL dbUtilMySQL = DbUtilMySQL.getInstance();// 获取SQLServer连接实例

            String querySql = MemberForm.memberForm.getImportFromSqlTextArea().getText();

            MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(true);
            if (StringUtils.isNotEmpty(querySql)) {
                try {
                    ResultSet rs = dbUtilMySQL.executeQuery(querySql);// 表查询
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                    int currentImported = 0;

                    while (rs.next()) {
                        PushData.allUser.add(new String[]{rs.getString(1).trim()});
                        currentImported++;
                        MemberForm.memberForm.getMemberTabCountLabel().setText(String.valueOf(currentImported));
                    }

                    JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);

                    Init.config.setMemberSql(querySql);
                    Init.config.save();
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(MemberForm.memberForm.getMemberPanelRight(), "导入失败！\n\n" + e1.getMessage(), "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(e1);
                    e1.printStackTrace();
                } finally {
                    MemberForm.memberForm.getImportFromSqlButton().setEnabled(true);
                    MemberForm.memberForm.getImportFromSqlButton().updateUI();
                    MemberForm.memberForm.getMemberTabImportProgressBar().setMaximum(100);
                    MemberForm.memberForm.getMemberTabImportProgressBar().setValue(100);
                    MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(false);
                }
            }
        }));

        // 浏览按钮
        MemberForm.memberForm.getMemberImportExploreButton().addActionListener(e -> {
            File beforeFile = new File(MemberForm.memberForm.getMemberFilePathField().getText());
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
                MemberForm.memberForm.getMemberFilePathField().setText(fileChooser.getSelectedFile().getAbsolutePath());
            }

        });
    }

    /**
     * 拉取公众平台用户列表
     */
    public static void getMpUserList() throws WxErrorException {
        MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(true);

        WxMpService wxMpService = PushManage.getWxMpService();
        if (wxMpService.getWxMpConfigStorage() == null) {
            return;
        }

        WxMpUserList wxMpUserList = wxMpService.getUserService().userList(null);

        PushManage.console("关注该公众账号的总用户数：" + wxMpUserList.getTotal());
        PushManage.console("拉取的OPENID个数：" + wxMpUserList.getCount());

        MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(false);
        MemberForm.memberForm.getMemberTabImportProgressBar().setMaximum((int) wxMpUserList.getTotal());
        int importedCount = 0;
        PushData.allUser = Collections.synchronizedList(new ArrayList<>());

        if (wxMpUserList.getCount() == 0) {
            MemberForm.memberForm.getMemberTabCountLabel().setText(String.valueOf(importedCount));
            MemberForm.memberForm.getMemberTabImportProgressBar().setValue(importedCount);
            return;
        }

        List<String> openIds = wxMpUserList.getOpenids();

        for (String openId : openIds) {
            PushData.allUser.add(new String[]{openId});
            importedCount++;
            MemberForm.memberForm.getMemberTabCountLabel().setText(String.valueOf(importedCount));
            MemberForm.memberForm.getMemberTabImportProgressBar().setValue(importedCount);
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
                MemberForm.memberForm.getMemberTabCountLabel().setText(String.valueOf(importedCount));
                MemberForm.memberForm.getMemberTabImportProgressBar().setValue(importedCount);
            }
        }

        MemberForm.memberForm.getMemberTabImportProgressBar().setValue((int) wxMpUserList.getTotal());
    }

    /**
     * 按标签拉取公众平台用户列表
     *
     * @param tagId
     * @param retain 是否取交集
     * @throws WxErrorException
     */
    public static void getMpUserListByTag(Long tagId, boolean retain) throws WxErrorException {
        MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(true);

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

        MemberForm.memberForm.getMemberTabCountLabel().setText(String.valueOf(PushData.allUser.size()));
        MemberForm.memberForm.getMemberTabImportProgressBar().setIndeterminate(false);
        MemberForm.memberForm.getMemberTabImportProgressBar().setValue(MemberForm.memberForm.getMemberTabImportProgressBar().getMaximum());

    }
}
