package com.fangxuele.tool.push.ui.dialog.importway;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiDepartmentListRequest;
import com.dingtalk.api.request.OapiUserSimplelistRequest;
import com.dingtalk.api.response.OapiDepartmentListResponse;
import com.dingtalk.api.response.OapiUserSimplelistResponse;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.dao.TPeopleImportConfigMapper;
import com.fangxuele.tool.push.dao.TPeopleMapper;
import com.fangxuele.tool.push.domain.TPeople;
import com.fangxuele.tool.push.domain.TPeopleData;
import com.fangxuele.tool.push.domain.TPeopleImportConfig;
import com.fangxuele.tool.push.logic.PeopleImportWayEnum;
import com.fangxuele.tool.push.logic.msgsender.DingMsgSender;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.importway.config.DingImportConfig;
import com.fangxuele.tool.push.ui.form.PeopleEditForm;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.common.collect.Maps;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImportByDing extends JDialog {
    private JPanel contentPane;
    private JComboBox dingDeptsComboBox;
    private JButton dingImportAllButton;
    private JButton dingDeptsRefreshButton;
    private JButton dingDeptsImportButton;

    private static final Log logger = LogFactory.get();

    private Integer peopleId;

    private TPeople tPeople;

    private static TPeopleDataMapper peopleDataMapper = MybatisUtil.getSqlSession().getMapper(TPeopleDataMapper.class);
    private static TPeopleMapper peopleMapper = MybatisUtil.getSqlSession().getMapper(TPeopleMapper.class);
    private static TPeopleImportConfigMapper peopleImportConfigMapper = MybatisUtil.getSqlSession().getMapper(TPeopleImportConfigMapper.class);

    /**
     * 企业号部门名称->部门ID
     */
    private static Map<String, Long> wxCpDeptNameToIdMap = Maps.newHashMap();

    /**
     * 企业号部门ID>部门名称
     */
    private static Map<Long, String> wxCpIdToDeptNameMap = Maps.newHashMap();

    public ImportByDing(Integer peopleId) {
        super(App.mainFrame, "通过钉钉通讯录导入");
        setContentPane(contentPane);
        setModal(true);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.3, 0.2);
        getRootPane().setDefaultButton(dingImportAllButton);

        this.peopleId = peopleId;
        tPeople = peopleMapper.selectByPrimaryKey(peopleId);

        dingDeptsImportButton.setIcon(new FlatSVGIcon("icon/import.svg"));
        dingImportAllButton.setIcon(new FlatSVGIcon("icon/import.svg"));
        dingDeptsRefreshButton.setIcon(new FlatSVGIcon("icon/refresh.svg"));

        // 钉钉-按部门导入-刷新
        dingDeptsRefreshButton.addActionListener(e -> {
            ThreadUtil.execute(() -> {
                dingDeptsComboBox.removeAllItems();

                try {
                    // 获取部门列表
                    DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/department/list");
                    OapiDepartmentListRequest request = new OapiDepartmentListRequest();
                    request.setHttpMethod("GET");
                    OapiDepartmentListResponse response = client.execute(request, DingMsgSender.getAccessTokenTimedCache(tPeople.getAccountId()).get("accessToken"));
                    if (response.getErrcode() != 0) {
                        JOptionPane.showMessageDialog(App.mainFrame, "刷新失败！\n\n" + response.getErrmsg(), "失败",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    List<OapiDepartmentListResponse.Department> departmentList = response.getDepartment();
                    for (OapiDepartmentListResponse.Department department : departmentList) {
                        dingDeptsComboBox.addItem(department.getName());
                        wxCpDeptNameToIdMap.put(department.getName(), department.getId());
                        wxCpIdToDeptNameMap.put(department.getId(), department.getName());
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(App.mainFrame, "刷新失败！\n\n" + ex, "失败",
                            JOptionPane.ERROR_MESSAGE);
                    logger.error(ExceptionUtils.getStackTrace(ex));
                }
            });
        });

        // 钉钉-按部门导入-导入
        dingDeptsImportButton.addActionListener(e -> {
            ThreadUtil.execute(() -> {
                PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();
                JProgressBar progressBar = peopleEditForm.getMemberTabImportProgressBar();
                JLabel memberCountLabel = peopleEditForm.getMemberTabCountLabel();

                if (dingDeptsComboBox.getSelectedItem() == null) {
                    return;
                }
                try {
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                    int importedCount = 0;
                    String now = SqliteUtil.nowDateForSqlite();

                    String dataVersion = UUID.fastUUID().toString(true);

                    // 获取部门id
                    Long deptId = wxCpDeptNameToIdMap.get(dingDeptsComboBox.getSelectedItem());

                    // 保存导入配置
                    TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);

                    TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
                    tPeopleImportConfig.setPeopleId(peopleId);
                    tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_DING_CODE));
                    tPeopleImportConfig.setLastDataVersion(dataVersion);
                    tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
                    tPeopleImportConfig.setModifiedTime(now);

                    DingImportConfig dingImportConfig = new DingImportConfig();
                    dingImportConfig.setUserType(2);
                    dingImportConfig.setDeptId(deptId);
                    tPeopleImportConfig.setLastWayConfig(JSONUtil.toJsonStr(dingImportConfig));

                    if (beforePeopleImportConfig != null) {
                        tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                        peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
                    } else {
                        tPeopleImportConfig.setCreateTime(now);
                        peopleImportConfigMapper.insert(tPeopleImportConfig);
                    }

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
                        response = client.execute(request, DingMsgSender.getAccessTokenTimedCache(tPeople.getAccountId()).get("accessToken"));
                        if (response.getErrcode() != 0) {
                            if (response.getErrcode() == 60011) {
                                JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + response.getErrmsg() + "\n\n进入开发者后台，在小程序或者微应用详情的「接口权限」模块，点击申请对应的通讯录接口读写权限", "失败",
                                        JOptionPane.ERROR_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + response.getErrmsg(), "失败", JOptionPane.ERROR_MESSAGE);
                            }

                            logger.error(response.getErrmsg());
                            return;
                        }
                        List<OapiUserSimplelistResponse.Userlist> userlist = response.getUserlist();
                        for (OapiUserSimplelistResponse.Userlist dingUser : userlist) {
                            String[] dataArray = new String[]{dingUser.getUserid(), dingUser.getName()};

                            TPeopleData tPeopleData = new TPeopleData();
                            tPeopleData.setPeopleId(peopleId);
                            tPeopleData.setPin(dataArray[0]);
                            tPeopleData.setVarData(JSONUtil.toJsonStr(dataArray));
                            tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                            tPeopleData.setDataVersion(dataVersion);
                            tPeopleData.setCreateTime(now);
                            tPeopleData.setModifiedTime(now);

                            peopleDataMapper.insert(tPeopleData);

                            importedCount++;
                            memberCountLabel.setText(String.valueOf(importedCount));
                        }
                        offset += 100;
                        request.setOffset(offset);
                    }

                    PeopleEditForm.initDataTable(peopleId);

                    JOptionPane.showMessageDialog(App.mainFrame, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
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

        // 钉钉-导入全部
        dingImportAllButton.addActionListener(e -> onOK());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        ThreadUtil.execute(() -> {
            importDingAll();
        });
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }


    /**
     * 导入钉钉通讯录全员
     */
    public void importDingAll() {
        PeopleEditForm instance = PeopleEditForm.getInstance();
        JProgressBar progressBar = instance.getMemberTabImportProgressBar();
        JLabel memberCountLabel = instance.getMemberTabCountLabel();

        try {
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            int importedCount = 0;

            String now = SqliteUtil.nowDateForSqlite();
            String dataVersion = UUID.fastUUID().toString(true);

            // 保存导入配置
            TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);

            TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
            tPeopleImportConfig.setPeopleId(peopleId);
            tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_DING_CODE));
            tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
            tPeopleImportConfig.setLastDataVersion(dataVersion);
            tPeopleImportConfig.setModifiedTime(now);

            DingImportConfig dingImportConfig = new DingImportConfig();
            dingImportConfig.setUserType(1);
            tPeopleImportConfig.setLastWayConfig(JSONUtil.toJsonStr(dingImportConfig));

            if (beforePeopleImportConfig != null) {
                tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
            } else {
                tPeopleImportConfig.setCreateTime(now);
                peopleImportConfigMapper.insert(tPeopleImportConfig);
            }

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
                response = client.execute(request, DingMsgSender.getAccessTokenTimedCache(tPeople.getAccountId()).get("accessToken"));
                if (response.getErrcode() != 0) {
                    if (response.getErrcode() == 60011) {
                        JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + response.getErrmsg() + "\n\n进入开发者后台，在小程序或者微应用详情的「接口权限」模块，点击申请对应的通讯录接口读写权限", "失败",
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + response.getErrmsg(), "失败", JOptionPane.ERROR_MESSAGE);
                    }

                    logger.error(response.getErrmsg());
                    return;
                }
                List<OapiUserSimplelistResponse.Userlist> userlist = response.getUserlist();
                for (OapiUserSimplelistResponse.Userlist dingUser : userlist) {
                    String[] dataArray = new String[]{dingUser.getUserid(), dingUser.getName()};

                    TPeopleData tPeopleData = new TPeopleData();
                    tPeopleData.setPeopleId(peopleId);
                    tPeopleData.setPin(dataArray[0]);
                    tPeopleData.setVarData(JSONUtil.toJsonStr(dataArray));
                    tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                    tPeopleData.setDataVersion(dataVersion);
                    tPeopleData.setCreateTime(now);
                    tPeopleData.setModifiedTime(now);

                    peopleDataMapper.insert(tPeopleData);

                    importedCount++;
                    memberCountLabel.setText(String.valueOf(importedCount));
                }
                offset += 100;
                request.setOffset(offset);
            }

            PeopleEditForm.initDataTable(peopleId);

            JOptionPane.showMessageDialog(App.mainFrame, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + ex, "失败",
                    JOptionPane.ERROR_MESSAGE);
            logger.error(ExceptionUtils.getStackTrace(ex));
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
        dingImportAllButton = new JButton();
        Font dingImportAllButtonFont = this.$$$getFont$$$(null, Font.PLAIN, -1, dingImportAllButton.getFont());
        if (dingImportAllButtonFont != null) dingImportAllButton.setFont(dingImportAllButtonFont);
        dingImportAllButton.setText("导入通讯录中所有用户");
        panel2.add(dingImportAllButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dingDeptsComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        dingDeptsComboBox.setModel(defaultComboBoxModel1);
        panel3.add(dingDeptsComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dingDeptsRefreshButton = new JButton();
        Font dingDeptsRefreshButtonFont = this.$$$getFont$$$(null, Font.PLAIN, -1, dingDeptsRefreshButton.getFont());
        if (dingDeptsRefreshButtonFont != null) dingDeptsRefreshButton.setFont(dingDeptsRefreshButtonFont);
        dingDeptsRefreshButton.setText("刷新");
        panel3.add(dingDeptsRefreshButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("按部门导入");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dingDeptsImportButton = new JButton();
        dingDeptsImportButton.setText("导入");
        panel3.add(dingDeptsImportButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

    public void reImport() {
        TPeopleImportConfig peopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);
        String lastWayConfig = peopleImportConfig.getLastWayConfig();
        DingImportConfig dingImportConfigBefore = JSONUtil.toBean(lastWayConfig, DingImportConfig.class);
        if (dingImportConfigBefore == null) {
            return;
        }

        try {
            if (dingImportConfigBefore.getUserType() == 1) {
                int importedCount = 0;

                String now = SqliteUtil.nowDateForSqlite();
                String dataVersion = UUID.fastUUID().toString(true);

                // 保存导入配置
                TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);

                TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
                tPeopleImportConfig.setPeopleId(peopleId);
                tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_DING_CODE));
                tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
                tPeopleImportConfig.setLastDataVersion(dataVersion);
                tPeopleImportConfig.setModifiedTime(now);

                DingImportConfig dingImportConfig = new DingImportConfig();
                dingImportConfig.setUserType(1);
                tPeopleImportConfig.setLastWayConfig(JSONUtil.toJsonStr(dingImportConfig));

                if (beforePeopleImportConfig != null) {
                    tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                    peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
                } else {
                    tPeopleImportConfig.setCreateTime(now);
                    peopleImportConfigMapper.insert(tPeopleImportConfig);
                }

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

                peopleDataMapper.deleteByPeopleId(peopleId);

                while (response.getErrcode() == null || response.getUserlist().size() > 0) {
                    response = client.execute(request, DingMsgSender.getAccessTokenTimedCache(tPeople.getAccountId()).get("accessToken"));
                    if (response.getErrcode() != 0) {
                        if (response.getErrcode() == 60011) {
                            JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + response.getErrmsg() + "\n\n进入开发者后台，在小程序或者微应用详情的「接口权限」模块，点击申请对应的通讯录接口读写权限", "失败",
                                    JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + response.getErrmsg(), "失败", JOptionPane.ERROR_MESSAGE);
                        }

                        logger.error(response.getErrmsg());
                        return;
                    }
                    List<OapiUserSimplelistResponse.Userlist> userlist = response.getUserlist();
                    for (OapiUserSimplelistResponse.Userlist dingUser : userlist) {
                        String[] dataArray = new String[]{dingUser.getUserid(), dingUser.getName()};

                        TPeopleData tPeopleData = new TPeopleData();
                        tPeopleData.setPeopleId(peopleId);
                        tPeopleData.setPin(dataArray[0]);
                        tPeopleData.setVarData(JSONUtil.toJsonStr(dataArray));
                        tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                        tPeopleData.setDataVersion(dataVersion);
                        tPeopleData.setCreateTime(now);
                        tPeopleData.setModifiedTime(now);

                        peopleDataMapper.insert(tPeopleData);

                        importedCount++;
                    }
                    offset += 100;
                    request.setOffset(offset);
                }
            } else if (dingImportConfigBefore.getUserType() == 2) {
                int importedCount = 0;
                String now = SqliteUtil.nowDateForSqlite();

                String dataVersion = UUID.fastUUID().toString(true);

                // 获取部门id
                Long deptId = dingImportConfigBefore.getDeptId();

                // 保存导入配置
                TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);

                TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
                tPeopleImportConfig.setPeopleId(peopleId);
                tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_DING_CODE));
                tPeopleImportConfig.setLastDataVersion(dataVersion);
                tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
                tPeopleImportConfig.setModifiedTime(now);

                DingImportConfig dingImportConfig = new DingImportConfig();
                dingImportConfig.setUserType(2);
                dingImportConfig.setDeptId(deptId);
                tPeopleImportConfig.setLastWayConfig(JSONUtil.toJsonStr(dingImportConfig));

                if (beforePeopleImportConfig != null) {
                    tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                    peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
                } else {
                    tPeopleImportConfig.setCreateTime(now);
                    peopleImportConfigMapper.insert(tPeopleImportConfig);
                }

                // 获取用户
                DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/simplelist");
                OapiUserSimplelistRequest request = new OapiUserSimplelistRequest();
                request.setDepartmentId(deptId);
                request.setOffset(0L);
                request.setSize(100L);
                request.setHttpMethod("GET");

                long offset = 0;
                OapiUserSimplelistResponse response = new OapiUserSimplelistResponse();

                peopleDataMapper.deleteByPeopleId(peopleId);

                while (response.getErrcode() == null || response.getUserlist().size() > 0) {
                    response = client.execute(request, DingMsgSender.getAccessTokenTimedCache(tPeople.getAccountId()).get("accessToken"));
                    if (response.getErrcode() != 0) {
                        if (response.getErrcode() == 60011) {
                            JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + response.getErrmsg() + "\n\n进入开发者后台，在小程序或者微应用详情的「接口权限」模块，点击申请对应的通讯录接口读写权限", "失败",
                                    JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + response.getErrmsg(), "失败", JOptionPane.ERROR_MESSAGE);
                        }

                        logger.error(response.getErrmsg());
                        return;
                    }
                    List<OapiUserSimplelistResponse.Userlist> userlist = response.getUserlist();
                    for (OapiUserSimplelistResponse.Userlist dingUser : userlist) {
                        String[] dataArray = new String[]{dingUser.getUserid(), dingUser.getName()};

                        TPeopleData tPeopleData = new TPeopleData();
                        tPeopleData.setPeopleId(peopleId);
                        tPeopleData.setPin(dataArray[0]);
                        tPeopleData.setVarData(JSONUtil.toJsonStr(dataArray));
                        tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                        tPeopleData.setDataVersion(dataVersion);
                        tPeopleData.setCreateTime(now);
                        tPeopleData.setModifiedTime(now);

                        peopleDataMapper.insert(tPeopleData);

                        importedCount++;
                    }
                    offset += 100;
                    request.setOffset(offset);
                }
            }
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }
}
