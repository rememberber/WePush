package com.fangxuele.tool.push.ui.dialog.importway;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.dao.TPeopleImportConfigMapper;
import com.fangxuele.tool.push.domain.TPeopleData;
import com.fangxuele.tool.push.domain.TPeopleImportConfig;
import com.fangxuele.tool.push.logic.PeopleImportWayEnum;
import com.fangxuele.tool.push.logic.PushData;
import com.fangxuele.tool.push.logic.msgsender.WxCpMsgSender;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.form.MainWindow;
import com.fangxuele.tool.push.ui.form.PeopleEditForm;
import com.fangxuele.tool.push.ui.form.msg.WxCpMsgForm;
import com.fangxuele.tool.push.ui.listener.PeopleManageListener;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.google.common.collect.Maps;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import me.chanjar.weixin.cp.bean.WxCpDepart;
import me.chanjar.weixin.cp.bean.WxCpTag;
import me.chanjar.weixin.cp.bean.WxCpUser;
import org.apache.commons.compress.utils.Lists;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImportByWxCp extends JDialog {
    private JPanel contentPane;
    private JComboBox wxCpTagsComboBox;
    private JComboBox wxCpDeptsComboBox;
    private JButton wxCpImportAllButton;
    private JButton wxCpTagsRefreshButton;
    private JButton wxCpDeptsRefreshButton;
    private JButton wxCpTagsImportButton;
    private JButton wxCpDeptsImportButton;

    private static final Log logger = LogFactory.get();

    private static TPeopleDataMapper peopleDataMapper = MybatisUtil.getSqlSession().getMapper(TPeopleDataMapper.class);
    private static TPeopleImportConfigMapper peopleImportConfigMapper = MybatisUtil.getSqlSession().getMapper(TPeopleImportConfigMapper.class);

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

    public ImportByWxCp() {
        super(App.mainFrame, "通过微信企业通讯录导入");
        setContentPane(contentPane);
        setModal(true);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.3, 0.2);
        getRootPane().setDefaultButton(wxCpImportAllButton);

        // 企业号-按标签导入-刷新
        wxCpTagsRefreshButton.addActionListener(e -> {
            ThreadUtil.execute(() -> {
                if (WxCpMsgForm.getInstance().getAppNameComboBox().getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "请先在编辑消息tab中选择应用！", "提示",
                            JOptionPane.ERROR_MESSAGE);
                    MainWindow.getInstance().getTabbedPane().setSelectedIndex(2);
                    return;
                }
                wxCpTagsComboBox.removeAllItems();

                try {
                    // 获取标签列表
                    List<WxCpTag> wxCpTagList = WxCpMsgSender.getWxCpService().getTagService().listAll();
                    for (WxCpTag wxCpTag : wxCpTagList) {
                        wxCpTagsComboBox.addItem(wxCpTag.getName());
                        wxCpTagNameToIdMap.put(wxCpTag.getName(), wxCpTag.getId());
                        wxCpIdToTagNameMap.put(wxCpTag.getId(), wxCpTag.getName());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(App.mainFrame, "刷新失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ex.toString());
                }
            });
        });

        // 企业号-按标签导入-导入
        wxCpTagsImportButton.addActionListener(e -> {
            ThreadUtil.execute(() -> {
                PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();
                JProgressBar progressBar = peopleEditForm.getMemberTabImportProgressBar();
                JLabel memberCountLabel = peopleEditForm.getMemberTabCountLabel();

                if (wxCpTagsComboBox.getSelectedItem() == null) {
                    return;
                }
                try {
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                    int importedCount = 0;
                    String now = SqliteUtil.nowDateForSqlite();

                    // 保存导入配置
                    TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(PeopleManageListener.selectedPeopleId);

                    TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
                    tPeopleImportConfig.setPeopleId(PeopleManageListener.selectedPeopleId);
                    tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_DING_CODE));
                    tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
                    tPeopleImportConfig.setModifiedTime(now);

                    if (beforePeopleImportConfig != null) {
                        tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                        peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
                    } else {
                        tPeopleImportConfig.setCreateTime(now);
                        peopleImportConfigMapper.insert(tPeopleImportConfig);
                    }

                    // 获取标签id
                    String tagId = wxCpTagNameToIdMap.get(wxCpTagsComboBox.getSelectedItem());
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

                        TPeopleData tPeopleData = new TPeopleData();
                        tPeopleData.setPeopleId(PeopleManageListener.selectedPeopleId);
                        tPeopleData.setPin(dataArray[0]);
                        tPeopleData.setVarData(JSONUtil.toJsonStr(dataArray));
                        tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                        tPeopleData.setCreateTime(now);
                        tPeopleData.setModifiedTime(now);

                        peopleDataMapper.insert(tPeopleData);

                        importedCount++;
                        memberCountLabel.setText(String.valueOf(importedCount));
                    }
                    PeopleEditForm.initDataTable(PeopleManageListener.selectedPeopleId);
                    if (!PushData.fixRateScheduling) {
                        JOptionPane.showMessageDialog(App.mainFrame, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ex.toString());
                } finally {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                }

            });
        });

        // 企业号-按部门导入-刷新
        wxCpDeptsRefreshButton.addActionListener(e -> {
            ThreadUtil.execute(() -> {
                if (WxCpMsgForm.getInstance().getAppNameComboBox().getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(MainWindow.getInstance().getMessagePanel(), "请先在编辑消息tab中选择应用！", "提示",
                            JOptionPane.ERROR_MESSAGE);
                    MainWindow.getInstance().getTabbedPane().setSelectedIndex(2);
                    return;
                }
                wxCpDeptsComboBox.removeAllItems();

                try {
                    // 获取部门列表
                    List<WxCpDepart> wxCpDepartList = WxCpMsgSender.getWxCpService().getDepartmentService().list(null);
                    for (WxCpDepart wxCpDepart : wxCpDepartList) {
                        wxCpDeptsComboBox.addItem(wxCpDepart.getName());
                        wxCpDeptNameToIdMap.put(wxCpDepart.getName(), wxCpDepart.getId());
                        wxCpIdToDeptNameMap.put(wxCpDepart.getId(), wxCpDepart.getName());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(App.mainFrame, "刷新失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ex.toString());
                }
            });
        });

        // 企业号-按部门导入-导入
        wxCpDeptsImportButton.addActionListener(e -> {
            ThreadUtil.execute(() -> {
                PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();
                JProgressBar progressBar = peopleEditForm.getMemberTabImportProgressBar();
                JLabel memberCountLabel = peopleEditForm.getMemberTabCountLabel();

                if (wxCpDeptsComboBox.getSelectedItem() == null) {
                    return;
                }
                try {
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                    int importedCount = 0;
                    String now = SqliteUtil.nowDateForSqlite();

                    // 保存导入配置
                    TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(PeopleManageListener.selectedPeopleId);

                    TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
                    tPeopleImportConfig.setPeopleId(PeopleManageListener.selectedPeopleId);
                    tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_DING_CODE));
                    tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
                    tPeopleImportConfig.setModifiedTime(now);

                    if (beforePeopleImportConfig != null) {
                        tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                        peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
                    } else {
                        tPeopleImportConfig.setCreateTime(now);
                        peopleImportConfigMapper.insert(tPeopleImportConfig);
                    }

                    // 获取部门id
                    Long deptId = wxCpDeptNameToIdMap.get(wxCpDeptsComboBox.getSelectedItem());
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

                        TPeopleData tPeopleData = new TPeopleData();
                        tPeopleData.setPeopleId(PeopleManageListener.selectedPeopleId);
                        tPeopleData.setPin(dataArray[0]);
                        tPeopleData.setVarData(JSONUtil.toJsonStr(dataArray));
                        tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                        tPeopleData.setCreateTime(now);
                        tPeopleData.setModifiedTime(now);

                        peopleDataMapper.insert(tPeopleData);

                        importedCount++;
                        memberCountLabel.setText(String.valueOf(importedCount));
                    }
                    PeopleEditForm.initDataTable(PeopleManageListener.selectedPeopleId);

                    if (!PushData.fixRateScheduling) {
                        JOptionPane.showMessageDialog(App.mainFrame, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ex.toString());
                } finally {
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                }

            });
        });

        // 企业号-导入全部
        wxCpImportAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        ThreadUtil.execute(() -> {
            importWxCpAll();
        });

        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    /**
     * 导入企业通讯录全员
     */
    public static void importWxCpAll() {
        PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();
        JProgressBar progressBar = peopleEditForm.getMemberTabImportProgressBar();
        JLabel memberCountLabel = peopleEditForm.getMemberTabCountLabel();

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
            String now = SqliteUtil.nowDateForSqlite();

            // 保存导入配置
            TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(PeopleManageListener.selectedPeopleId);

            TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
            tPeopleImportConfig.setPeopleId(PeopleManageListener.selectedPeopleId);
            tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_DING_CODE));
            tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
            tPeopleImportConfig.setModifiedTime(now);

            if (beforePeopleImportConfig != null) {
                tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
            } else {
                tPeopleImportConfig.setCreateTime(now);
                peopleImportConfigMapper.insert(tPeopleImportConfig);
            }

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

                TPeopleData tPeopleData = new TPeopleData();
                tPeopleData.setPeopleId(PeopleManageListener.selectedPeopleId);
                tPeopleData.setPin(dataArray[0]);
                tPeopleData.setVarData(JSONUtil.toJsonStr(dataArray));
                tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                tPeopleData.setCreateTime(now);
                tPeopleData.setModifiedTime(now);

                peopleDataMapper.insert(tPeopleData);

                importedCount++;
                memberCountLabel.setText(String.valueOf(importedCount));
            }

            PeopleEditForm.initDataTable(PeopleManageListener.selectedPeopleId);

            if (!PushData.fixRateScheduling) {
                JOptionPane.showMessageDialog(App.mainFrame, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + ex, "失败",
                    JOptionPane.ERROR_MESSAGE);
            logger.error(ex.toString());
        } finally {
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
        }
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        wxCpImportAllButton = new JButton();
        Font wxCpImportAllButtonFont = this.$$$getFont$$$(null, Font.PLAIN, -1, wxCpImportAllButton.getFont());
        if (wxCpImportAllButtonFont != null) wxCpImportAllButton.setFont(wxCpImportAllButtonFont);
        wxCpImportAllButton.setIcon(new ImageIcon(getClass().getResource("/icon/import_dark.png")));
        wxCpImportAllButton.setText("导入通讯录中所有用户");
        panel2.add(wxCpImportAllButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        wxCpTagsComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        wxCpTagsComboBox.setModel(defaultComboBoxModel1);
        panel3.add(wxCpTagsComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        wxCpDeptsComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        wxCpDeptsComboBox.setModel(defaultComboBoxModel2);
        panel3.add(wxCpDeptsComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        wxCpTagsRefreshButton = new JButton();
        Font wxCpTagsRefreshButtonFont = this.$$$getFont$$$(null, Font.PLAIN, -1, wxCpTagsRefreshButton.getFont());
        if (wxCpTagsRefreshButtonFont != null) wxCpTagsRefreshButton.setFont(wxCpTagsRefreshButtonFont);
        wxCpTagsRefreshButton.setIcon(new ImageIcon(getClass().getResource("/icon/refresh.png")));
        wxCpTagsRefreshButton.setText("刷新");
        panel3.add(wxCpTagsRefreshButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wxCpDeptsRefreshButton = new JButton();
        Font wxCpDeptsRefreshButtonFont = this.$$$getFont$$$(null, Font.PLAIN, -1, wxCpDeptsRefreshButton.getFont());
        if (wxCpDeptsRefreshButtonFont != null) wxCpDeptsRefreshButton.setFont(wxCpDeptsRefreshButtonFont);
        wxCpDeptsRefreshButton.setIcon(new ImageIcon(getClass().getResource("/icon/refresh.png")));
        wxCpDeptsRefreshButton.setText("刷新");
        panel3.add(wxCpDeptsRefreshButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("按标签导入");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("按部门导入");
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wxCpTagsImportButton = new JButton();
        wxCpTagsImportButton.setIcon(new ImageIcon(getClass().getResource("/icon/import_dark.png")));
        wxCpTagsImportButton.setText("导入");
        panel3.add(wxCpTagsImportButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wxCpDeptsImportButton = new JButton();
        wxCpDeptsImportButton.setIcon(new ImageIcon(getClass().getResource("/icon/import_dark.png")));
        wxCpDeptsImportButton.setText("导入");
        panel3.add(wxCpDeptsImportButton, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        return contentPane;
    }

}
