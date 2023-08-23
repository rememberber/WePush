package com.fangxuele.tool.push.ui.dialog.importway;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.db.DbUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.dao.TPeopleImportConfigMapper;
import com.fangxuele.tool.push.domain.TPeopleData;
import com.fangxuele.tool.push.domain.TPeopleImportConfig;
import com.fangxuele.tool.push.logic.PeopleImportWayEnum;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.form.PeopleEditForm;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.HikariUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.util.List;
import java.util.Set;

public class ImportBySQL extends JDialog {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JButton importFromSqlButton;
    private JTextArea importFromSqlTextArea;

    private static final Log logger = LogFactory.get();

    private static TPeopleDataMapper peopleDataMapper = MybatisUtil.getSqlSession().getMapper(TPeopleDataMapper.class);
    private static TPeopleImportConfigMapper peopleImportConfigMapper = MybatisUtil.getSqlSession().getMapper(TPeopleImportConfigMapper.class);

    private Integer peopleId;

    public ImportBySQL(Integer peopleId) {
        super(App.mainFrame, "通过SQL导入人群");
        setContentPane(contentPane);
        setModal(true);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.4, 0.4);
        getRootPane().setDefaultButton(importFromSqlButton);

        this.peopleId = peopleId;

        importFromSqlButton.setIcon(new FlatSVGIcon("icon/import.svg"));

        // 获取上一次导入的配置
        TPeopleImportConfig tPeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);
        if (tPeopleImportConfig != null) {
            importFromSqlTextArea.setText(tPeopleImportConfig.getLastSql());
        }

        importFromSqlButton.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

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
        // add your code here
        String sql = importFromSqlTextArea.getText();

        PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();

        if (StringUtils.isBlank(App.config.getMysqlUrl()) || StringUtils.isBlank(App.config.getMysqlUser())) {
            JOptionPane.showMessageDialog(App.mainFrame, "请先在设置中填写并保存MySQL的配置信息！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            peopleEditForm.getImportButton().setEnabled(true);
            return;
        }
        if (StringUtils.isBlank(sql)) {
            JOptionPane.showMessageDialog(App.mainFrame, "请先填写要执行导入的SQL！", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            peopleEditForm.getImportButton().setEnabled(true);
            return;
        }

        ThreadUtil.execute(() -> importFromSql(sql, false, false));

        dispose();
    }

    private void onCancel() {
        dispose();
    }

    /**
     * 通过SQL导入
     */
    public void importFromSql(String querySql, Boolean clear, Boolean silence) {
        PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();
        if (!silence) {
            peopleEditForm.getImportButton().setEnabled(false);
        }
        JProgressBar progressBar = peopleEditForm.getMemberTabImportProgressBar();
        JLabel memberCountLabel = peopleEditForm.getMemberTabCountLabel();

        if (StringUtils.isNotEmpty(querySql)) {
            Connection conn = null;
            try {
                if (!silence) {
                    peopleEditForm.getImportButton().setEnabled(false);
                    peopleEditForm.getImportButton().updateUI();
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                }

                String now = SqliteUtil.nowDateForSqlite();
                String dataVersion = UUID.fastUUID().toString(true);

                // 保存导入配置
                TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);

                TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
                tPeopleImportConfig.setPeopleId(peopleId);
                tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_SQL_CODE));
                tPeopleImportConfig.setLastSql(querySql);
                tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
                tPeopleImportConfig.setLastDataVersion(dataVersion);
                tPeopleImportConfig.setModifiedTime(now);

                if (beforePeopleImportConfig != null) {
                    tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                    peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
                } else {
                    tPeopleImportConfig.setCreateTime(now);
                    peopleImportConfigMapper.insert(tPeopleImportConfig);
                }

                // 表查询
                int currentImported = 0;

                conn = HikariUtil.getConnection();
                List<Entity> entityList = SqlExecutor.query(conn, querySql, new EntityListHandler());

                if (clear) {
                    peopleDataMapper.deleteByPeopleId(peopleId);
                }

                for (Entity entity : entityList) {
                    Set<String> fieldNames = entity.getFieldNames();
                    String[] msgData = new String[fieldNames.size()];
                    int i = 0;
                    for (String fieldName : fieldNames) {
                        msgData[i] = entity.getStr(fieldName);
                        i++;
                    }

                    TPeopleData tPeopleData = new TPeopleData();
                    tPeopleData.setPeopleId(peopleId);
                    tPeopleData.setPin(msgData[0]);
                    tPeopleData.setVarData(JSONUtil.toJsonStr(msgData));
                    tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                    tPeopleData.setDataVersion(dataVersion);
                    tPeopleData.setCreateTime(now);
                    tPeopleData.setModifiedTime(now);

                    peopleDataMapper.insert(tPeopleData);
                    currentImported++;
                    if (!silence) {
                        memberCountLabel.setText(String.valueOf(currentImported));
                    }
                }

                if (!silence) {
                    PeopleEditForm.initDataTable(peopleId);

                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    JOptionPane.showMessageDialog(App.mainFrame, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);

                    App.config.setMemberSql(querySql);
                    App.config.save();
                }
            } catch (Exception e1) {
                if (!silence) {
                    JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + e1.getMessage(), "失败",
                            JOptionPane.ERROR_MESSAGE);
                }
                logger.error(e1);
            } finally {
                DbUtil.close(conn);
                if (!silence) {
                    peopleEditForm.getImportButton().setEnabled(true);
                    peopleEditForm.getImportButton().updateUI();
                    progressBar.setMaximum(100);
                    progressBar.setValue(100);
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    peopleEditForm.getImportButton().setEnabled(true);
                }
            }
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
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("取消");
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        importFromSqlButton = new JButton();
        importFromSqlButton.setText("导入");
        panel2.add(importFromSqlButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        importFromSqlTextArea = new JTextArea();
        panel3.add(importFromSqlTextArea, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(40, 50), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    public void reImport() {
        TPeopleImportConfig tPeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);
        importFromSql(tPeopleImportConfig.getLastSql(), true, true);
        dispose();
    }
}
