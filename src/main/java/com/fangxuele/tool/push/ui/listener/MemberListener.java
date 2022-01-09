package com.fangxuele.tool.push.ui.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.DbUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiDepartmentListRequest;
import com.dingtalk.api.request.OapiUserSimplelistRequest;
import com.dingtalk.api.response.OapiDepartmentListResponse;
import com.dingtalk.api.response.OapiUserSimplelistResponse;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TWxMpUserMapper;
import com.fangxuele.tool.push.domain.TWxMpUser;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.msgsender.DingMsgSender;
import com.fangxuele.tool.push.logic.msgsender.WxCpMsgSender;
import com.fangxuele.tool.push.logic.msgsender.WxMpTemplateMsgSender;
import com.fangxuele.tool.push.ui.component.TableInCellImageLabelRenderer;
import com.fangxuele.tool.push.ui.dialog.ExportDialog;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.MemberForm;
import com.fangxuele.tool.push.ui.form.msg.DingMsgForm;
import com.fangxuele.tool.push.ui.form.msg.WxCpMsgForm;
import com.fangxuele.tool.push.util.*;
import com.google.common.collect.Maps;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.bean.WxCpDepart;
import me.chanjar.weixin.cp.bean.WxCpTag;
import me.chanjar.weixin.cp.bean.WxCpUser;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.bean.result.WxMpUserList;
import me.chanjar.weixin.mp.bean.tag.WxTagListUser;
import me.chanjar.weixin.mp.bean.tag.WxUserTag;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.sql.Connection;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

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

    public static Map<String, Long> userTagMap = new HashMap<>();

    /**
     * 用于导入多个标签的用户时去重判断
     */
    public static Set<String> tagUserSet;

    public static final String TXT_FILE_DATA_SEPERATOR_REGEX = "\\|";

    private static List<String> toSearchRowsList;

    /**
     * 企业号标签名称->标签ID
     */
    private static Map<String, String> wxCpTagNameToIdMap = Maps.newHashMap();

    /**
     * 企业号标签ID>标签名称
     */
    private static Map<String, String> wxCpIdToTagNameMap = Maps.newHashMap();

    /**
     * 企业号部门名称->部门ID
     */
    private static Map<String, Long> wxCpDeptNameToIdMap = Maps.newHashMap();

    /**
     * 企业号部门ID>部门名称
     */
    private static Map<Long, String> wxCpIdToDeptNameMap = Maps.newHashMap();

    private static TWxMpUserMapper tWxMpUserMapper = MybatisUtil.getSqlSession().getMapper(TWxMpUserMapper.class);

    public static void addListeners() {
        MemberForm memberForm = MemberForm.getInstance();
        JProgressBar progressBar = memberForm.getMemberTabImportProgressBar();
        JTextField filePathField = memberForm.getMemberFilePathField();
        JLabel memberCountLabel = memberForm.getMemberTabCountLabel();
        JPanel memberPanel = memberForm.getMemberPanel();
        JTable memberListTable = memberForm.getMemberListTable();

        // 按数量导入按钮事件
        memberForm.getImportFromNumButton().addActionListener(e -> ThreadUtil.execute(() -> {
            importByNum(memberForm, progressBar, memberCountLabel, memberPanel);
        }));

        memberForm.getImportNumTextField().addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent evt) {

            }

            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    importByNum(memberForm, progressBar, memberCountLabel, memberPanel);
                }
            }
        });

        // 从文件导入按钮事件
        memberForm.getImportFromFileButton().addActionListener(e -> ThreadUtil.execute(MemberListener::importFromFile));

        // 从sql导入 按钮事件
        memberForm.getImportFromSqlButton().addActionListener(e -> ThreadUtil.execute(MemberListener::importFromSql));

        // 公众号-导入全员按钮事件
        memberForm.getMemberImportAllButton().addActionListener(e -> ThreadUtil.execute(MemberListener::importWxAll));

        // 公众号-刷新可选的标签按钮事件
        memberForm.getMemberImportTagFreshButton().addActionListener(e -> {
            WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService();
            if (wxMpService.getWxMpConfigStorage() == null) {
                return;
            }

            try {
                List<WxUserTag> wxUserTagList = wxMpService.getUserTagService().tagGet();

                memberForm.getMemberImportTagComboBox().removeAllItems();
                userTagMap = new HashMap<>();

                for (WxUserTag wxUserTag : wxUserTagList) {
                    String item = wxUserTag.getName() + "/" + wxUserTag.getCount() + "用户";
                    memberForm.getMemberImportTagComboBox().addItem(item);
                    userTagMap.put(item, wxUserTag.getId());
                }

            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(memberPanel, "刷新失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            }
        });

        // 公众号-导入选择的标签分组用户按钮事件(取并集)
        memberForm.getMemberImportTagButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                if (memberForm.getMemberImportTagComboBox().getSelectedItem() != null
                        && StringUtils.isNotEmpty(memberForm.getMemberImportTagComboBox().getSelectedItem().toString())) {

                    long selectedTagId = userTagMap.get(memberForm.getMemberImportTagComboBox().getSelectedItem());
                    getMpUserListByTag(selectedTagId, false);
                    renderMemberListTable();
                    JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(memberPanel, "请先选择需要导入的标签！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            } finally {
                progressBar.setIndeterminate(false);
                progressBar.setValue(progressBar.getMaximum());
                progressBar.setVisible(false);
            }
        }));

        // 公众号-导入选择的标签分组用户按钮事件(取交集)
        memberForm.getMemberImportTagRetainButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                if (memberForm.getMemberImportTagComboBox().getSelectedItem() != null
                        && StringUtils.isNotEmpty(memberForm.getMemberImportTagComboBox().getSelectedItem().toString())) {

                    long selectedTagId = userTagMap.get(memberForm.getMemberImportTagComboBox().getSelectedItem());
                    getMpUserListByTag(selectedTagId, true);
                    renderMemberListTable();
                    JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(memberPanel, "请先选择需要导入的标签！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            } finally {
                progressBar.setIndeterminate(false);
                progressBar.setValue(progressBar.getMaximum());
                progressBar.setVisible(false);
            }
        }));

        // 公众号-排除选择的标签分组用户按钮事件
        memberForm.getMemberImportTagExceptButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                if (memberForm.getMemberImportTagComboBox().getSelectedItem() != null
                        && StringUtils.isNotEmpty(memberForm.getMemberImportTagComboBox().getSelectedItem().toString())) {

                    long selectedTagId = userTagMap.get(memberForm.getMemberImportTagComboBox().getSelectedItem());
                    removeMpUserListByTag(selectedTagId);
                    renderMemberListTable();
                    JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(memberPanel, "请先选择需要导入的标签！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            } finally {
                progressBar.setIndeterminate(false);
                progressBar.setValue(progressBar.getMaximum());
                progressBar.setVisible(false);
            }
        }));

        // 公众号-清空本地缓存按钮事件
        memberForm.getClearDbCacheButton().addActionListener(e -> {
            int count = tWxMpUserMapper.deleteAll();
            JOptionPane.showMessageDialog(memberPanel, "清理完毕！\n\n共清理：" + count + "条本地数据", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        // 企业号-按标签导入-刷新
        memberForm.getWxCpTagsRefreshButton().addActionListener(e -> {
            ThreadUtil.execute(() -> {
                if (WxCpMsgForm.getInstance().getAppNameComboBox().getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "请先在编辑消息tab中选择应用！", "提示",
                            JOptionPane.ERROR_MESSAGE);
                    MainWindow.getInstance().getTabbedPane().setSelectedIndex(2);
                    return;
                }
                memberForm.getWxCpTagsComboBox().removeAllItems();

                try {
                    // 获取标签列表
                    List<WxCpTag> wxCpTagList = WxCpMsgSender.getWxCpService().getTagService().listAll();
                    for (WxCpTag wxCpTag : wxCpTagList) {
                        memberForm.getWxCpTagsComboBox().addItem(wxCpTag.getName());
                        wxCpTagNameToIdMap.put(wxCpTag.getName(), wxCpTag.getId());
                        wxCpIdToTagNameMap.put(wxCpTag.getId(), wxCpTag.getName());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(memberPanel, "刷新失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ex.toString());
                }
            });
        });

        // 企业号-按标签导入-导入
        memberForm.getWxCpTagsImportButton().addActionListener(e -> {
            ThreadUtil.execute(() -> {
                if (memberForm.getWxCpTagsComboBox().getSelectedItem() == null) {
                    return;
                }
                try {
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                    int importedCount = 0;
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());

                    // 获取标签id
                    String tagId = wxCpTagNameToIdMap.get(memberForm.getWxCpTagsComboBox().getSelectedItem());
                    // 获取用户
                    List<WxCpUser> wxCpUsers = WxCpMsgSender.getWxCpService().getTagService().listUsersByTagId(tagId);
                    for (WxCpUser wxCpUser : wxCpUsers) {
                        Long[] depIds = wxCpUser.getDepartIds();
                        List<String> deptNameList = Lists.newArrayList();
                        if (depIds != null) {
                            for (Long depId : depIds) {
                                deptNameList.add(wxCpIdToDeptNameMap.get(depId));
                            }
                        }
                        String[] dataArray = new String[]{wxCpUser.getUserId(), wxCpUser.getName(), String.join("/", deptNameList)};
                        PushData.allUser.add(dataArray);
                        importedCount++;
                        memberCountLabel.setText(String.valueOf(importedCount));
                    }
                    renderMemberListTable();
                    if (!PushData.fixRateScheduling) {
                        JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ex.toString());
                } finally {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                }

            });
        });

        // 企业号-按部门导入-刷新
        memberForm.getWxCpDeptsRefreshButton().addActionListener(e -> {
            ThreadUtil.execute(() -> {
                if (WxCpMsgForm.getInstance().getAppNameComboBox().getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "请先在编辑消息tab中选择应用！", "提示",
                            JOptionPane.ERROR_MESSAGE);
                    MainWindow.getInstance().getTabbedPane().setSelectedIndex(2);
                    return;
                }
                memberForm.getWxCpDeptsComboBox().removeAllItems();

                try {
                    // 获取部门列表
                    List<WxCpDepart> wxCpDepartList = WxCpMsgSender.getWxCpService().getDepartmentService().list(null);
                    for (WxCpDepart wxCpDepart : wxCpDepartList) {
                        memberForm.getWxCpDeptsComboBox().addItem(wxCpDepart.getName());
                        wxCpDeptNameToIdMap.put(wxCpDepart.getName(), wxCpDepart.getId());
                        wxCpIdToDeptNameMap.put(wxCpDepart.getId(), wxCpDepart.getName());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(memberPanel, "刷新失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ex.toString());
                }
            });
        });

        // 企业号-按部门导入-导入
        memberForm.getWxCpDeptsImportButton().addActionListener(e -> {
            ThreadUtil.execute(() -> {
                if (memberForm.getWxCpDeptsComboBox().getSelectedItem() == null) {
                    return;
                }
                try {
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                    int importedCount = 0;
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());

                    // 获取部门id
                    Long deptId = wxCpDeptNameToIdMap.get(memberForm.getWxCpDeptsComboBox().getSelectedItem());
                    // 获取用户
                    List<WxCpUser> wxCpUsers = WxCpMsgSender.getWxCpService().getUserService().listByDepartment(deptId, true, 0);
                    for (WxCpUser wxCpUser : wxCpUsers) {
                        String statusStr = "";
                        if (wxCpUser.getStatus() == 1) {
                            statusStr = "已关注";
                        } else if (wxCpUser.getStatus() == 2) {
                            statusStr = "已冻结";
                        } else if (wxCpUser.getStatus() == 4) {
                            statusStr = "未关注";
                        }
                        Long[] depIds = wxCpUser.getDepartIds();
                        List<String> deptNameList = Lists.newArrayList();
                        if (depIds != null) {
                            for (Long depId : depIds) {
                                deptNameList.add(wxCpIdToDeptNameMap.get(depId));
                            }
                        }
                        String[] dataArray = new String[]{wxCpUser.getUserId(), wxCpUser.getName(), wxCpUser.getGender().getGenderName(), wxCpUser.getEmail(), String.join("/", deptNameList), wxCpUser.getPosition(), statusStr};
                        PushData.allUser.add(dataArray);
                        importedCount++;
                        memberCountLabel.setText(String.valueOf(importedCount));
                    }
                    renderMemberListTable();
                    if (!PushData.fixRateScheduling) {
                        JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ex.toString());
                } finally {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                }

            });
        });

        // 企业号-导入全部
        memberForm.getWxCpImportAllButton().addActionListener(e -> {
            ThreadUtil.execute(() -> {
                importWxCpAll();
            });
        });

        // 钉钉-按部门导入-刷新
        memberForm.getDingDeptsRefreshButton().addActionListener(e -> {
            ThreadUtil.execute(() -> {
                if (DingMsgForm.getInstance().getAppNameComboBox().getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "请先在编辑消息tab中选择应用！", "提示",
                            JOptionPane.ERROR_MESSAGE);
                    MainWindow.getInstance().getTabbedPane().setSelectedIndex(2);
                    return;
                }
                memberForm.getDingDeptsComboBox().removeAllItems();

                try {
                    // 获取部门列表
                    DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/department/list");
                    OapiDepartmentListRequest request = new OapiDepartmentListRequest();
                    request.setHttpMethod("GET");
                    OapiDepartmentListResponse response = client.execute(request, DingMsgSender.getAccessTokenTimedCache().get("accessToken"));
                    if (response.getErrcode() != 0) {
                        JOptionPane.showMessageDialog(memberPanel, "刷新失败！\n\n" + response.getErrmsg(), "失败",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    List<OapiDepartmentListResponse.Department> departmentList = response.getDepartment();
                    for (OapiDepartmentListResponse.Department department : departmentList) {
                        memberForm.getDingDeptsComboBox().addItem(department.getName());
                        wxCpDeptNameToIdMap.put(department.getName(), department.getId());
                        wxCpIdToDeptNameMap.put(department.getId(), department.getName());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(memberPanel, "刷新失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ex.toString());
                }
            });
        });

        // 钉钉-按部门导入-导入
        memberForm.getDingDeptsImportButton().addActionListener(e -> {
            ThreadUtil.execute(() -> {
                if (memberForm.getDingDeptsComboBox().getSelectedItem() == null) {
                    return;
                }
                try {
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                    int importedCount = 0;
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());

                    // 获取部门id
                    Long deptId = wxCpDeptNameToIdMap.get(memberForm.getDingDeptsComboBox().getSelectedItem());
                    // 获取用户
                    DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/simplelist");
                    OapiUserSimplelistRequest request = new OapiUserSimplelistRequest();
                    request.setDepartmentId(deptId);
                    request.setOffset(0L);
                    request.setSize(100L);
                    request.setHttpMethod("GET");

                    long offset = 0;
                    OapiUserSimplelistResponse response = new OapiUserSimplelistResponse();
                    while (response.getErrcode() == null || response.getUserlist().size() > 0) {
                        response = client.execute(request, DingMsgSender.getAccessTokenTimedCache().get("accessToken"));
                        if (response.getErrcode() != 0) {
                            if (response.getErrcode() == 60011) {
                                JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + response.getErrmsg() + "\n\n进入开发者后台，在小程序或者微应用详情的「接口权限」模块，点击申请对应的通讯录接口读写权限", "失败",
                                        JOptionPane.ERROR_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + response.getErrmsg(), "失败", JOptionPane.ERROR_MESSAGE);
                            }

                            logger.error(response.getErrmsg());
                            return;
                        }
                        List<OapiUserSimplelistResponse.Userlist> userlist = response.getUserlist();
                        for (OapiUserSimplelistResponse.Userlist dingUser : userlist) {
                            String[] dataArray = new String[]{dingUser.getUserid(), dingUser.getName()};
                            PushData.allUser.add(dataArray);
                            importedCount++;
                            memberCountLabel.setText(String.valueOf(importedCount));
                        }
                        offset += 100;
                        request.setOffset(offset);
                    }
                    renderMemberListTable();
                    if (!PushData.fixRateScheduling) {
                        JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ex.toString());
                } finally {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                }

            });
        });

        // 钉钉-导入全部
        memberForm.getDingImportAllButton().addActionListener(e -> {
            ThreadUtil.execute(() -> {
                importDingAll();
            });
        });

        // 清除按钮事件
        memberForm.getClearImportButton().addActionListener(e -> {
            int isClear = JOptionPane.showConfirmDialog(memberPanel, "确认清除？", "确认",
                    JOptionPane.YES_NO_OPTION);
            if (isClear == JOptionPane.YES_OPTION) {
                MemberForm.clearMember();
            }
        });

        // 浏览按钮
        memberForm.getMemberImportExploreButton().addActionListener(e -> {
            File beforeFile = new File(filePathField.getText());
            JFileChooser fileChooser;

            if (beforeFile.exists()) {
                fileChooser = new JFileChooser(beforeFile);
            } else {
                fileChooser = new JFileChooser();
            }

            FileFilter filter = new FileNameExtensionFilter("*.txt,*.csv,*.xlsx,*.xls", "txt", "csv", "TXT", "CSV", "xlsx", "xls");
            fileChooser.setFileFilter(filter);

            int approve = fileChooser.showOpenDialog(memberPanel);
            if (approve == JFileChooser.APPROVE_OPTION) {
                filePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }

        });

        // 列表-全选按钮事件
        memberForm.getSelectAllButton().addActionListener(e -> ThreadUtil.execute(() -> memberListTable.selectAll()));

        // 列表-删除按钮事件
        memberForm.getDeleteButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                int[] selectedRows = memberListTable.getSelectedRows();
                if (selectedRows.length == 0) {
                    JOptionPane.showMessageDialog(memberPanel, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    int isDelete = JOptionPane.showConfirmDialog(memberPanel, "确认删除？", "确认",
                            JOptionPane.YES_NO_OPTION);
                    if (isDelete == JOptionPane.YES_OPTION) {
                        DefaultTableModel tableModel = (DefaultTableModel) memberListTable.getModel();
                        for (int i = selectedRows.length; i > 0; i--) {
                            tableModel.removeRow(memberListTable.getSelectedRow());
                        }
                        memberListTable.updateUI();
                    }
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(memberPanel, "删除失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }));

        // 列表-导入按钮事件
        memberForm.getImportSelectedButton().addActionListener(e -> ThreadUtil.execute(() -> {
            try {
                int[] selectedRows = memberListTable.getSelectedRows();
                if (selectedRows.length <= 0) {
                    JOptionPane.showMessageDialog(memberPanel, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                    progressBar.setIndeterminate(true);
                    progressBar.setVisible(true);
                    for (int selectedRow : selectedRows) {
                        String toImportData = (String) memberListTable.getValueAt(selectedRow, 0);
                        PushData.allUser.add(toImportData.split(TXT_FILE_DATA_SEPERATOR_REGEX));
                        memberCountLabel.setText(String.valueOf(PushData.allUser.size()));
                        progressBar.setMaximum(100);
                        progressBar.setValue(100);
                        progressBar.setIndeterminate(false);
                    }
                    JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            } finally {
                progressBar.setMaximum(100);
                progressBar.setValue(100);
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
            }
        }));

        // 列表-导出按钮事件
        memberForm.getExportButton().addActionListener(e -> ThreadUtil.execute(() -> {
            int[] selectedRows = memberListTable.getSelectedRows();
            int columnCount = memberListTable.getColumnCount();
            BigExcelWriter writer = null;
            try {
                if (selectedRows.length > 0) {
                    ExportDialog.showDialog();
                    if (ExportDialog.confirm) {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                        int approve = fileChooser.showOpenDialog(memberPanel);
                        String exportPath;
                        if (approve == JFileChooser.APPROVE_OPTION) {
                            exportPath = fileChooser.getSelectedFile().getAbsolutePath();
                        } else {
                            return;
                        }

                        List<String> rowData;
                        List<List<String>> rows = Lists.newArrayList();
                        for (int selectedRow : selectedRows) {
                            rowData = Lists.newArrayList();
                            for (int i = 0; i < columnCount; i++) {
                                String data = (String) memberListTable.getValueAt(selectedRow, i);
                                rowData.add(data);
                            }
                            rows.add(rowData);
                        }
                        String nowTime = DateUtil.now().replace(":", "_").replace(" ", "_");
                        String fileName = "MemberExport_" + MessageTypeEnum.getName(App.config.getMsgType()) + "_" + nowTime;
                        String fileFullName = exportPath + File.separator + fileName;
                        if (ExportDialog.fileType == ExportDialog.EXCEL) {
                            fileFullName += ".xlsx";
                            //通过工具类创建writer
                            writer = ExcelUtil.getBigWriter(fileFullName);
                            //合并单元格后的标题行，使用默认标题样式
                            writer.merge(rows.get(0).size() - 1, "目标用户列表导出");
                            //一次性写出内容，强制输出标题
                            writer.write(rows);
                            writer.flush();
                        } else if (ExportDialog.fileType == ExportDialog.CSV) {
                            fileFullName += ".csv";
                            CSVWriter csvWriter = new CSVWriter(new FileWriter(FileUtil.touch(fileFullName)));
                            for (List<String> row : rows) {
                                String[] array = row.toArray(new String[row.size()]);
                                csvWriter.writeNext(array);
                            }
                            csvWriter.flush();
                            csvWriter.close();
                        } else if (ExportDialog.fileType == ExportDialog.TXT) {
                            fileFullName += ".txt";
                            FileWriter fileWriter = new FileWriter(fileFullName);
                            int size = rows.size();
                            for (int i = 0; i < size; i++) {
                                List<String> row = rows.get(i);
                                fileWriter.append(String.join("|", row));
                                if (i < size - 1) {
                                    fileWriter.append(StrUtil.CRLF);
                                }
                            }
                            fileWriter.flush();
                            fileWriter.close();
                        }
                        JOptionPane.showMessageDialog(memberPanel, "导出成功！", "提示",
                                JOptionPane.INFORMATION_MESSAGE);
                        try {
                            Desktop desktop = Desktop.getDesktop();
                            desktop.open(FileUtil.file(fileFullName));
                        } catch (Exception e2) {
                            logger.error(e2);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(memberPanel, "请至少选择一个！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(memberPanel, "导出失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            }
        }));

        // 列表-搜索按钮事件
        memberForm.getSearchButton().addActionListener(e -> searchEvent());

        // 列表-搜索框键入回车
        memberForm.getSearchTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                try {
                    searchEvent();
                } catch (Exception e1) {
                    logger.error(e1);
                } finally {
                    super.keyPressed(e);
                }
            }
        });

    }

    /**
     * 按数量导入
     *
     * @param memberForm
     * @param progressBar
     * @param memberCountLabel
     * @param memberPanel
     */
    private static void importByNum(MemberForm memberForm, JProgressBar progressBar, JLabel memberCountLabel, JPanel memberPanel) {
        if (StringUtils.isBlank(memberForm.getImportNumTextField().getText())) {
            JOptionPane.showMessageDialog(memberPanel, "请填写数量！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int currentImported = 0;

        try {
            int importNum = Integer.parseInt(memberForm.getImportNumTextField().getText());
            progressBar.setVisible(true);
            progressBar.setMaximum(importNum);

            PushData.allUser = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < importNum; i++) {
                String[] array = new String[1];
                array[0] = String.valueOf(i);
                PushData.allUser.add(array);
                currentImported++;
                memberCountLabel.setText(String.valueOf(currentImported));
            }

            renderMemberListTable();

            if (!PushData.fixRateScheduling) {
                JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + e1.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            logger.error(e1);
            e1.printStackTrace();
        } finally {
            progressBar.setMaximum(100);
            progressBar.setValue(100);
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
        }
    }

    private static void searchEvent() {
        JTable memberListTable = MemberForm.getInstance().getMemberListTable();
        ThreadUtil.execute(() -> {
            int rowCount = memberListTable.getRowCount();
            int columnCount = memberListTable.getColumnCount();
            try {
                if (rowCount > 0 || toSearchRowsList != null) {
                    if (toSearchRowsList == null) {
                        toSearchRowsList = Lists.newArrayList();
                        List<String> rowData;
                        for (int i = 0; i < rowCount; i++) {
                            rowData = Lists.newArrayList();
                            for (int j = 0; j < columnCount; j++) {
                                String data = (String) memberListTable.getValueAt(i, j);
                                rowData.add(data);
                            }
                            toSearchRowsList.add(String.join("==", rowData));
                        }
                    }

                    String keyWord = MemberForm.getInstance().getSearchTextField().getText();
                    List<String> searchResultList = toSearchRowsList.parallelStream().filter(rowStr -> rowStr.contains(keyWord)).collect(Collectors.toList());

                    DefaultTableModel tableModel = (DefaultTableModel) memberListTable.getModel();
                    tableModel.setRowCount(0);
                    for (String rowString : searchResultList) {
                        tableModel.addRow(rowString.split("=="));
                    }
                }
            } catch (Exception e1) {
                logger.error(e1);
            }
        });
    }

    /**
     * 拉取公众平台用户列表
     */
    public static void getMpUserList() throws WxErrorException {
        JProgressBar progressBar = MemberForm.getInstance().getMemberTabImportProgressBar();
        JLabel memberCountLabel = MemberForm.getInstance().getMemberTabCountLabel();

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService();
        if (wxMpService.getWxMpConfigStorage() == null) {
            return;
        }

        WxMpUserList wxMpUserList = wxMpService.getUserService().userList(null);

        ConsoleUtil.consoleWithLog("关注该公众账号的总用户数：" + wxMpUserList.getTotal());
        ConsoleUtil.consoleWithLog("拉取的OPENID个数：" + wxMpUserList.getCount());

        progressBar.setIndeterminate(false);
        progressBar.setMaximum((int) wxMpUserList.getTotal());
        int importedCount = 0;
        PushData.allUser = Collections.synchronizedList(new ArrayList<>());

        if (wxMpUserList.getCount() == 0) {
            memberCountLabel.setText(String.valueOf(importedCount));
            progressBar.setValue(importedCount);
            return;
        }

        List<String> openIds = wxMpUserList.getOpenids();

        for (String openId : openIds) {
            PushData.allUser.add(new String[]{openId});
            importedCount++;
            memberCountLabel.setText(String.valueOf(importedCount));
            progressBar.setValue(importedCount);
        }

        while (StringUtils.isNotEmpty(wxMpUserList.getNextOpenid())) {
            wxMpUserList = wxMpService.getUserService().userList(wxMpUserList.getNextOpenid());

            ConsoleUtil.consoleWithLog("拉取的OPENID个数：" + wxMpUserList.getCount());

            if (wxMpUserList.getCount() == 0) {
                break;
            }
            openIds = wxMpUserList.getOpenids();
            for (String openId : openIds) {
                PushData.allUser.add(new String[]{openId});
                importedCount++;
                memberCountLabel.setText(String.valueOf(importedCount));
                progressBar.setValue(importedCount);
            }
        }

        progressBar.setValue((int) wxMpUserList.getTotal());
    }

    /**
     * 按标签拉取公众平台用户列表
     *
     * @param tagId
     * @throws WxErrorException
     */
    public static void getMpUserListByTag(Long tagId) throws WxErrorException {
        JProgressBar progressBar = MemberForm.getInstance().getMemberTabImportProgressBar();
        JLabel memberCountLabel = MemberForm.getInstance().getMemberTabCountLabel();

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService();
        if (wxMpService.getWxMpConfigStorage() == null) {
            return;
        }

        WxTagListUser wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, "");

        ConsoleUtil.consoleWithLog("拉取的OPENID个数：" + wxTagListUser.getCount());

        if (wxTagListUser.getCount() == 0) {
            return;
        }

        List<String> openIds = wxTagListUser.getData().getOpenidList();

        tagUserSet = Collections.synchronizedSet(new HashSet<>());
        tagUserSet.addAll(openIds);

        while (StringUtils.isNotEmpty(wxTagListUser.getNextOpenid())) {
            wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, wxTagListUser.getNextOpenid());

            ConsoleUtil.consoleWithLog("拉取的OPENID个数：" + wxTagListUser.getCount());

            if (wxTagListUser.getCount() == 0) {
                break;
            }
            openIds = wxTagListUser.getData().getOpenidList();

            tagUserSet.addAll(openIds);
        }

        PushData.allUser = Collections.synchronizedList(new ArrayList<>());
        for (String openId : tagUserSet) {
            PushData.allUser.add(new String[]{openId});
        }

        memberCountLabel.setText(String.valueOf(PushData.allUser.size()));
        progressBar.setIndeterminate(false);
        progressBar.setValue(progressBar.getMaximum());

    }

    /**
     * 按标签拉取公众平台用户列表
     *
     * @param tagId
     * @param retain 是否取交集
     * @throws WxErrorException
     */
    public static void getMpUserListByTag(Long tagId, boolean retain) throws WxErrorException {
        JProgressBar progressBar = MemberForm.getInstance().getMemberTabImportProgressBar();
        JLabel memberCountLabel = MemberForm.getInstance().getMemberTabCountLabel();

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService();
        if (wxMpService.getWxMpConfigStorage() == null) {
            return;
        }

        WxTagListUser wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, "");

        ConsoleUtil.consoleWithLog("拉取的OPENID个数：" + wxTagListUser.getCount());

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

            ConsoleUtil.consoleWithLog("拉取的OPENID个数：" + wxTagListUser.getCount());

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

        memberCountLabel.setText(String.valueOf(PushData.allUser.size()));
        progressBar.setIndeterminate(false);
        progressBar.setValue(progressBar.getMaximum());

    }

    /**
     * 排除所选标签
     *
     * @param tagId
     * @throws WxErrorException
     */
    private static void removeMpUserListByTag(long tagId) throws WxErrorException {
        JProgressBar progressBar = MemberForm.getInstance().getMemberTabImportProgressBar();
        JLabel memberCountLabel = MemberForm.getInstance().getMemberTabCountLabel();

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService();
        if (wxMpService.getWxMpConfigStorage() == null) {
            return;
        }

        WxTagListUser wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, "");

        ConsoleUtil.consoleWithLog("拉取的OPENID个数：" + wxTagListUser.getCount());

        if (wxTagListUser.getCount() == 0) {
            return;
        }

        List<String> openIds = wxTagListUser.getData().getOpenidList();

        tagUserSet = PushData.allUser.stream().map(e -> e[0]).collect(Collectors.toSet());
        openIds.forEach(tagUserSet::remove);

        while (StringUtils.isNotEmpty(wxTagListUser.getNextOpenid())) {
            wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, wxTagListUser.getNextOpenid());

            ConsoleUtil.consoleWithLog("拉取的OPENID个数：" + wxTagListUser.getCount());

            if (wxTagListUser.getCount() == 0) {
                break;
            }
            openIds = wxTagListUser.getData().getOpenidList();

            openIds.forEach(tagUserSet::remove);
        }

        PushData.allUser = Collections.synchronizedList(new ArrayList<>());
        for (String openId : tagUserSet) {
            PushData.allUser.add(new String[]{openId});
        }

        memberCountLabel.setText(String.valueOf(PushData.allUser.size()));
        progressBar.setIndeterminate(false);
        progressBar.setValue(progressBar.getMaximum());

    }

    /**
     * 获取导入数据信息列表
     */
    public static void renderMemberListTable() {
        JTable memberListTable = MemberForm.getInstance().getMemberListTable();
        JProgressBar progressBar = MemberForm.getInstance().getMemberTabImportProgressBar();
        MemberForm memberForm = MemberForm.getInstance();

        toSearchRowsList = null;
        DefaultTableModel tableModel = (DefaultTableModel) memberListTable.getModel();
        tableModel.setRowCount(0);
        progressBar.setVisible(true);
        progressBar.setMaximum(PushData.allUser.size());

        int msgType = App.config.getMsgType();

        // 导入列表
        List<String> headerNameList = Lists.newArrayList();
        headerNameList.add("Data");
        if (MessageTypeEnum.isWxMaOrMpType(msgType)) {
            if (memberForm.getImportOptionAvatarCheckBox().isSelected()) {
                headerNameList.add("头像");
            }
            if (memberForm.getImportOptionBasicInfoCheckBox().isSelected()) {
                headerNameList.add("昵称");
                headerNameList.add("性别");
                headerNameList.add("地区");
                headerNameList.add("关注时间");
            }
            headerNameList.add("openId");
        } else {
            headerNameList.add("数据");
        }

        String[] headerNames = new String[headerNameList.size()];
        headerNameList.toArray(headerNames);
        DefaultTableModel model = new DefaultTableModel(null, headerNames);
        memberListTable.setModel(model);
        if (MessageTypeEnum.isWxMaOrMpType(msgType) && memberForm.getImportOptionAvatarCheckBox().isSelected()) {
            memberListTable.getColumn("头像").setCellRenderer(new TableInCellImageLabelRenderer());
        }

        DefaultTableCellRenderer hr = (DefaultTableCellRenderer) memberListTable.getTableHeader()
                .getDefaultRenderer();
        // 表头列名居左
        hr.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);

        // 隐藏第0列Data数据列
        JTableUtil.hideColumn(memberListTable, 0);

        // 设置行高
        if (MessageTypeEnum.isWxMaOrMpType(msgType) && memberForm.getImportOptionAvatarCheckBox().isSelected()) {
            memberListTable.setRowHeight(66);
        } else {
            memberListTable.setRowHeight(36);
        }

        List<Object> rowDataList;
        WxMpService wxMpService = null;
        boolean needToGetInfoFromWeiXin = false;
        if (MessageTypeEnum.isWxMaOrMpType(msgType) && (memberForm.getImportOptionBasicInfoCheckBox().isSelected() ||
                memberForm.getImportOptionAvatarCheckBox().isSelected())) {
            needToGetInfoFromWeiXin = true;
        }
        if (needToGetInfoFromWeiXin) {
            wxMpService = WxMpTemplateMsgSender.getWxMpService();
        }
        for (int i = 0; i < PushData.allUser.size(); i++) {
            String[] importedData = PushData.allUser.get(i);
            try {
                String openId = importedData[0];
                rowDataList = new ArrayList<>();
                rowDataList.add(String.join("|", importedData));
                if (needToGetInfoFromWeiXin) {
                    WxMpUser wxMpUser = null;
                    TWxMpUser tWxMpUser = tWxMpUserMapper.selectByPrimaryKey(openId);
                    if (tWxMpUser != null) {
                        wxMpUser = new WxMpUser();
                        BeanUtil.copyProperties(tWxMpUser, wxMpUser);
                    } else {
                        if (wxMpService != null) {
                            try {
                                wxMpUser = wxMpService.getUserService().userInfo(openId);
                                if (wxMpUser != null) {
                                    tWxMpUser = new TWxMpUser();
                                    BeanUtil.copyProperties(wxMpUser, tWxMpUser);
                                    tWxMpUserMapper.insertSelective(tWxMpUser);
                                }
                            } catch (Exception e) {
                                logger.error(e);
                            }
                        }
                    }

                    if (wxMpUser != null) {
                        if (memberForm.getImportOptionAvatarCheckBox().isSelected()) {
                            rowDataList.add(wxMpUser.getHeadImgUrl());
                        }
                        if (memberForm.getImportOptionBasicInfoCheckBox().isSelected()) {
                            rowDataList.add(wxMpUser.getNickname());
                            rowDataList.add(wxMpUser.getSexDesc());
                            rowDataList.add(wxMpUser.getCountry() + "-" + wxMpUser.getProvince() + "-" + wxMpUser.getCity());
                            rowDataList.add(DateFormatUtils.format(wxMpUser.getSubscribeTime() * 1000, "yyyy-MM-dd HH:mm:ss"));
                        }
                    } else {
                        if (memberForm.getImportOptionAvatarCheckBox().isSelected()) {
                            rowDataList.add("");
                        }
                        if (memberForm.getImportOptionBasicInfoCheckBox().isSelected()) {
                            rowDataList.add("");
                            rowDataList.add("");
                            rowDataList.add("");
                            rowDataList.add("");
                        }
                    }
                    rowDataList.add(openId);
                } else {
                    rowDataList.add(String.join("|", importedData));
                }

                model.addRow(rowDataList.toArray());
            } catch (Exception e) {
                logger.error(e);
            }
            progressBar.setValue(i + 1);
        }
    }

    /**
     * 通过文件导入
     */
    public static void importFromFile() {
        MemberForm.getInstance().getImportFromFileButton().setEnabled(false);
        JTextField filePathField = MemberForm.getInstance().getMemberFilePathField();
        JPanel memberPanel = MemberForm.getInstance().getMemberPanel();
        JProgressBar progressBar = MemberForm.getInstance().getMemberTabImportProgressBar();
        JLabel memberCountLabel = MemberForm.getInstance().getMemberTabCountLabel();

        if (StringUtils.isBlank(filePathField.getText())) {
            JOptionPane.showMessageDialog(memberPanel, "请填写或点击浏览按钮选择要导入的文件的路径！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            MemberForm.getInstance().getImportFromFileButton().setEnabled(true);
            return;
        }
        File file = new File(filePathField.getText());
        if (!file.exists()) {
            JOptionPane.showMessageDialog(memberPanel, filePathField.getText() + "\n该文件不存在！", "文件不存在",
                    JOptionPane.ERROR_MESSAGE);
            MemberForm.getInstance().getImportFromFileButton().setEnabled(true);
            return;
        }
        CSVReader reader = null;
        FileReader fileReader;

        int currentImported = 0;

        try {
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            String fileNameLowerCase = file.getName().toLowerCase();

            if (fileNameLowerCase.endsWith(".csv")) {
                // 可以解决中文乱码问题
                DataInputStream in = new DataInputStream(new FileInputStream(file));
                reader = new CSVReader(new InputStreamReader(in, FileCharSetUtil.getCharSet(file)));
                String[] nextLine;
                PushData.allUser = Collections.synchronizedList(new ArrayList<>());

                while ((nextLine = reader.readNext()) != null) {
                    PushData.allUser.add(nextLine);
                    currentImported++;
                    memberCountLabel.setText(String.valueOf(currentImported));
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
                        memberCountLabel.setText(String.valueOf(currentImported));
                    }
                }
            } else if (fileNameLowerCase.endsWith(".txt")) {
                fileReader = new FileReader(file, FileCharSetUtil.getCharSetName(file));
                PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                BufferedReader br = fileReader.getReader();
                String line;
                while ((line = br.readLine()) != null) {
                    PushData.allUser.add(line.split(TXT_FILE_DATA_SEPERATOR_REGEX));
                    currentImported++;
                    memberCountLabel.setText(String.valueOf(currentImported));
                }
            } else {
                JOptionPane.showMessageDialog(memberPanel, "不支持该格式的文件！", "文件格式不支持",
                        JOptionPane.ERROR_MESSAGE);
                MemberForm.getInstance().getImportFromFileButton().setEnabled(true);
                return;
            }
            renderMemberListTable();

            if (!PushData.fixRateScheduling) {
                JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
            }

            App.config.setMemberFilePath(filePathField.getText());
            App.config.save();
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + e1.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            logger.error(e1);
            e1.printStackTrace();
        } finally {
            progressBar.setMaximum(100);
            progressBar.setValue(100);
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
            MemberForm.getInstance().getImportFromFileButton().setEnabled(true);
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

    /**
     * 通过SQL导入
     */
    public static void importFromSql() {
        MemberForm memberForm = MemberForm.getInstance();
        memberForm.getImportFromSqlButton().setEnabled(false);
        JPanel memberPanel = memberForm.getMemberPanel();
        JProgressBar progressBar = memberForm.getMemberTabImportProgressBar();
        JLabel memberCountLabel = memberForm.getMemberTabCountLabel();

        if (StringUtils.isBlank(App.config.getMysqlUrl()) || StringUtils.isBlank(App.config.getMysqlUser())) {
            JOptionPane.showMessageDialog(memberPanel, "请先在设置中填写并保存MySQL的配置信息！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            memberForm.getImportFromSqlButton().setEnabled(true);
            return;
        }
        String querySql = memberForm.getImportFromSqlTextArea().getText();
        if (StringUtils.isBlank(querySql)) {
            JOptionPane.showMessageDialog(memberPanel, "请先填写要执行导入的SQL！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            memberForm.getImportFromSqlButton().setEnabled(true);
            return;
        }

        if (StringUtils.isNotEmpty(querySql)) {
            Connection conn = null;
            try {
                memberForm.getImportFromSqlButton().setEnabled(false);
                memberForm.getImportFromSqlButton().updateUI();
                progressBar.setVisible(true);
                progressBar.setIndeterminate(true);

                // 表查询
                PushData.allUser = Collections.synchronizedList(new ArrayList<>());
                int currentImported = 0;

                conn = HikariUtil.getConnection();
                List<Entity> entityList = SqlExecutor.query(conn, querySql, new EntityListHandler());
                for (Entity entity : entityList) {
                    Set<String> fieldNames = entity.getFieldNames();
                    String[] msgData = new String[fieldNames.size()];
                    int i = 0;
                    for (String fieldName : fieldNames) {
                        msgData[i] = entity.getStr(fieldName);
                        i++;
                    }
                    PushData.allUser.add(msgData);
                    currentImported++;
                    memberCountLabel.setText(String.valueOf(currentImported));
                }

                renderMemberListTable();
                if (!PushData.fixRateScheduling) {
                    JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
                }

                App.config.setMemberSql(querySql);
                App.config.save();
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            } finally {
                DbUtil.close(conn);
                memberForm.getImportFromSqlButton().setEnabled(true);
                memberForm.getImportFromSqlButton().updateUI();
                progressBar.setMaximum(100);
                progressBar.setValue(100);
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                memberForm.getImportFromSqlButton().setEnabled(true);
            }
        }
    }

    /**
     * 导入微信全员
     */
    public static void importWxAll() {
        JPanel memberPanel = MemberForm.getInstance().getMemberPanel();
        JProgressBar progressBar = MemberForm.getInstance().getMemberTabImportProgressBar();
        MemberForm.getInstance().getMemberImportAllButton().setEnabled(false);

        try {
            getMpUserList();
            renderMemberListTable();
            if (!PushData.fixRateScheduling) {
                JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (WxErrorException e1) {
            JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + e1.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            logger.error(e1);
            e1.printStackTrace();
        } finally {
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
            MemberForm.getInstance().getMemberImportAllButton().setEnabled(true);
        }
    }

    /**
     * 导入企业通讯录全员
     */
    public static void importWxCpAll() {
        JProgressBar progressBar = MemberForm.getInstance().getMemberTabImportProgressBar();
        JLabel memberCountLabel = MemberForm.getInstance().getMemberTabCountLabel();
        JPanel memberPanel = MemberForm.getInstance().getMemberPanel();

        try {
            if (WxCpMsgForm.getInstance().getAppNameComboBox().getSelectedItem() == null) {
                JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "请先在编辑消息tab中选择应用！", "提示",
                        JOptionPane.ERROR_MESSAGE);
                MainWindow.getInstance().getTabbedPane().setSelectedIndex(2);
                return;
            }

            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            int importedCount = 0;
            PushData.allUser = Collections.synchronizedList(new ArrayList<>());

            // 获取最小部门id
            List<WxCpDepart> wxCpDepartList = WxCpMsgSender.getWxCpService().getDepartmentService().list(null);
            long minDeptId = Long.MAX_VALUE;
            for (WxCpDepart wxCpDepart : wxCpDepartList) {
                if (wxCpDepart.getId() < minDeptId) {
                    minDeptId = wxCpDepart.getId();
                }
            }
            // 获取用户
            List<WxCpUser> wxCpUsers = WxCpMsgSender.getWxCpService().getUserService().listByDepartment(minDeptId, true, 0);
            for (WxCpUser wxCpUser : wxCpUsers) {
                String statusStr = "";
                if (wxCpUser.getStatus() == 1) {
                    statusStr = "已关注";
                } else if (wxCpUser.getStatus() == 2) {
                    statusStr = "已冻结";
                } else if (wxCpUser.getStatus() == 4) {
                    statusStr = "未关注";
                }
                Long[] depIds = wxCpUser.getDepartIds();
                List<String> deptNameList = Lists.newArrayList();
                if (depIds != null) {
                    for (Long depId : depIds) {
                        deptNameList.add(wxCpIdToDeptNameMap.get(depId));
                    }
                }
                String[] dataArray = new String[]{wxCpUser.getUserId(), wxCpUser.getName(), wxCpUser.getGender().getGenderName(), wxCpUser.getEmail(), String.join("/", deptNameList), wxCpUser.getPosition(), statusStr};
                PushData.allUser.add(dataArray);
                importedCount++;
                memberCountLabel.setText(String.valueOf(importedCount));
            }
            renderMemberListTable();
            if (!PushData.fixRateScheduling) {
                JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + ex, "失败",
                    JOptionPane.ERROR_MESSAGE);
            logger.error(ex.toString());
        } finally {
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
        }
    }

    /**
     * 导入钉钉通讯录全员
     */
    public static void importDingAll() {
        JProgressBar progressBar = MemberForm.getInstance().getMemberTabImportProgressBar();
        JLabel memberCountLabel = MemberForm.getInstance().getMemberTabCountLabel();
        JPanel memberPanel = MemberForm.getInstance().getMemberPanel();

        try {
            if (DingMsgForm.getInstance().getAppNameComboBox().getSelectedItem() == null) {
                JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "请先在编辑消息tab中选择应用！", "提示",
                        JOptionPane.ERROR_MESSAGE);
                MainWindow.getInstance().getTabbedPane().setSelectedIndex(2);
                return;
            }

            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            int importedCount = 0;
            PushData.allUser = Collections.synchronizedList(new ArrayList<>());

            // 最小部门id为1
            // 获取用户
            DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/simplelist");
            OapiUserSimplelistRequest request = new OapiUserSimplelistRequest();
            request.setDepartmentId(1L);
            request.setOffset(0L);
            request.setSize(100L);
            request.setHttpMethod("GET");

            long offset = 0;
            OapiUserSimplelistResponse response = new OapiUserSimplelistResponse();
            while (response.getErrcode() == null || response.getUserlist().size() > 0) {
                response = client.execute(request, DingMsgSender.getAccessTokenTimedCache().get("accessToken"));
                if (response.getErrcode() != 0) {
                    if (response.getErrcode() == 60011) {
                        JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + response.getErrmsg() + "\n\n进入开发者后台，在小程序或者微应用详情的「接口权限」模块，点击申请对应的通讯录接口读写权限", "失败",
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + response.getErrmsg(), "失败", JOptionPane.ERROR_MESSAGE);
                    }

                    logger.error(response.getErrmsg());
                    return;
                }
                List<OapiUserSimplelistResponse.Userlist> userlist = response.getUserlist();
                for (OapiUserSimplelistResponse.Userlist dingUser : userlist) {
                    String[] dataArray = new String[]{dingUser.getUserid(), dingUser.getName()};
                    PushData.allUser.add(dataArray);
                    importedCount++;
                    memberCountLabel.setText(String.valueOf(importedCount));
                }
                offset += 100;
                request.setOffset(offset);
            }

            renderMemberListTable();
            if (!PushData.fixRateScheduling) {
                JOptionPane.showMessageDialog(memberPanel, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(memberPanel, "导入失败！\n\n" + ex, "失败",
                    JOptionPane.ERROR_MESSAGE);
            logger.error(ex.toString());
        } finally {
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
        }
    }
}
